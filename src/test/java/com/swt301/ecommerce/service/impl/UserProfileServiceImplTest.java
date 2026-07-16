package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.ProfileUpdateRequest;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.exception.ConflictException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {
    @Mock UserRepository repository;
    @InjectMocks UserProfileServiceImpl service;

    @Test void getsProfileWithoutPassword() {
        User user = TestFixtures.user(1, "CUSTOMER");
        when(repository.findById(1)).thenReturn(Optional.of(user));
        var response = service.getProfile(1);
        assertThat(response.getUsername()).isEqualTo("user1");
        assertThat(response.getRole()).isEqualTo("CUSTOMER");
    }

    @Test void getMissingProfileFails() {
        when(repository.findById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getProfile(1)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void updatesAndNormalizesPhone() {
        User user = TestFixtures.user(1, "CUSTOMER");
        when(repository.findById(1)).thenReturn(Optional.of(user));
        when(repository.existsByPhoneAndUserIdNot(anyString(), eq(1))).thenReturn(false);
        when(repository.save(user)).thenReturn(user);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName(" Updated Name "); request.setPhone("0912345678");
        var response = service.updateProfile(1, request);
        assertThat(response.getFullName()).isEqualTo("Updated Name");
        assertThat(response.getPhone()).isEqualTo("+84912345678");
    }

    @Test void rejectsPhoneOwnedByAnotherUser() {
        User user = TestFixtures.user(1, "CUSTOMER");
        when(repository.findById(1)).thenReturn(Optional.of(user));
        when(repository.existsByPhoneAndUserIdNot("+84912345678", 1)).thenReturn(true);
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setFullName("Name"); request.setPhone("0912345678");
        assertThatThrownBy(() -> service.updateProfile(1, request)).isInstanceOf(ConflictException.class);
    }
}
