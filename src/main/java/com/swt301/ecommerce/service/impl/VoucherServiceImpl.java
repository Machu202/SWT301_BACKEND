// Vị trí: src/main/java/com/swt301/ecommerce/service/impl/VoucherServiceImpl.java
package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.response.VoucherResponse;
import com.swt301.ecommerce.entity.Voucher;
import com.swt301.ecommerce.exception.BusinessRuleException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.VoucherRepository;
import com.swt301.ecommerce.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;

    @Override
    public VoucherResponse checkVoucher(String code, BigDecimal orderSubtotal) {
        Voucher voucher = voucherRepository.findByVoucherCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Mã giảm giá không tồn tại"));

        if (!"ACTIVE".equalsIgnoreCase(voucher.getStatus())) {
            throw new BusinessRuleException("Mã giảm giá không còn hoạt động");
        }

        if (voucher.getQuantity() != null && voucher.getQuantity() <= 0) {
            throw new BusinessRuleException("Mã giảm giá đã hết lượt sử dụng");
        }

        if (voucher.getExpiredDate() != null && voucher.getExpiredDate().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("Mã giảm giá đã hết hạn");
        }

        if (voucher.getMinOrder() != null && orderSubtotal.compareTo(voucher.getMinOrder()) < 0) {
            throw new BusinessRuleException("Đơn hàng chưa đạt giá trị tối thiểu (" + voucher.getMinOrder() + "đ)");
        }

        return VoucherResponse.builder()
                .voucherId(voucher.getVoucherId())
                .voucherCode(voucher.getVoucherCode())
                .voucherName(voucher.getVoucherName())
                .discountType(voucher.getDiscountType())
                .discountValue(voucher.getDiscountValue())
                .minOrder(voucher.getMinOrder())
                .build();
    }
}