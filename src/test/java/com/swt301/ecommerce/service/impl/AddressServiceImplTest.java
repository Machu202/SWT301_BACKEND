package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.entity.Address;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.exception.ConflictException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.AddressRepository;
import com.swt301.ecommerce.repository.OrderRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {
    @Mock AddressRepository addressRepository;
    @Mock UserRepository userRepository;
    @Mock OrderRepository orderRepository;
    @InjectMocks AddressServiceImpl service;

    @Test void getsAndMapsUserAddresses() {
        User user = TestFixtures.user(1, "CUSTOMER");
        when(addressRepository.findByUser_UserId(1)).thenReturn(List.of(TestFixtures.address(9, user, true)));
        var result = service.getUserAddresses(1);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAddressId()).isEqualTo(9);
        assertThat(result.get(0).getReceiverPhone()).isEqualTo("+84912345678");
    }

    @Test void createsAddressAndUnsetsPreviousDefault() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Address previous = TestFixtures.address(1, user, true);
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(addressRepository.findByUser_UserId(1)).thenReturn(List.of(previous));
        when(addressRepository.save(any(Address.class))).thenAnswer(inv -> {
            Address a = inv.getArgument(0); a.setAddressId(2); return a;
        });
        AddressRequest request = request(true);
        var result = service.createAddress(1, request);
        assertThat(previous.getIsDefault()).isFalse();
        assertThat(result.getAddressId()).isEqualTo(2);
        assertThat(result.getReceiverPhone()).isEqualTo("+84912345678");
        verify(addressRepository).saveAll(List.of(previous));
    }


    @Test void firstAddressIsAlwaysDefaultEvenWhenRequestIsFalse() {
        User user = TestFixtures.user(1, "CUSTOMER");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(addressRepository.findByUser_UserId(1)).thenReturn(List.of());
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> {
            Address address = invocation.getArgument(0);
            address.setAddressId(1);
            return address;
        });
        var result = service.createAddress(1, request(false));
        assertThat(result.getIsDefault()).isTrue();
    }

    @Test void uncheckingCurrentDefaultPromotesAnotherAddress() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Address current = TestFixtures.address(3, user, true);
        Address replacement = TestFixtures.address(4, user, false);
        when(addressRepository.findByAddressIdAndUser_UserId(3, 1)).thenReturn(Optional.of(current));
        when(addressRepository.findByUser_UserId(1)).thenReturn(List.of(current, replacement));
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service.updateAddress(3, 1, request(false));
        assertThat(current.getIsDefault()).isFalse();
        assertThat(replacement.getIsDefault()).isTrue();
        verify(addressRepository).save(replacement);
    }

    @Test void onlyAddressCannotLoseDefaultStatus() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Address current = TestFixtures.address(3, user, true);
        when(addressRepository.findByAddressIdAndUser_UserId(3, 1)).thenReturn(Optional.of(current));
        when(addressRepository.findByUser_UserId(1)).thenReturn(List.of(current));
        when(addressRepository.save(current)).thenReturn(current);
        var result = service.updateAddress(3, 1, request(false));
        assertThat(result.getIsDefault()).isTrue();
    }

    @Test void createFailsWhenUserMissing() {
        when(userRepository.findById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createAddress(1, request(false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void updatesOwnedAddress() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Address address = TestFixtures.address(3, user, false);
        when(addressRepository.findByAddressIdAndUser_UserId(3, 1)).thenReturn(Optional.of(address));
        when(addressRepository.save(address)).thenReturn(address);
        AddressRequest request = request(false);
        request.setReceiverName(" Updated ");
        var result = service.updateAddress(3, 1, request);
        assertThat(result.getReceiverName()).isEqualTo("Updated");
    }

    @Test void updateFailsWhenAddressNotOwned() {
        when(addressRepository.findByAddressIdAndUser_UserId(3, 1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateAddress(3, 1, request(false)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test void deleteRejectsAddressUsedByOrder() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Address address = TestFixtures.address(3, user, false);
        when(addressRepository.findByAddressIdAndUser_UserId(3, 1)).thenReturn(Optional.of(address));
        when(orderRepository.existsByAddress_AddressId(3)).thenReturn(true);
        assertThatThrownBy(() -> service.deleteAddress(3, 1)).isInstanceOf(ConflictException.class);
        verify(addressRepository, never()).delete(any());
    }

    @Test void deletingDefaultPromotesRemainingAddress() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Address deleted = TestFixtures.address(3, user, true);
        Address replacement = TestFixtures.address(4, user, false);
        when(addressRepository.findByAddressIdAndUser_UserId(3, 1)).thenReturn(Optional.of(deleted));
        when(orderRepository.existsByAddress_AddressId(3)).thenReturn(false);
        when(addressRepository.findByUser_UserId(1)).thenReturn(List.of(replacement));
        var response = service.deleteAddress(3, 1);
        assertThat(response.getMessage()).contains("thành công");
        assertThat(replacement.getIsDefault()).isTrue();
        verify(addressRepository).flush();
        verify(addressRepository).save(replacement);
    }

    private AddressRequest request(boolean isDefault) {
        AddressRequest request = new AddressRequest();
        request.setReceiverName(" Receiver ");
        request.setReceiverPhone("0912345678");
        request.setProvince(" Tien Giang ");
        request.setDistrict(" My Tho ");
        request.setWard(" Ward 1 ");
        request.setStreet(" 2D Street ");
        request.setIsDefault(isDefault);
        return request;
    }
}
