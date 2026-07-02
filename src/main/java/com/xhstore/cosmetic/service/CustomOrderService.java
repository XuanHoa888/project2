package com.xhstore.cosmetic.service;

import com.xhstore.cosmetic.model.CustomOrder;
import com.xhstore.cosmetic.model.User;
import com.xhstore.cosmetic.repository.CustomOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
public class CustomOrderService {

    @Autowired
    private CustomOrderRepository customOrderRepo;

    private static final Map<String, List<String>> NEXT_STATUS = Map.of(
        "SUBMITTED",               List.of("APPROVED", "REJECTED"),
        "APPROVED",                List.of("WAITING_CONFIRM_PAYMENT", "REJECTED"),
        "WAITING_CONFIRM_PAYMENT", List.of("PAID", "APPROVED"), // Can go back to APPROVED if payment is invalid
        "PAID",                    List.of("PROCESSING"),
        "PROCESSING",              List.of("SHIPPED"),
        "SHIPPED",                 List.of("COMPLETED"),
        "COMPLETED",               List.of(),
        "REJECTED",                List.of()
    );

    @Transactional
    public CustomOrder createRequest(User user, CustomOrder req) {
        if (req.getProductName() == null || req.getProductName().trim().isEmpty())
            throw new RuntimeException("Tên sản phẩm mẫu không được để trống");
        if (req.getReceiverName() == null || req.getReceiverName().trim().isEmpty())
            throw new RuntimeException("Vui lòng nhập tên người nhận");
        if (req.getReceiverPhone() == null || req.getReceiverPhone().trim().isEmpty())
            throw new RuntimeException("Vui lòng nhập số điện thoại người nhận");
        if (req.getReceiverAddress() == null || req.getReceiverAddress().trim().isEmpty())
            throw new RuntimeException("Vui lòng nhập địa chỉ nhận hàng");

        CustomOrder order = new CustomOrder();
        order.setUser(user);
        order.setProductName(req.getProductName().trim());
        order.setDescription(req.getDescription());
        order.setImageUrl(req.getImageUrl());
        order.setReceiverName(req.getReceiverName().trim());
        order.setReceiverPhone(req.getReceiverPhone().trim());
        order.setReceiverAddress(req.getReceiverAddress().trim());
        order.setStatus("SUBMITTED");
        order.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "BANK_TRANSFER");

        return customOrderRepo.save(order);
    }

    public List<CustomOrder> getMyRequests(Long userId) {
        return customOrderRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<CustomOrder> getAllRequests() {
        return customOrderRepo.findAllByOrderByCreatedAtDesc();
    }

    public CustomOrder getById(Long id) {
        return customOrderRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu đặt hàng (id=" + id + ")"));
    }

    @Transactional
    public CustomOrder adminApproveAndQuote(Long id, BigDecimal price, String adminNote) {
        CustomOrder order = getById(id);
        if (!"SUBMITTED".equals(order.getStatus()))
            throw new RuntimeException("Yêu cầu này đã được xử lý hoặc báo giá rồi");
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Giá duyệt phải lớn hơn 0");

        order.setPrice(price);
        order.setAdminNote(adminNote);
        order.setStatus("APPROVED");
        return customOrderRepo.save(order);
    }

    @Transactional
    public CustomOrder adminReject(Long id, String adminNote) {
        CustomOrder order = getById(id);
        if (!"SUBMITTED".equals(order.getStatus()) && !"APPROVED".equals(order.getStatus()))
            throw new RuntimeException("Yêu cầu này không thể từ chối ở trạng thái hiện tại");

        order.setStatus("REJECTED");
        order.setAdminNote(adminNote);
        return customOrderRepo.save(order);
    }

    @Transactional
    public CustomOrder clientPay(Long id, Long userId) {
        CustomOrder order = getById(id);
        if (!order.getUser().getId().equals(userId))
            throw new RuntimeException("Bạn không có quyền thao tác với yêu cầu này");
        if (!"APPROVED".equals(order.getStatus()))
            throw new RuntimeException("Chỉ yêu cầu đã được báo giá mới có thể thanh toán");

        order.setStatus("WAITING_CONFIRM_PAYMENT");
        return customOrderRepo.save(order);
    }

    @Transactional
    public CustomOrder adminConfirmPayment(Long id) {
        CustomOrder order = getById(id);
        if (!"WAITING_CONFIRM_PAYMENT".equals(order.getStatus()))
            throw new RuntimeException("Yêu cầu này chưa gửi yêu cầu xác nhận thanh toán");

        order.setStatus("PAID");
        return customOrderRepo.save(order);
    }

    @Transactional
    public CustomOrder adminUpdateStatus(Long id, String newStatus) {
        CustomOrder order = getById(id);
        String cur = order.getStatus();

        List<String> allowed = NEXT_STATUS.getOrDefault(cur, List.of());
        if (!allowed.contains(newStatus))
            throw new RuntimeException("Không thể chuyển trạng thái từ '" + cur + "' sang '" + newStatus + "'");

        order.setStatus(newStatus);
        return customOrderRepo.save(order);
    }
}
