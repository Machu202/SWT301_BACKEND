// Vị trí: src/main/java/com/swt301/ecommerce/dto/request/CheckoutRequest.java
package com.swt301.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {

    @NotNull(message = "Vui lòng chọn địa chỉ giao hàng")
    private Integer addressId;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private Integer paymentMethodId;

    // Voucher code có thể null nếu khách không dùng
    private String voucherCode;

    private String note;
}