package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class AdminOrderMetricsResponse {
    private long totalOrders;
    private long paymentsPendingVerification;
    private BigDecimal paidRevenue;
}
