package com.swt301.ecommerce.dto.request;

import com.swt301.ecommerce.util.VietnamPhoneUtils;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3 đến 50 ký tự")
    private String username;

    @NotBlank(message = "Password không được để trống")
    @Size(min = 6, max = 40, message = "Password phải từ 6 đến 40 ký tự")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    @Size(max = 100, message = "Họ tên không được vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = VietnamPhoneUtils.REGEX, message = "Số điện thoại phải có dạng 0xxxxxxxxx hoặc +84xxxxxxxxx")
    private String phone;

    @NotBlank(message = "Role không được để trống")
    @Pattern(regexp = "(?i)^(ROLE_)?CUSTOMER$", message = "Đăng ký công khai chỉ cho phép role CUSTOMER")
    private String role;
}
