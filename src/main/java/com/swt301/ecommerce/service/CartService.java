// Vị trí: src/main/java/com/swt301/ecommerce/service/CartService.java
package com.swt301.ecommerce.service;

import com.swt301.ecommerce.dto.request.CartItemRequest;
import com.swt301.ecommerce.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Integer userId);
    CartResponse addToCart(Integer userId, CartItemRequest request);
    CartResponse updateCartItem(Integer userId, CartItemRequest request);
    CartResponse removeCartItem(Integer userId, Integer cartItemId);
}