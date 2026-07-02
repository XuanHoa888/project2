package com.xhstore.cosmetic.service;

import com.xhstore.cosmetic.model.*;
import com.xhstore.cosmetic.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired private OrderRepository orderRepo;
    @Autowired private OrderItemRepository itemRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private RefundRequestRepository refundRepo;
    @Autowired private VoucherService voucherService;

    // Luồng trạng thái hợp lệ chỉ cho phép chuyển theo chiều mũi tên
    private static final Map<String, List<String>> NEXT_STATUS = Map.of(
        "PENDING",          List.of("PROCESSING", "CANCELLED"),
        "PROCESSING",       List.of("SHIPPING", "CANCELLED"),
        "SHIPPING",         List.of("DELIVERED"),
        "DELIVERED",        List.of("REFUND_REQUESTED"),
        "REFUND_REQUESTED", List.of("REFUNDED", "REFUND_REJECTED"),
        "CANCELLED",        List.of(),
        "REFUNDED",         List.of(),
        "REFUND_REJECTED",  List.of()
    );

    /**
     * Đặt đơn hàng kiểm tra kho (khóa ghi), áp mã, lưu DB trong 1 transaction.
     */
    @Transactional
    public Order placeOrder(User user, OrderRequest req) {
        // Validate thông tin nhận hàng
        if (req.getItems() == null || req.getItems().isEmpty())
            throw new RuntimeException("Đơn hàng phải có ít nhất 1 sản phẩm");
        if (blank(req.getReceiverName()))
            throw new RuntimeException("Vui lòng nhập tên người nhận");
        if (blank(req.getReceiverPhone()))
            throw new RuntimeException("Vui lòng nhập số điện thoại người nhận");
        if (!req.getReceiverPhone().trim().matches("^0\\d{9,10}$"))
            throw new RuntimeException("Số điện thoại không hợp lệ (phải bắt đầu bằng 0, gồm 10-11 chữ số)");
        if (blank(req.getReceiverAddress()))
            throw new RuntimeException("Vui lòng nhập địa chỉ giao hàng");

        Order order = new Order();
        order.setUser(user);
        order.setOrderCode(genCode());
        order.setReceiverName(req.getReceiverName().trim());
        order.setReceiverPhone(req.getReceiverPhone().trim());
        order.setReceiverAddress(req.getReceiverAddress().trim());
        order.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "COD");
        order.setNote(req.getNote());
        order.setStatus("PENDING");
        order.setCreatedAt(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemReq itemReq : req.getItems()) {
            // Validate số lượng
            int qty = itemReq.getQuantity() == null ? 0 : itemReq.getQuantity();
            if (qty <= 0)
                throw new RuntimeException("Số lượng sản phẩm phải lớn hơn 0");

            // Dùng khóa ghi để tránh 2 request cùng trừ kho
            Product sp = productRepo.findByIdForUpdate(itemReq.getProductId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại (id=" + itemReq.getProductId() + ")"));

            if (!Boolean.TRUE.equals(sp.getIsActive()))
                throw new RuntimeException("Sản phẩm '" + sp.getName() + "' hiện không còn bán");

            BigDecimal itemPrice = sp.getPrice();
            String variantName = itemReq.getVariant();

            // Nếu sản phẩm có các biến thể
            if (sp.getVariants() != null && !sp.getVariants().isEmpty()) {
                if (variantName == null || variantName.trim().isEmpty()) {
                    throw new RuntimeException("Sản phẩm '" + sp.getName() + "' yêu cầu chọn phân loại");
                }
                
                ProductVariant matchVariant = null;
                for (ProductVariant v : sp.getVariants()) {
                    if (v.getName().equalsIgnoreCase(variantName.trim())) {
                        matchVariant = v;
                        break;
                    }
                }
                
                if (matchVariant == null) {
                    throw new RuntimeException("Phân loại '" + variantName + "' của sản phẩm '" + sp.getName() + "' không tồn tại");
                }
                
                if (matchVariant.getStock() < qty) {
                    throw new RuntimeException("Phân loại '" + variantName + "' của sản phẩm '" + sp.getName() + "' chỉ còn " + matchVariant.getStock() + " trong kho");
                }
                
                // Trừ kho của biến thể
                matchVariant.setStock(matchVariant.getStock() - qty);
                itemPrice = matchVariant.getPrice();
                
                // Cập nhật lại tổng kho của Product
                int totalStock = sp.getVariants().stream().mapToInt(ProductVariant::getStock).sum();
                sp.setStock(totalStock);
            } else {
                // Trực tiếp trừ kho trên Product
                if (sp.getStock() < qty)
                    throw new RuntimeException("Sản phẩm '" + sp.getName() + "' chỉ còn " + sp.getStock() + " trong kho");
                sp.setStock(sp.getStock() - qty);
            }

            // Tăng đã bán
            sp.setSoldCount(sp.getSoldCount() + qty);
            productRepo.save(sp);

            BigDecimal subtotal = itemPrice.multiply(BigDecimal.valueOf(qty));
            total = total.add(subtotal);

            // Snapshot giá lúc mua giữ đúng lịch sử dù sản phẩm bị sửa sau
            OrderItem item = new OrderItem();
            item.setProduct(sp);
            item.setProductName(sp.getName());
            item.setProductPrice(itemPrice);
            item.setVariant(variantName);
            item.setQuantity(qty);
            item.setImageUrl(sp.getImageUrl());
            orderItems.add(item);
        }

        order.setTotalAmount(total);

        // Áp mã giảm giá nếu có
        BigDecimal discount = BigDecimal.ZERO;
        Voucher appliedVoucher = null;
        if (!blank(req.getVoucherCode())) {
            appliedVoucher = voucherService.validate(req.getVoucherCode(), total);
            discount = voucherService.calcDiscount(appliedVoucher, total);
            order.setVoucherCode(appliedVoucher.getCode());
        }

        order.setDiscountAmount(discount);
        order.setFinalAmount(total.subtract(discount));

        Order saved = orderRepo.save(order);
        for (OrderItem item : orderItems) {
            item.setOrder(saved);
            itemRepo.save(item);
        }

        if (appliedVoucher != null) {
            voucherService.markUsed(appliedVoucher);
        }

        return saved;
    }

    public List<Order> getMyOrders(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Order getById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng (id=" + id + ")"));
    }

    public List<Order> getAll() {
        return orderRepo.findAllByOrderByCreatedAtDesc();
    }

 
    @Transactional
    public Order updateStatus(Long id, String newStatus) {
        Order order = getById(id);
        String cur = order.getStatus();

        List<String> allowed = NEXT_STATUS.getOrDefault(cur, List.of());
        if (!allowed.contains(newStatus))
            throw new RuntimeException(
                "Không thể chuyển từ '" + cur + "' sang '" + newStatus + "'. " +
                "Cho phép: " + allowed
            );

        order.setStatus(newStatus);
        return orderRepo.save(order);
    }

    /**
     * Khách yêu cầu hoàn chỉ khi đơn đã giao, chỉ 1 lần.
     */
    @Transactional
    public RefundRequest requestRefund(Long orderId, Long userId, String reason, String desc) {
        Order order = getById(orderId);

        if (!order.getUser().getId().equals(userId))
            throw new RuntimeException("Bạn không có quyền thao tác với đơn hàng này");
        if (!"DELIVERED".equals(order.getStatus()))
            throw new RuntimeException("Chỉ đơn hàng đã giao thành công mới được yêu cầu hoàn");
        if (refundRepo.findByOrderId(orderId).isPresent())
            throw new RuntimeException("Đơn hàng này đã có yêu cầu hoàn trước đó");

        order.setStatus("REFUND_REQUESTED");
        orderRepo.save(order);

        RefundRequest req = new RefundRequest();
        req.setOrder(order);
        req.setReason(reason != null ? reason.trim() : "Không rõ");
        req.setDescription(desc);
        req.setStatus("PENDING");
        req.setCreatedAt(LocalDateTime.now());
        return refundRepo.save(req);
    }

    /**
     * Admin duyệt hoặc từ chối hoàn hàng.
     * Nếu duyệt: hoàn kho về.
     */
    @Transactional
    public RefundRequest handleRefund(Long refundId, boolean approve) {
        RefundRequest req = refundRepo.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu hoàn (id=" + refundId + ")"));

        if (!"PENDING".equals(req.getStatus()))
            throw new RuntimeException("Yêu cầu này đã được xử lý rồi");

        Order order = req.getOrder();
        if (approve) {
            req.setStatus("APPROVED");
            order.setStatus("REFUNDED");

            // Hoàn kho đảm bảo soldCount không âm
            List<OrderItem> items = itemRepo.findByOrderId(order.getId());
            for (OrderItem item : items) {
                if (item.getProduct() != null) {
                    Product sp = item.getProduct();
                    String variantName = item.getVariant();
                    
                    if (sp.getVariants() != null && !sp.getVariants().isEmpty() && variantName != null && !variantName.trim().isEmpty()) {
                        ProductVariant matchVariant = null;
                        for (ProductVariant v : sp.getVariants()) {
                            if (v.getName().equalsIgnoreCase(variantName.trim())) {
                                matchVariant = v;
                                break;
                            }
                        }
                        if (matchVariant != null) {
                            matchVariant.setStock(matchVariant.getStock() + item.getQuantity());
                        }
                        int totalStock = sp.getVariants().stream().mapToInt(ProductVariant::getStock).sum();
                        sp.setStock(totalStock);
                    } else {
                        sp.setStock(sp.getStock() + item.getQuantity());
                    }
                    
                    sp.setSoldCount(Math.max(0, sp.getSoldCount() - item.getQuantity()));
                    productRepo.save(sp);
                }
            }
        } else {
            req.setStatus("REJECTED");
            order.setStatus("REFUND_REJECTED");
        }

        req.setResolvedAt(LocalDateTime.now());
        orderRepo.save(order);
        return refundRepo.save(req);
    }

    public List<RefundRequest> getAllRefunds() {
        return refundRepo.findAllByOrderByCreatedAtDesc();
    }

    private String genCode() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int attempt = 0; attempt < 5; attempt++) {
            String suffix = java.util.UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
            String code = "ORD-" + date + "-" + suffix;
            if (!orderRepo.existsByOrderCode(code)) {
                return code;
            }
        }
        // Fallback an toàn: dùng 12 ký tự UUID
        return "ORD-" + date + "-" + java.util.UUID.randomUUID().toString()
                .replace("-", "").substring(0, 12).toUpperCase();
    }

    private boolean blank(String s) {
        return s == null || s.trim().isEmpty();
    }


    public static class OrderRequest {
        private String receiverName;
        private String receiverPhone;
        private String receiverAddress;
        private String paymentMethod; // COD | BANK TRANSFER
        private String voucherCode;
        private String note;
        private List<OrderItemReq> items;

        public String getReceiverName() { return receiverName; }
        public void setReceiverName(String v) { this.receiverName = v; }
        public String getReceiverPhone() { return receiverPhone; }
        public void setReceiverPhone(String v) { this.receiverPhone = v; }
        public String getReceiverAddress() { return receiverAddress; }
        public void setReceiverAddress(String v) { this.receiverAddress = v; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String v) { this.paymentMethod = v; }
        public String getVoucherCode() { return voucherCode; }
        public void setVoucherCode(String v) { this.voucherCode = v; }
        public String getNote() { return note; }
        public void setNote(String v) { this.note = v; }
        public List<OrderItemReq> getItems() { return items; }
        public void setItems(List<OrderItemReq> v) { this.items = v; }
    }

    public static class OrderItemReq {
        private Long productId;
        private String variant;
        private Integer quantity;

        public Long getProductId() { return productId; }
        public void setProductId(Long v) { this.productId = v; }
        public String getVariant() { return variant; }
        public void setVariant(String v) { this.variant = v; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer v) { this.quantity = v; }
    }
}
