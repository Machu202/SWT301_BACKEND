package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.CartItemRequest;
import com.swt301.ecommerce.dto.response.CartResponse;
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
import com.swt301.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Integer userId) {
        return mapToCartResponse(getOrCreateCart(userId));
    }

    @Override
    @Transactional
    public CartResponse addToCart(Integer userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
        ensurePurchasable(product);

        Optional<CartItem> existingItem = cartItemRepository.findByCart_CartIdAndProduct_ProductId(
                cart.getCartId(), product.getProductId());
        int currentQuantity = existingItem.map(CartItem::getQuantity).orElse(0);
        int requestedTotal = currentQuantity + request.getQuantity();
        if (requestedTotal > product.getStock()) {
            throw new BusinessRuleException("Tổng số lượng trong giỏ không được vượt quá tồn kho hiện tại: " + product.getStock());
        }

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(requestedTotal);
            item.setUnitPrice(product.getPrice());
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice())
                    .build();
            cartItemRepository.save(newItem);
            cart.getCartItems().add(newItem);
        }
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Integer userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không có trong giỏ hàng"));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm không tồn tại"));
        ensurePurchasable(product);
        if (product.getStock() < request.getQuantity()) {
            throw new BusinessRuleException("Số lượng sản phẩm trong kho không đủ");
        }
        item.setQuantity(request.getQuantity());
        item.setUnitPrice(product.getPrice());
        cartItemRepository.save(item);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Integer userId, Integer cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ"));
        if (!item.getCart().getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa sản phẩm này");
        }
        Cart cart = item.getCart();
        cartItemRepository.delete(item);
        cart.getCartItems().remove(item);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse clearCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCart_CartId(cart.getCartId());
        cart.getCartItems().clear();
        return mapToCartResponse(cart);
    }

    private Cart getOrCreateCart(Integer userId) {
        return cartRepository.findByUser_UserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy user"));
            return cartRepository.save(Cart.builder().user(user).build());
        });
    }

    private void ensurePurchasable(Product product) {
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessRuleException("Sản phẩm hiện không còn được bán");
        }
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartResponse.CartItemDto> itemDtos = cart.getCartItems().stream().map(item -> {
            Product product = item.getProduct();
            BigDecimal currentUnitPrice = product.getPrice();
            BigDecimal subtotal = currentUnitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            return CartResponse.CartItemDto.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(product.getProductId())
                    .productName(product.getProductName())
                    .productImage(product.getImage())
                    .quantity(item.getQuantity())
                    .productStock(product.getStock())
                    .availableToAdd(Math.max(0, product.getStock() - item.getQuantity()))
                    .unitPrice(currentUnitPrice)
                    .itemSubtotal(subtotal)
                    .build();
        }).toList();
        BigDecimal total = itemDtos.stream()
                .map(CartResponse.CartItemDto::getItemSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(itemDtos)
                .totalCartPrice(total)
                .build();
    }
}
