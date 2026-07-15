package com.swt301.ecommerce.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CheckoutRequest {
    /** Existing saved address. Leave null when manualAddress is supplied. */
    private Integer addressId;

    @Valid
    private ManualAddressRequest manualAddress;

    @NotNull(message = "Vui lòng chọn phương thức thanh toán")
    private Integer paymentMethodId;

    private String voucherCode;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;

    /** May be supplied in the body; the controller also accepts the Idempotency-Key header. */
    @Size(max = 100)
    private String idempotencyKey;
}
