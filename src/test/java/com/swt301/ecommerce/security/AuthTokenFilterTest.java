package com.swt301.ecommerce.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthTokenFilterTest {
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test void validBearerTokenSetsAuthenticationAndContinuesChain() throws Exception {
        JwtUtils jwt = mock(JwtUtils.class);
        UserDetailsServiceImpl users = mock(UserDetailsServiceImpl.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", users);
        UserDetailsImpl details = new UserDetailsImpl(1, "user", "u@example.com", "x", List.of());
        when(jwt.validateJwtToken("token")).thenReturn(true);
        when(jwt.getUserNameFromJwtToken("token")).thenReturn("user");
        when(users.loadUserByUsername("user")).thenReturn(details);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("user");
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test void missingOrInvalidTokenDoesNotAuthenticateButContinues() throws Exception {
        JwtUtils jwt = mock(JwtUtils.class);
        UserDetailsServiceImpl users = mock(UserDetailsServiceImpl.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", users);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwt, users);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test void tokenProcessingFailureIsContained() throws Exception {
        JwtUtils jwt = mock(JwtUtils.class);
        UserDetailsServiceImpl users = mock(UserDetailsServiceImpl.class);
        AuthTokenFilter filter = new AuthTokenFilter();
        ReflectionTestUtils.setField(filter, "jwtUtils", jwt);
        ReflectionTestUtils.setField(filter, "userDetailsService", users);
        when(jwt.validateJwtToken("broken")).thenThrow(new RuntimeException("boom"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer broken");
        MockFilterChain chain = new MockFilterChain();
        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isSameAs(request);
    }
}
