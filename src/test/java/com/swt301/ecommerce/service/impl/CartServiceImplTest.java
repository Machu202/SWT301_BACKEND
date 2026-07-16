package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.CartItemRequest;
import com.swt301.ecommerce.entity.Cart;
import com.swt301.ecommerce.entity.CartItem;
import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.enums.ProductStatus;
import com.swt301.ecommerce.exception.BusinessRuleException;
import com.swt301.ecommerce.exception.ResourceNotFoundException;
import com.swt301.ecommerce.repository.CartItemRepository;
import com.swt301.ecommerce.repository.CartRepository;
import com.swt301.ecommerce.repository.ProductRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {
    @Mock CartRepository cartRepository;
    @Mock CartItemRepository cartItemRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @InjectMocks CartServiceImpl service;

    @Test void getCartCreatesCartWhenMissing() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Cart created = TestFixtures.cart(10, user);
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.empty());
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(cartRepository.save(any(Cart.class))).thenReturn(created);
        var result = service.getCart(1);
        assertThat(result.getCartId()).isEqualTo(10);
        assertThat(result.getItems()).isEmpty();
    }

    @Test void addNewItemUsesCurrentPriceAndMapsTotal() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Cart cart = TestFixtures.cart(10, user);
        Product product = TestFixtures.product(5, 12, new BigDecimal("300000"));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(10, 5)).thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(inv -> { CartItem i = inv.getArgument(0); i.setCartItemId(99); return i; });
        var result = service.addToCart(1, request(5, 2));
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getAvailableToAdd()).isEqualTo(10);
        assertThat(result.getTotalCartPrice()).isEqualByComparingTo("600000");
    }

    @Test void addExistingItemStacksButNeverExceedsStock() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Cart cart = TestFixtures.cart(10, user);
        Product product = TestFixtures.product(5, 12, new BigDecimal("100"));
        CartItem item = TestFixtures.cartItem(1, cart, product, 10);
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(10, 5)).thenReturn(Optional.of(item));
        service.addToCart(1, request(5, 2));
        assertThat(item.getQuantity()).isEqualTo(12);
        assertThatThrownBy(() -> service.addToCart(1, request(5, 1))).isInstanceOf(BusinessRuleException.class);
    }

    @Test void addAndUpdateRejectInactiveOrInsufficientStock() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Cart cart = TestFixtures.cart(10, user);
        Product product = TestFixtures.product(5, 2, BigDecimal.TEN);
        product.setStatus(ProductStatus.INACTIVE);
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        assertThatThrownBy(() -> service.addToCart(1, request(5, 1))).isInstanceOf(BusinessRuleException.class);

        product.setStatus(ProductStatus.ACTIVE);
        CartItem item = TestFixtures.cartItem(1, cart, product, 1);
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(10, 5)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.updateCartItem(1, request(5, 3))).isInstanceOf(BusinessRuleException.class);
    }

    @Test void updateRefreshesStoredPrice() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Cart cart = TestFixtures.cart(10, user);
        Product product = TestFixtures.product(5, 10, new BigDecimal("500"));
        CartItem item = TestFixtures.cartItem(1, cart, product, 1);
        item.setUnitPrice(new BigDecimal("100"));
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCart_CartIdAndProduct_ProductId(10, 5)).thenReturn(Optional.of(item));
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        service.updateCartItem(1, request(5, 4));
        assertThat(item.getQuantity()).isEqualTo(4);
        assertThat(item.getUnitPrice()).isEqualByComparingTo("500");
    }

    @Test void removeChecksOwnership() {
        User owner = TestFixtures.user(2, "CUSTOMER");
        Cart cart = TestFixtures.cart(10, owner);
        CartItem item = TestFixtures.cartItem(1, cart, TestFixtures.product(5, 10, BigDecimal.TEN), 1);
        when(cartItemRepository.findById(1)).thenReturn(Optional.of(item));
        assertThatThrownBy(() -> service.removeCartItem(1, 1)).isInstanceOf(AccessDeniedException.class);
        verify(cartItemRepository, never()).delete(any());
    }

    @Test void removeAndClearMutateCart() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Cart cart = TestFixtures.cart(10, user);
        CartItem item = TestFixtures.cartItem(1, cart, TestFixtures.product(5, 10, BigDecimal.TEN), 1);
        when(cartItemRepository.findById(1)).thenReturn(Optional.of(item));
        service.removeCartItem(1, 1);
        assertThat(cart.getCartItems()).isEmpty();
        verify(cartItemRepository).delete(item);

        TestFixtures.cartItem(2, cart, TestFixtures.product(6, 10, BigDecimal.TEN), 1);
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        service.clearCart(1);
        assertThat(cart.getCartItems()).isEmpty();
        verify(cartItemRepository).deleteByCart_CartId(10);
    }

    @Test void missingProductOrCartItemReturnsNotFound() {
        User user = TestFixtures.user(1, "CUSTOMER");
        Cart cart = TestFixtures.cart(10, user);
        when(cartRepository.findByUser_UserId(1)).thenReturn(Optional.of(cart));
        when(productRepository.findById(5)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addToCart(1, request(5, 1))).isInstanceOf(ResourceNotFoundException.class);
    }

    private CartItemRequest request(int productId, int quantity) {
        CartItemRequest request = new CartItemRequest();
        request.setProductId(productId); request.setQuantity(quantity); return request;
    }
}
