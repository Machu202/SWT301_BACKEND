// Vị trí: src/main/java/com/swt301/ecommerce/dto/request/AddressRequest.java
package com.swt301.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|84)(3|5|7|8|9)[0-9]{8}$", message = "Số điện thoại không đúng định dạng VN")
    private String receiverPhone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String ward;

    @NotBlank(message = "Tên đường/Số nhà không được để trống")
    private String street;

    private Boolean isDefault;
}