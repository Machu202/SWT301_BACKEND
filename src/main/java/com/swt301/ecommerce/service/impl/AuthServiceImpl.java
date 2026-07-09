// Vị trí: src/main/java/com/swt301/ecommerce/service/impl/AuthServiceImpl.java
package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.LoginRequest;
import com.swt301.ecommerce.dto.request.RegisterRequest;
import com.swt301.ecommerce.dto.response.JwtResponse;
import com.swt301.ecommerce.dto.response.MessageResponse;
import com.swt301.ecommerce.entity.*;
import com.swt301.ecommerce.repository.*;
import com.swt301.ecommerce.security.JwtUtils;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return JwtResponse.builder()
                .token(jwt)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .role(userDetails.getAuthorities().iterator().next().getAuthority())
                .build();
    }

    @Override
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }
        
        Role userRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));

        User user = User.builder()
                .username(request.getUsername())
                .passwordHash(encoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(userRole)
                .build();

        userRepository.save(user);
        return new MessageResponse("User registered successfully!");
    }
}