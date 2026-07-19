package com.swt301.ecommerce.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManualAddressRequest {
    @Size(max = 100)
    @Pattern(regexp = "^[\\p{L} .'-]+$", message = "Tên người nhận không được chứa ký tự đặc biệt")
    private String receiverName;

    @Pattern(regexp = "^(?:0[35789][0-9]{8}|\\+84[35789][0-9]{8})$", message = "Số điện thoại phải là 0xxxxxxxxx hoặc +84xxxxxxxxx")
    private String receiverPhone;

    @Size(max = 100)
    private String province;

    @Size(max = 100)
    private String district;

    @Size(max = 100)
    private String ward;

    @Size(max = 255)
    private String street;
}
