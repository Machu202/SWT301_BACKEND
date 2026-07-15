package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class PaymentQrInfoResponse {
    private Integer orderId;
    private String orderCode;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private String qrContent;
}
