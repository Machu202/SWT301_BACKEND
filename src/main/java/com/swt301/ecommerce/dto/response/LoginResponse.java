// Vị trí: dto/response/LoginResponse.java
package com.swt301.ecommerce.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    
    private String accessToken;
    
    @Builder.Default
    private String tokenType = "Bearer"; // Chuẩn token mặc định
    
    private Integer userId;
    private String username;
    private String fullName;
    private String roleName; // Trả về role để Frontend biết đường hiển thị menu Admin/User
}