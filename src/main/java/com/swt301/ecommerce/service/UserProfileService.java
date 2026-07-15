package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.ProfileUpdateRequest;
import com.swt301.ecommerce.dto.response.ProfileResponse;

public interface UserProfileService {
    ProfileResponse getProfile(Integer userId);
    ProfileResponse updateProfile(Integer userId, ProfileUpdateRequest request);
}
