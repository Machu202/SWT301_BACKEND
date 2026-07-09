// Vị trí: src/main/java/com/swt301/ecommerce/repository/CartRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser_UserId(Integer userId);
}