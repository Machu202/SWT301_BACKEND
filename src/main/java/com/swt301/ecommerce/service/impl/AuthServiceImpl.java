package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.LoginRequest;
import com.swt301.ecommerce.dto.request.RegisterRequest;
import com.swt301.ecommerce.dto.response.JwtResponse;
import com.swt301.ecommerce.dto.response.MessageResponse;
import com.swt301.ecommerce.entity.Role;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.exception.ConflictException;
import com.swt301.ecommerce.repository.RoleRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.security.JwtUtils;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.AuthService;
import com.swt301.ecommerce.util.VietnamPhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

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
        String identifier = loginRequest.getUsername().trim();
        User account = resolveLoginAccount(identifier);
        String normalizedRole = account.getRole().getRoleName().trim().toUpperCase(Locale.ROOT);
        if (!normalizedRole.equals("CUSTOMER") && !normalizedRole.equals("ADMIN")) {
            throw new AccessDeniedException("Tài khoản có role không hợp lệ. Vui lòng liên hệ quản trị viên");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(account.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        return JwtResponse.builder()
                .token(jwt)
                .id(userDetails.getId())
                .username(userDetails.getUsername())
                .email(userDetails.getEmail())
                .role("ROLE_" + normalizedRole)
                .build();
    }

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        String requestedRole = request.getRole().trim().toUpperCase(Locale.ROOT).replace("ROLE_", "");
        if (!"CUSTOMER".equals(requestedRole)) {
            throw new AccessDeniedException("Đăng ký công khai chỉ cho phép role CUSTOMER");
        }
        if (userRepository.existsByUsername(request.getUsername().trim())) {
            throw new ConflictException("Username đã được sử dụng");
        }
        if (userRepository.existsByEmail(request.getEmail().trim())) {
            throw new ConflictException("Email đã được sử dụng");
        }

        String normalizedPhone = VietnamPhoneUtils.normalize(request.getPhone());
        String localPhone = VietnamPhoneUtils.toLocal(normalizedPhone);
        if (userRepository.existsByPhone(normalizedPhone) || userRepository.existsByPhone(localPhone)) {
            throw new ConflictException("Số điện thoại đã được sử dụng");
        }

        Role userRole = roleRepository.findByRoleName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("Hệ thống chưa cấu hình role CUSTOMER"));

        User user = User.builder()
                .username(request.getUsername().trim())
                .passwordHash(encoder.encode(request.getPassword()))
                .email(request.getEmail().trim())
                .fullName(request.getFullName().trim())
                .phone(normalizedPhone)
                .role(userRole)
                .build();

        userRepository.save(user);
        return new MessageResponse("Đăng ký tài khoản CUSTOMER thành công");
    }

    private User resolveLoginAccount(String identifier) {
        return userRepository.findByUsername(identifier)
                .orElseGet(() -> {
                    if (!VietnamPhoneUtils.isValid(identifier)) {
                        throw new BadCredentialsException("Username hoặc số điện thoại không tồn tại");
                    }
                    String normalized = VietnamPhoneUtils.normalize(identifier);
                    String local = VietnamPhoneUtils.toLocal(normalized);
                    return userRepository.findByPhone(normalized)
                            .or(() -> userRepository.findByPhone(local))
                            .orElseThrow(() -> new BadCredentialsException("Username hoặc số điện thoại không tồn tại"));
                });
    }
}
