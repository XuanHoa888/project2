package com.xhstore.cosmetic.service;

import com.xhstore.cosmetic.model.Category;
import com.xhstore.cosmetic.model.Product;
import com.xhstore.cosmetic.model.ProductVariant;
import com.xhstore.cosmetic.repository.CategoryRepository;
import com.xhstore.cosmetic.repository.OrderItemRepository;
import com.xhstore.cosmetic.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductService {

    @Autowired private ProductRepository repo;
    @Autowired private CategoryRepository categoryRepo;
    @Autowired private OrderItemRepository orderItemRepo;

    /** Danh sách sản phẩm đang bán (khách hàng xem). */
    public List<Product> getActive() {
        return repo.findByIsActiveTrue();
    }

    /** Toàn bộ sản phẩm kể cả đã ẩn (admin xem để quản lý). */
    public List<Product> getAll() {
        return repo.findAll();
    }

    public List<Product> getByCategory(Long categoryId) {
        return repo.findByCategoryIdAndIsActiveTrue(categoryId);
    }

    public List<Product> search(String query) {
        if (query == null || query.trim().isEmpty()) return getActive();
        return repo.findByNameContainingIgnoreCaseAndIsActiveTrue(query.trim());
    }

    public Product getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm (id=" + id + ")"));
    }

    public List<Product> getRelated(Long productId) {
        Product cur = getById(productId);
        if (cur.getCategory() == null) {
            return getActive().stream()
                    .filter(p -> !p.getId().equals(productId))
                    .limit(4).collect(Collectors.toList());
        }
        return repo.findByCategoryIdAndIdNotAndIsActiveTrue(cur.getCategory().getId(), productId)
                .stream().limit(4).collect(Collectors.toList());
    }

    public Product save(Product sp) {
        if (sp.getName() == null || sp.getName().trim().isEmpty())
            throw new RuntimeException("Tên sản phẩm không được để trống");

        // Đồng bộ biến thể nếu có
        if (sp.getVariants() != null && !sp.getVariants().isEmpty()) {
            BigDecimal minPrice = null;
            int totalStock = 0;
            for (ProductVariant v : sp.getVariants()) {
                v.setProduct(sp);
                if (v.getPrice() == null || v.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Giá của phân loại '" + v.getName() + "' phải lớn hơn 0");
                }
                if (v.getStock() == null || v.getStock() < 0) {
                    throw new RuntimeException("Tồn kho của phân loại '" + v.getName() + "' không được âm");
                }
                if (minPrice == null || v.getPrice().compareTo(minPrice) < 0) {
                    minPrice = v.getPrice();
                }
                totalStock += v.getStock();
            }
            sp.setPrice(minPrice);
            sp.setStock(totalStock);
        }

        if (sp.getPrice() == null || sp.getPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Giá sản phẩm phải lớn hơn 0");
        if (sp.getStock() != null && sp.getStock() < 0)
            throw new RuntimeException("Số lượng tồn kho không được âm");

        // Gắn đúng category từ DB (tránh detached entity)
        if (sp.getCategory() != null && sp.getCategory().getId() != null) {
            Category cat = categoryRepo.findById(sp.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
            sp.setCategory(cat);
        }
        return repo.save(sp);
    }

    public Product update(Long id, Product data) {
        Product sp = getById(id);
        sp.setName(data.getName());
        sp.setDescription(data.getDescription());
        sp.setPrice(data.getPrice());
        sp.setImageUrl(data.getImageUrl());
        sp.setStock(data.getStock());
        if (data.getCategory() != null && data.getCategory().getId() != null) {
            Category cat = categoryRepo.findById(data.getCategory().getId())
                    .orElseThrow(() -> new RuntimeException("Danh mục không tồn tại"));
            sp.setCategory(cat);
        }
        if (data.getIsActive() != null) {
            sp.setIsActive(data.getIsActive());
        }

        // Cập nhật danh sách biến thể
        sp.getVariants().clear();
        if (data.getVariants() != null) {
            sp.getVariants().addAll(data.getVariants());
        }

        return save(sp);
    }

    /**
     * Xóa hoặc ẩn sản phẩm.
     * Nếu sản phẩm đã có trong đơn hàng, chỉ ẩn đi (isActive = false).
     * Nếu chưa từng có đơn hàng, xóa vĩnh viễn khỏi DB.
     * Trả về true nếu xóa vĩnh viễn (hard delete), false nếu chỉ ẩn đi (soft delete).
     */
    public boolean delete(Long id) {
        Product sp = getById(id);
        boolean hasOrders = orderItemRepo.existsByProductId(id);
        if (hasOrders) {
            sp.setIsActive(false);
            repo.save(sp);
            return false;
        } else {
            repo.delete(sp);
            return true;
        }
    }

    public void hide(Long id) {
        Product sp = getById(id);
        sp.setIsActive(false);
        repo.save(sp);
    }


    @Transactional
    public void applyBoothDiscount(String boothKey, String saleTag) {
        int discountPercent = 0;
        if (saleTag != null && !saleTag.trim().isEmpty()) {
            Matcher m = Pattern.compile("(\\d+)%").matcher(saleTag);
            if (m.find()) {
                try {
                    discountPercent = Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {
                    discountPercent = 0;
                }
            }
        }

        List<String> categoryNames = new ArrayList<>();
        if ("office".equals(boothKey)) {
            categoryNames.add("Thời Trang");
        } else if ("beauty".equals(boothKey)) {
            categoryNames.add("Mỹ Phẩm");
            categoryNames.add("Chăm Sóc Da");
        } else if ("jewel".equals(boothKey)) {
            categoryNames.add("Trang Sức");
            categoryNames.add("Phụ Kiện");
        }

        if (categoryNames.isEmpty()) return;

        final int dp = discountPercent;
        for (String catName : categoryNames) {
            Optional<Category> catOpt = categoryRepo.findByName(catName);
            if (catOpt.isEmpty()) continue;

            List<Product> products = getByCategory(catOpt.get().getId());
            for (Product p : products) {
                boolean hasVariants = p.getVariants() != null && !p.getVariants().isEmpty();

                if (hasVariants) {

                    for (ProductVariant v : p.getVariants()) {
                        // Ghi nhớ giá gốc của variant lần đầu
                        if (v.getOriginalPrice() == null || v.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                            v.setOriginalPrice(v.getPrice());
                        }
                        if (dp > 0) {
                            BigDecimal newVPrice = v.getOriginalPrice()
                                    .multiply(BigDecimal.valueOf(1.0 - dp / 100.0))
                                    .setScale(2, RoundingMode.HALF_UP);
                            v.setPrice(newVPrice);
                        } else {
                            v.setPrice(v.getOriginalPrice());
                        }
                    }
                    // Cập nhật p.price = min variant price sau discount để hiển thị đúng
                    BigDecimal minVariantPrice = p.getVariants().stream()
                            .map(ProductVariant::getPrice)
                            .min(BigDecimal::compareTo)
                            .orElse(p.getPrice());
                    // Ghi nhớ giá gốc của product nếu chưa có
                    if (p.getOriginalPrice() == null || p.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        p.setOriginalPrice(minVariantPrice);
                    }
                    p.setPrice(minVariantPrice);
                } else {
                    //Sản phẩm KHÔNG có biến thể: áp discount lên p.price
                    if (p.getOriginalPrice() == null || p.getOriginalPrice().compareTo(BigDecimal.ZERO) <= 0) {
                        p.setOriginalPrice(p.getPrice());
                    }
                    if (dp > 0) {
                        BigDecimal newPrice = p.getOriginalPrice()
                                .multiply(BigDecimal.valueOf(1.0 - dp / 100.0))
                                .setScale(2, RoundingMode.HALF_UP);
                        p.setPrice(newPrice);
                    } else {
                        p.setPrice(p.getOriginalPrice());
                    }
                }
                repo.save(p);
            }
        }
    }
}
