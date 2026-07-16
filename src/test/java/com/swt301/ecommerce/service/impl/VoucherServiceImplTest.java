package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.entity.Voucher;
import com.swt301.ecommerce.exception.BusinessRuleException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.VoucherRepository;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {
    @Mock VoucherRepository repository;
    @InjectMocks VoucherServiceImpl service;

    @Test void returnsValidVoucher() {
        Voucher voucher = TestFixtures.voucher(1, "PERCENT", new BigDecimal("10"));
        when(repository.findByVoucherCode("SAVE1")).thenReturn(Optional.of(voucher));
        var result = service.checkVoucher("SAVE1", new BigDecimal("100000"));
        assertThat(result.getVoucherCode()).isEqualTo("SAVE1");
    }

    @Test void rejectsMissingVoucher() {
        when(repository.findByVoucherCode("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.checkVoucher("NOPE", BigDecimal.TEN))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void rejectsInactiveExhaustedExpiredAndMinimumOrder() {
        Voucher voucher = TestFixtures.voucher(1, "FIXED", BigDecimal.TEN);
        when(repository.findByVoucherCode("SAVE1")).thenReturn(Optional.of(voucher));

        voucher.setStatus("INACTIVE");
        assertThatThrownBy(() -> service.checkVoucher("SAVE1", BigDecimal.TEN)).isInstanceOf(BusinessRuleException.class);
        voucher.setStatus("ACTIVE"); voucher.setQuantity(0);
        assertThatThrownBy(() -> service.checkVoucher("SAVE1", BigDecimal.TEN)).isInstanceOf(BusinessRuleException.class);
        voucher.setQuantity(1); voucher.setExpiredDate(LocalDateTime.now().minusMinutes(1));
        assertThatThrownBy(() -> service.checkVoucher("SAVE1", BigDecimal.TEN)).isInstanceOf(BusinessRuleException.class);
        voucher.setExpiredDate(LocalDateTime.now().plusDays(1)); voucher.setMinOrder(new BigDecimal("100"));
        assertThatThrownBy(() -> service.checkVoucher("SAVE1", BigDecimal.TEN)).isInstanceOf(BusinessRuleException.class);
    }
}
