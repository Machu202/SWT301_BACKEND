// Vị trí: src/main/java/com/swt301/ecommerce/repository/CartItemRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    Optional<CartItem> findByCart_CartIdAndProduct_ProductId(Integer cartId, Integer productId);
}