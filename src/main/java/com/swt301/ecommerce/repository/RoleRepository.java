// Vị trí: src/main/java/com/swt301/ecommerce/repository/RoleRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(String roleName);
}