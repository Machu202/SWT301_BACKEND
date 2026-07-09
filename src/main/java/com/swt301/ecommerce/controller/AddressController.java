// Vị trí: src/main/java/com/swt301/ecommerce/controller/AddressController.java
package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.request.AddressRequest;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}