package com.swt301.ecommerce.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProfileResponse {
    private Integer userId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
}
