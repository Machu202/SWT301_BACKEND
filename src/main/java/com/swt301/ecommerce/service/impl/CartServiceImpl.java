// Vị trí: src/main/java/com/swt301/ecommerce/service/impl/CartServiceImpl.java
package com.swt301.ecommerce.service.impl;

import com.swt301.ecommerce.dto.request.CartItemRequest;
import com.swt301.ecommerce.dto.response.CartResponse;
import com.swt301.ecommerce.entity.Cart;
import com.swt301.ecommerce.entity.CartItem;
import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.entity.User;
import com.swt301.ecommerce.repository.CartItemRepository;
import com.swt301.ecommerce.repository.CartRepository;
import com.swt301.ecommerce.repository.ProductRepository;
import com.swt301.ecommerce.repository.UserRepository;
import com.swt301.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    public CartResponse getCart(Integer userId) {
        Cart cart = getOrCreateCart(userId);
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(Integer userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        if (product.getStock() < request.getQuantity()) {
            throw new RuntimeException("Số lượng sản phẩm trong kho không đủ");
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCart_CartIdAndProduct_ProductId(
                cart.getCartId(), product.getProductId());

        // Trích đoạn file: src/main/java/com/swt301/ecommerce/service/impl/CartServiceImpl.java

        if (existingItem.isPresent()) {
            // Đã có trong giỏ -> Cộng dồn
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            cartItemRepository.save(item);
        } else {
            // Chưa có -> Tạo mới
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .unitPrice(product.getPrice()) 
                    .build();
            cartItemRepository.save(newItem);
            
            // 👉 THÊM DÒNG NÀY: Đồng bộ vào bộ nhớ RAM của Hibernate
            cart.getCartItems().add(newItem); 
        }

        return mapToCartResponse(cart); // Không cần gọi lại getOrCreateCart() nữa cho nhẹ máy

        
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Integer userId, CartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCart_CartIdAndProduct_ProductId(cart.getCartId(), request.getProductId())
                .orElseThrow(() -> new RuntimeException("Sản phẩm không có trong giỏ hàng"));

        if (item.getProduct().getStock() < request.getQuantity()) {
            throw new RuntimeException("Số lượng sản phẩm trong kho không đủ");
        }

        // Sửa số lượng (ghi đè, không cộng dồn)
        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse removeCartItem(Integer userId, Integer cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm trong giỏ"));

        if (!item.getCart().getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa sản phẩm này");
        }

        Cart cart = item.getCart(); // Lấy giỏ hàng hiện tại
        cartItemRepository.delete(item);
        
        // 👉 THÊM DÒNG NÀY: Xóa khỏi RAM để kết quả trả về không bị dính cục cũ
        cart.getCartItems().remove(item);

        return mapToCartResponse(cart);
    }

    // --- INTERNAL METHODS ---
    private Cart getOrCreateCart(Integer userId) {
        return cartRepository.findByUser_UserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy user"));
            Cart newCart = Cart.builder().user(user).build();
            return cartRepository.save(newCart);
        });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        BigDecimal totalCartPrice = BigDecimal.ZERO;
        
        List<CartResponse.CartItemDto> itemDtos = cart.getCartItems().stream().map(item -> {
            BigDecimal subtotal = item.getUnitPrice().multiply(new BigDecimal(item.getQuantity()));
            return CartResponse.CartItemDto.builder()
                    .cartItemId(item.getCartItemId())
                    .productId(item.getProduct().getProductId())
                    .productName(item.getProduct().getProductName())
                    .productImage(item.getProduct().getImage())
                    .quantity(item.getQuantity())
                    .unitPrice(item.getUnitPrice())
                    .itemSubtotal(subtotal)
                    .build();
        }).collect(Collectors.toList());

        for (CartResponse.CartItemDto dto : itemDtos) {
            totalCartPrice = totalCartPrice.add(dto.getItemSubtotal());
        }

        return CartResponse.builder()
                .cartId(cart.getCartId())
                .items(itemDtos)
                .totalCartPrice(totalCartPrice)
                .build();
    }
}