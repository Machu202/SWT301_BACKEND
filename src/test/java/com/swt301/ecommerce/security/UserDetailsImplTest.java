package com.swt301.ecommerce.security;

import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDetailsImplTest {
    @Test void buildsFromEntityAndReportsEnabledAccountFlags() {
        var user = TestFixtures.user(3, "CUSTOMER");
        var details = UserDetailsImpl.build(user);
        assertThat(details.getId()).isEqualTo(3);
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_CUSTOMER");
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
        assertThat(details.isEnabled()).isTrue();
    }
}
