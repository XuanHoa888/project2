package com.xhstore.cosmetic.service;

import com.xhstore.cosmetic.model.Voucher;
import com.xhstore.cosmetic.repository.VoucherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class VoucherService {

    @Autowired
    private VoucherRepository repo;

    /**
     * Kiểm tra voucher có dùng được không.
     * Dùng khóa ghi để tránh 2 người cùng apply voucher hết lượt.
     */
    @Transactional
    public Voucher validate(String rawCode, BigDecimal total) {
        String code = rawCode.toUpperCase().trim();

        // Khóa ghi để đọc + dùng atomic
        Voucher v = repo.findByCodeForUpdate(code)
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

        if (!Boolean.TRUE.equals(v.getIsActive()))
            throw new RuntimeException("Mã giảm giá đã bị vô hiệu hóa");

        if (v.getExpiryDate() != null && v.getExpiryDate().isBefore(LocalDate.now()))
            throw new RuntimeException("Mã giảm giá đã hết hạn");

        if (v.getMaxUses() != null && v.getUsedCount() >= v.getMaxUses())
            throw new RuntimeException("Mã giảm giá đã đạt giới hạn sử dụng");

        if (total.compareTo(v.getMinOrderValue()) < 0)
            throw new RuntimeException(
                "Đơn hàng tối thiểu " + formatVnd(v.getMinOrderValue()) + " để dùng mã này"
            );

        return v;
    }

    /**
     * Tính số tiền giảm.
     * PERCENT: giảm %, FIXED: giảm cố định — không vượt quá tổng đơn.
     */
    public BigDecimal calcDiscount(Voucher v, BigDecimal total) {
        BigDecimal discount;
        if ("PERCENT".equals(v.getDiscountType())) {
            BigDecimal rate = v.getDiscountValue().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            discount = total.multiply(rate).setScale(0, RoundingMode.HALF_UP);
        } else {
            discount = v.getDiscountValue();
        }
        return discount.min(total); // không giảm nhiều hơn tổng đơn
    }

    /** Tăng lượt dùng sau khi áp thành công. */
    public void markUsed(Voucher v) {
        v.setUsedCount(v.getUsedCount() + 1);
        repo.save(v);
    }

    public Optional<Voucher> findByCode(String code) {
        return repo.findByCode(code.toUpperCase().trim());
    }

    public List<Voucher> getAll() {
        return repo.findAll();
    }

    public Voucher save(Voucher v) {
        return repo.save(v);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }

    private String formatVnd(BigDecimal amount) {
        return String.format("%,.0f₫", amount);
    }
}
