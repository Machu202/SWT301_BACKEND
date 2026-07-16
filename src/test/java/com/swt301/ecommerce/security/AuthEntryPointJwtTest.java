package com.swt301.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuthEntryPointJwtTest {
    @Test void writesStructured401Json() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/api/orders");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AuthEntryPointJwt().commence(request, response, new BadCredentialsException("Bad token"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).contains("application/json");
        Map<?, ?> body = new ObjectMapper().readValue(response.getContentAsByteArray(), Map.class);
        assertThat(body.get("status")).isEqualTo(401);
        assertThat(body.get("path")).isEqualTo("/api/orders");
    }
}
