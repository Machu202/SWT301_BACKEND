// Vị trí: src/main/java/com/swt301/ecommerce/repository/CategoryRepository.java
package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
}