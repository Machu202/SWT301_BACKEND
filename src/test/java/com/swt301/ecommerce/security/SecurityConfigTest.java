package com.swt301.ecommerce.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SecurityConfigTest {
    @Test void createsSecurityBeans() throws Exception {
        UserDetailsServiceImpl userService = mock(UserDetailsServiceImpl.class);
        AuthEntryPointJwt entryPoint = mock(AuthEntryPointJwt.class);
        SecurityConfig config = new SecurityConfig(userService, entryPoint);
        assertThat(config.authenticationJwtTokenFilter()).isNotNull();
        assertThat(config.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(config.authenticationProvider()).isNotNull();
        AuthenticationConfiguration authConfig = mock(AuthenticationConfiguration.class);
        AuthenticationManager manager = mock(AuthenticationManager.class);
        when(authConfig.getAuthenticationManager()).thenReturn(manager);
        assertThat(config.authenticationManager(authConfig)).isSameAs(manager);
    }
}
