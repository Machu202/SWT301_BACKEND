// Vị trí: src/main/java/com/swt301/ecommerce/dto/response/VoucherResponse.java
package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class VoucherResponse {
    private Integer voucherId;
    private String voucherCode;
    private String voucherName;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrder;
}