// Vị trí: src/main/java/com/swt301/ecommerce/repository/ProductRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
}