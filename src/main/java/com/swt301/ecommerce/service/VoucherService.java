// Vị trí: src/main/java/com/swt301/ecommerce/service/VoucherService.java
package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.response.VoucherResponse;
import java.math.BigDecimal;

public interface VoucherService {
    VoucherResponse checkVoucher(String code, BigDecimal orderSubtotal);
}