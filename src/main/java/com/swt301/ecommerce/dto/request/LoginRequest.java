// Vị trí: src/main/java/com/teamproject/ecommerceapi/dto/request/LoginRequest.java
package com.swt301.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Fields cannot be empty")
    private String username;

    @NotBlank(message = "Fields cannot be empty")
    private String password;
}