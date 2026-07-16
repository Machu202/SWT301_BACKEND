package com.swt301.ecommerce.dto.request;

import com.swt301.ecommerce.util.VietnamPhoneUtils;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận không được vượt quá 100 ký tự")
    private String receiverName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = VietnamPhoneUtils.REGEX, message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
    private String receiverPhone;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    @Size(max = 100, message = "Tỉnh/Thành phố không được vượt quá 100 ký tự")
    private String province;

    @NotBlank(message = "Quận/Huyện không được để trống")
    @Size(max = 100, message = "Quận/Huyện không được vượt quá 100 ký tự")
    private String district;

    @NotBlank(message = "Phường/Xã không được để trống")
    @Size(max = 100, message = "Phường/Xã không được vượt quá 100 ký tự")
    private String ward;

    @NotBlank(message = "Tên đường/Số nhà không được để trống")
    @Size(max = 255, message = "Tên đường/Số nhà không được vượt quá 255 ký tự")
    private String street;

    private Boolean isDefault;
}
