package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.dto.response.AddressResponse;
import com.swt301.ecommerce.dto.response.MessageResponse;
import com.swt301.ecommerce.entity.Address;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.repository.AddressRepository;
import com.swt301.ecommerce.repository.OrderRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.service.AddressService;
import com.swt301.ecommerce.util.VietnamPhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public List<AddressResponse> getUserAddresses(Integer userId) {
        return addressRepository.findByUser_UserId(userId).stream().map(this::map).toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Integer userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        handleDefaultAddress(userId, request.getIsDefault());

        Address address = Address.builder()
                .user(user)
                .receiverName(request.getReceiverName().trim())
                .receiverPhone(VietnamPhoneUtils.normalize(request.getReceiverPhone()))
                .province(request.getProvince().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard().trim())
                .street(request.getStreet().trim())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        return map(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Integer addressId, Integer userId, AddressRequest request) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hoặc bạn không có quyền sửa"));

        handleDefaultAddress(userId, request.getIsDefault());

        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(VietnamPhoneUtils.normalize(request.getReceiverPhone()));
        address.setProvince(request.getProvince().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard().trim());
        address.setStreet(request.getStreet().trim());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));

        return map(addressRepository.save(address));
    }

    @Override
    @Transactional
    public MessageResponse deleteAddress(Integer addressId, Integer userId) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy địa chỉ hoặc bạn không có quyền xóa"));
        if (orderRepository.existsByAddress_AddressId(addressId)) {
            throw new RuntimeException("Không thể xóa địa chỉ đã được sử dụng trong đơn hàng");
        }

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        addressRepository.delete(address);
        addressRepository.flush();

        if (wasDefault) {
            List<Address> remaining = addressRepository.findByUser_UserId(userId);
            if (!remaining.isEmpty()) {
                Address replacement = remaining.get(0);
                replacement.setIsDefault(true);
                addressRepository.save(replacement);
            }
        }
        return new MessageResponse("Xóa địa chỉ thành công");
    }

    private void handleDefaultAddress(Integer userId, Boolean isDefault) {
        if (Boolean.TRUE.equals(isDefault)) {
            List<Address> addresses = addressRepository.findByUser_UserId(userId);
            for (Address addr : addresses) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                }
            }
            addressRepository.saveAll(addresses);
        }
    }

    private AddressResponse map(Address address) {
        return AddressResponse.builder()
                .addressId(address.getAddressId())
                .receiverName(address.getReceiverName())
                .receiverPhone(address.getReceiverPhone())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .street(address.getStreet())
                .isDefault(Boolean.TRUE.equals(address.getIsDefault()))
                .build();
    }
}
