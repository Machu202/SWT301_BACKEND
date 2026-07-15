package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Payment;
import com.swt301.ecommerce.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    @EntityGraph(attributePaths = {"paymentMethod", "order"})
    Optional<Payment> findByOrder_OrderId(Integer orderId);

    @EntityGraph(attributePaths = {"paymentMethod", "order"})
    List<Payment> findByOrder_OrderIdIn(Collection<Integer> orderIds);


    long countByPaymentStatus(PaymentStatus paymentStatus);

    @Query("select sum(p.amount) from Payment p where p.paymentStatus = :status")
    BigDecimal sumAmountByPaymentStatus(@Param("status") PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order o join fetch o.status join fetch o.paymentMethod where o.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") Integer orderId);
}
