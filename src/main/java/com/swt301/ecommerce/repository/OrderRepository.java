package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
    boolean existsByAddress_AddressId(Integer addressId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findByOrderId(Integer orderId);
}
