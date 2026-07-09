// Vị trí: src/main/java/com/swt301/ecommerce/service/impl/AddressServiceImpl.java
package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.entity.Address;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.repository.AddressRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public List<Address> getUserAddresses(Integer userId) {
        return addressRepository.findByUser_UserId(userId);
    }

    @Override
    @Transactional
    public Address createAddress(Integer userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));

        handleDefaultAddress(userId, request.getIsDefault());

        Address address = Address.builder()
                .user(user)
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .street(request.getStreet())
                .isDefault(request.getIsDefault() != null ? request.getIsDefault() : false)
                .build();

        return addressRepository.save(address);
    }

    @Override
    @Transactional
    public Address updateAddress(Integer addressId, Integer userId, AddressRequest request) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hoặc bạn không có quyền sửa"));

        handleDefaultAddress(userId, request.getIsDefault());

        address.setReceiverName(request.getReceiverName());
        address.setReceiverPhone(request.getReceiverPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setStreet(request.getStreet());
        
        if (request.getIsDefault() != null) {
            address.setIsDefault(request.getIsDefault());
        }

        return addressRepository.save(address);
    }

    private void handleDefaultAddress(Integer userId, Boolean isDefault) {
        if (Boolean.TRUE.equals(isDefault)) {
            List<Address> addresses = addressRepository.findByUser_UserId(userId);
            for (Address addr : addresses) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }
    }
}