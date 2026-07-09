// Vị trí: src/main/java/com/swt301/ecommerce/service/AuthService.java
package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.LoginRequest;
import com.swt301.ecommerce.dto.request.RegisterRequest;
import com.swt301.ecommerce.dto.response.JwtResponse;
import com.swt301.ecommerce.dto.response.MessageResponse;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);
    MessageResponse register(RegisterRequest registerRequest);
}