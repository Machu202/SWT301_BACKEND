package com.swt301.ecommerce.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class JwtUtilsTest {
    private JwtUtils configured(long expirationMs) {
        JwtUtils jwt = new JwtUtils();
        ReflectionTestUtils.setField(jwt, "jwtSecret", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
        ReflectionTestUtils.setField(jwt, "jwtExpirationMs", expirationMs);
        return jwt;
    }

    @Test void validatesConfigurationAndRoundTripsToken() {
        JwtUtils jwt = configured(60_000);
        assertThatCode(jwt::validateConfiguration).doesNotThrowAnyException();
        UserDetailsImpl principal = new UserDetailsImpl(1, "customer", "c@example.com", "encoded", List.of());
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String token = jwt.generateJwtToken(auth);
        assertThat(jwt.validateJwtToken(token)).isTrue();
        assertThat(jwt.getUserNameFromJwtToken(token)).isEqualTo("customer");
    }

    @Test void rejectsBlankShortSecretAndInvalidExpiration() {
        JwtUtils blank = configured(1000);
        ReflectionTestUtils.setField(blank, "jwtSecret", " ");
        assertThatThrownBy(blank::validateConfiguration).isInstanceOf(IllegalStateException.class);

        JwtUtils shortSecret = configured(1000);
        ReflectionTestUtils.setField(shortSecret, "jwtSecret", "short");
        assertThatThrownBy(shortSecret::validateConfiguration).isInstanceOf(IllegalStateException.class);

        JwtUtils expiration = configured(0);
        assertThatThrownBy(expiration::validateConfiguration).isInstanceOf(IllegalStateException.class);
    }

    @Test void rejectsMalformedAndExpiredTokens() throws InterruptedException {
        JwtUtils jwt = configured(5);
        jwt.validateConfiguration();
        UserDetailsImpl principal = new UserDetailsImpl(1, "customer", "c@example.com", "encoded", List.of());
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        String token = jwt.generateJwtToken(auth);
        Thread.sleep(15);
        assertThat(jwt.validateJwtToken(token)).isFalse();
        assertThat(jwt.validateJwtToken("not-a-token")).isFalse();
    }
}
