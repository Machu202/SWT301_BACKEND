package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<?> getMyAddresses(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(addressService.getUserAddresses(currentUser.getId()));
    }

    @PostMapping
    public ResponseEntity<?> createAddress(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.createAddress(currentUser.getId(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(
            @PathVariable("id") Integer addressId,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(addressService.updateAddress(addressId, currentUser.getId(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(
            @PathVariable("id") Integer addressId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(addressService.deleteAddress(addressId, currentUser.getId()));
    }
}
