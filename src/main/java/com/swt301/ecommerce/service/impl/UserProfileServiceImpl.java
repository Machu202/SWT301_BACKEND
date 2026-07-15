package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.ProfileUpdateRequest;
import com.swt301.ecommerce.dto.response.ProfileResponse;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.service.UserProfileService;
import com.swt301.ecommerce.util.VietnamPhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {
    private final UserRepository userRepository;

    @Override
    public ProfileResponse getProfile(Integer userId) {
        return map(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng")));
    }

    @Override
    @Transactional
    public ProfileResponse updateProfile(Integer userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        String normalizedPhone = VietnamPhoneUtils.normalize(request.getPhone());
        String localPhone = VietnamPhoneUtils.toLocal(normalizedPhone);
        if (Boolean.TRUE.equals(userRepository.existsByPhoneAndUserIdNot(normalizedPhone, userId))
                || Boolean.TRUE.equals(userRepository.existsByPhoneAndUserIdNot(localPhone, userId))) {
            throw new RuntimeException("Số điện thoại đã được sử dụng bởi tài khoản khác");
        }
        user.setFullName(request.getFullName().trim());
        user.setPhone(normalizedPhone);
        return map(userRepository.save(user));
    }

    private ProfileResponse map(User user) {
        return ProfileResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().getRoleName())
                .build();
    }
}
