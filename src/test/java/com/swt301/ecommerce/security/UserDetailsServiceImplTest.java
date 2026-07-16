package com.swt301.ecommerce.security;

import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {
    @Mock UserRepository repository;
    @InjectMocks UserDetailsServiceImpl service;

    @Test void loadsUserWithRoleAuthority() {
        User user = TestFixtures.user(1, "ADMIN");
        when(repository.findByUsername("admin")).thenReturn(Optional.of(user));
        user.setUsername("admin");
        var details = service.loadUserByUsername("admin");
        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test void missingUserThrowsUsernameNotFound() {
        when(repository.findByUsername("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing")).isInstanceOf(UsernameNotFoundException.class);
    }
}
