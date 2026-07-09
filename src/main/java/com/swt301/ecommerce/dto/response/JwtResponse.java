// Vị trí: src/main/java/com/teamproject/ecommerceapi/dto/response/JwtResponse.java
package com.swt301.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class JwtResponse {
    private String token;
    @Builder.Default
    private String type = "Bearer";
    private Integer id;
    private String username;
    private String email;
    private String role; // DB thiết kế mỗi user 1 quyền
}