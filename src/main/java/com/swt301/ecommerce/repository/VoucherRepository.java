package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Voucher;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    Optional<Voucher> findByVoucherCode(String voucherCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.voucherCode = :voucherCode")
    Optional<Voucher> findByVoucherCodeForUpdate(@Param("voucherCode") String voucherCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Voucher v where v.voucherId = :voucherId")
    Optional<Voucher> findByIdForUpdate(@Param("voucherId") Integer voucherId);
}
