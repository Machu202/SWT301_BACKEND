// Vị trí: src/main/java/com/swt301/ecommerce/repository/AddressRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    List<Address> findByUser_UserId(Integer userId);
    Optional<Address> findByAddressIdAndUser_UserId(Integer addressId, Integer userId);
}