package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.LoginRequest;
import com.swt301.ecommerce.dto.request.RegisterRequest;
import com.swt301.ecommerce.entity.Role;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.exception.ConflictException;
import com.swt301.ecommerce.repository.RoleRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.security.JwtUtils;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock AuthenticationManager authenticationManager;
    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock PasswordEncoder encoder;
    @Mock JwtUtils jwtUtils;
    @InjectMocks AuthServiceImpl service;

    @AfterEach void clearContext() { SecurityContextHolder.clearContext(); }

    @Test void loginByUsernameReturnsJwtAndRole() {
        User user = TestFixtures.user(1, "CUSTOMER");
        LoginRequest request = login("user1", "secret");
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl principal = new UserDetailsImpl(1, "user1", "user1@example.com", "encoded", List.of());
        when(userRepository.findByUsername("user1")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt");
        var result = service.login(request);
        assertThat(result.getToken()).isEqualTo("jwt");
        assertThat(result.getRole()).isEqualTo("ROLE_CUSTOMER");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
    }

    @Test void loginByPhoneUsesNormalizedAndLocalLookup() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Authentication authentication = mock(Authentication.class);
        UserDetailsImpl principal = new UserDetailsImpl(1, "user1", "user1@example.com", "encoded", List.of());
        when(userRepository.findByUsername("0912345678")).thenReturn(Optional.empty());
        when(userRepository.findByPhone("+84912345678")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("jwt");
        assertThat(service.login(login("0912345678", "secret")).getId()).isEqualTo(1);
    }

    @Test void loginRejectsUnknownIdentifierAndInvalidRole() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(login("unknown", "secret"))).isInstanceOf(BadCredentialsException.class);

        User invalid = TestFixtures.user(2, "EDITOR");
        when(userRepository.findByUsername("user2")).thenReturn(Optional.of(invalid));
        assertThatThrownBy(() -> service.login(login("user2", "secret"))).isInstanceOf(AccessDeniedException.class);
    }

    @Test void registerCustomerNormalizesAndPersistsData() {
        RegisterRequest request = register();
        Role customer = TestFixtures.role("CUSTOMER");
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(roleRepository.findByRoleName("CUSTOMER")).thenReturn(Optional.of(customer));
        when(encoder.encode("secret1")).thenReturn("encoded");
        var response = service.register(request);
        assertThat(response.getMessage()).contains("CUSTOMER");
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@example.com");
        assertThat(captor.getValue().getPhone()).isEqualTo("+84912345678");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
    }

    @Test void registerRejectsAdminAndDuplicates() {
        RegisterRequest request = register();
        request.setRole("ADMIN");
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(AccessDeniedException.class);

        request.setRole("CUSTOMER");
        when(userRepository.existsByUsername("newuser")).thenReturn(true);
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ConflictException.class);
    }

    @Test void registerRejectsDuplicateEmailAndPhone() {
        RegisterRequest request = register();
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ConflictException.class);

        when(userRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        when(userRepository.existsByPhone("+84912345678")).thenReturn(true);
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ConflictException.class);
    }

    private LoginRequest login(String username, String password) {
        LoginRequest request = new LoginRequest(); request.setUsername(username); request.setPassword(password); return request;
    }
    private RegisterRequest register() {
        RegisterRequest r = new RegisterRequest();
        r.setUsername("newuser"); r.setPassword("secret1"); r.setFullName(" New User ");
        r.setEmail(" NEW@EXAMPLE.COM "); r.setPhone("0912345678"); r.setRole("CUSTOMER");
        return r;
    }
}
