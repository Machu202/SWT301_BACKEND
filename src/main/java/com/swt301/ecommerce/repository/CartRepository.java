// Vị trí: src/main/java/com/swt301/ecommerce/repository/CartRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Cart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Integer> {
    Optional<Cart> findByUser_UserId(Integer userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.user.userId = :userId")
    Optional<Cart> findByUserIdForUpdate(@Param("userId") Integer userId);
}