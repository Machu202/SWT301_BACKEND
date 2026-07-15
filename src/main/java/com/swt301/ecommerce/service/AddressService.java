package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.dto.response.AddressResponse;
import com.swt301.ecommerce.dto.response.MessageResponse;

import java.util.List;

public interface AddressService {
    List<AddressResponse> getUserAddresses(Integer userId);
    AddressResponse createAddress(Integer userId, AddressRequest request);
    AddressResponse updateAddress(Integer addressId, Integer userId, AddressRequest request);
    MessageResponse deleteAddress(Integer addressId, Integer userId);
}
