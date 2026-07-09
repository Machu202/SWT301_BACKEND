// Vị trí: src/main/java/com/swt301/ecommerce/repository/UserRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username); // Dùng để check trùng username khi Đăng ký
    Boolean existsByEmail(String email);
}