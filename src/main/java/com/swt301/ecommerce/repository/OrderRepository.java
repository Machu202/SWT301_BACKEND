package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {
    Optional<Order> findByOrderCode(String orderCode);
    boolean existsByOrderCode(String orderCode);
    boolean existsByAddress_AddressId(Integer addressId);

    @EntityGraph(attributePaths = {"address", "voucher", "paymentMethod", "status"})
    List<Order> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    @EntityGraph(attributePaths = {"address", "voucher", "paymentMethod", "status"})
    Page<Order> findByUser_UserId(Integer userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "address", "voucher", "paymentMethod", "status"})
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"address", "voucher", "paymentMethod", "status"})
    Optional<Order> findByUser_UserIdAndIdempotencyKey(Integer userId, String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o join fetch o.status join fetch o.paymentMethod where o.orderId = :orderId")
    Optional<Order> findByOrderId(@Param("orderId") Integer orderId);
}
