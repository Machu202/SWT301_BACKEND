// Vị trí: src/main/java/com/swt301/ecommerce/controller/CartController.java
package com.swt301.ecommerce.controller;

import com.swt301.ecommerce.dto.request.CartItemRequest;
import com.swt301.ecommerce.security.UserDetailsImpl;
import com.swt301.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
@org.springframework.security.access.prepost.PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<?> getMyCart(@AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(cartService.getCart(currentUser.getId()));
    }

    @PostMapping("/items")
    public ResponseEntity<?> addToCart(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addToCart(currentUser.getId(), request));
    }

    @PutMapping("/items")
    public ResponseEntity<?> updateCartItem(
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            @Valid @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.updateCartItem(currentUser.getId(), request));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<?> removeCartItem(
            @PathVariable("cartItemId") Integer cartItemId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {
        return ResponseEntity.ok(cartService.removeCartItem(currentUser.getId(), cartItemId));
    }
}