package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.dto.response.AddressResponse;
import com.swt301.ecommerce.dto.response.MessageResponse;
import com.swt301.ecommerce.entity.Address;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.exception.ConflictException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.AddressRepository;
import com.swt301.ecommerce.repository.OrderRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.service.AddressService;
import com.swt301.ecommerce.util.VietnamPhoneUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getUserAddresses(Integer userId) {
        return orderedAddresses(userId).stream().map(this::map).toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Integer userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        List<Address> existing = orderedAddresses(userId);
        boolean makeDefault = existing.isEmpty()
                || existing.stream().noneMatch(item -> Boolean.TRUE.equals(item.getIsDefault()))
                || Boolean.TRUE.equals(request.getIsDefault());
        if (makeDefault) {
            unsetDefaults(existing, null);
        }

        Address address = Address.builder()
                .user(user)
                .receiverName(request.getReceiverName().trim())
                .receiverPhone(VietnamPhoneUtils.normalize(request.getReceiverPhone()))
                .province(request.getProvince().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard().trim())
                .street(request.getStreet().trim())
                .isDefault(makeDefault)
                .build();

        return map(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Integer addressId, Integer userId, AddressRequest request) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ hoặc bạn không có quyền sửa"));

        List<Address> addresses = orderedAddresses(userId);
        boolean requestedDefault = Boolean.TRUE.equals(request.getIsDefault());
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        if (requestedDefault) {
            unsetDefaults(addresses, addressId);
            address.setIsDefault(true);
        } else if (wasDefault) {
            Address replacement = addresses.stream()
                    .filter(candidate -> !candidate.getAddressId().equals(addressId))
                    .findFirst()
                    .orElse(null);
            if (replacement == null) {
                // A user with saved addresses must always have one default address.
                address.setIsDefault(true);
            } else {
                replacement.setIsDefault(true);
                addressRepository.save(replacement);
                address.setIsDefault(false);
            }
        } else {
            boolean anotherDefaultExists = addresses.stream()
                    .filter(candidate -> !candidate.getAddressId().equals(addressId))
                    .anyMatch(candidate -> Boolean.TRUE.equals(candidate.getIsDefault()));
            address.setIsDefault(!anotherDefaultExists);
        }

        address.setReceiverName(request.getReceiverName().trim());
        address.setReceiverPhone(VietnamPhoneUtils.normalize(request.getReceiverPhone()));
        address.setProvince(request.getProvince().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard().trim());
        address.setStreet(request.getStreet().trim());

        return map(addressRepository.save(address));
    }

    @Override
    @Transactional
    public MessageResponse deleteAddress(Integer addressId, Integer userId) {
        Address address = addressRepository.findByAddressIdAndUser_UserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ hoặc bạn không có quyền xóa"));
        if (orderRepository.existsByAddress_AddressId(addressId)) {
            throw new ConflictException("Không thể xóa địa chỉ đã được sử dụng trong đơn hàng");
        }

        addressRepository.delete(address);
        addressRepository.flush();

        List<Address> remaining = orderedAddresses(userId);
        if (!remaining.isEmpty() && remaining.stream().noneMatch(item -> Boolean.TRUE.equals(item.getIsDefault()))) {
            Address replacement = remaining.get(0);
            replacement.setIsDefault(true);
            addressRepository.save(replacement);
        }
        return new MessageResponse("Xóa địa chỉ thành công");
    }

    private List<Address> orderedAddresses(Integer userId) {
        return addressRepository.findByUser_UserId(userId).stream()
                .sorted(Comparator
                        .comparing((Address item) -> Boolean.TRUE.equals(item.getIsDefault())).reversed()
                        .thenComparing(Address::getAddressId, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private void unsetDefaults(List<Address> addresses, Integer exceptAddressId) {
        List<Address> changed = addresses.stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
                .filter(item -> exceptAddressId == null || !exceptAddressId.equals(item.getAddressId()))
                .peek(item -> item.setIsDefault(false))
                .toList();
        if (!changed.isEmpty()) {
            addressRepository.saveAll(changed);
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
