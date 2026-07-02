package com.xhstore.cosmetic.controller;

import com.xhstore.cosmetic.model.Voucher;
import com.xhstore.cosmetic.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    @Autowired VoucherService voucherService;

    /**
     * Xem trước số tiền giảm khi nhập mã KHÔNG tăng usedCount.
     * GET /api/vouchers/check?code=MYPHAM10&total=500000
     */
    @GetMapping("/check")
    public ResponseEntity<?> check(@RequestParam String code, @RequestParam BigDecimal total) {
        try {
            // validate KHÔNG dùng khóa ghi (chỉ xem trước, chưa dùng thật)
            Voucher v = voucherService.findByCode(code)
                    .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại"));

            // Gọi validate thật để kiểm tra đầy đủ (active, hạn, lượt)
            Voucher checked = voucherService.validate(code, total);
            BigDecimal discount = voucherService.calcDiscount(checked, total);

            return ResponseEntity.ok(Map.of(
                "code",          checked.getCode(),
                "discountType",  checked.getDiscountType(),
                "discountValue", checked.getDiscountValue(),
                "discount",      discount,
                "finalTotal",    total.subtract(discount)
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
