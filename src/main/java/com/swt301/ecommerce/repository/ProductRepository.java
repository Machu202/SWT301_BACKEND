package com.swt301.ecommerce.repository;

import com.swt301.ecommerce.entity.Product;
import com.swt301.ecommerce.enums.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p join fetch p.category where p.productId = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Integer productId);

    @EntityGraph(attributePaths = "category")
    Optional<Product> findByProductIdAndStatus(Integer productId, ProductStatus status);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where p.status = :status
              and (:categoryId is null or p.category.categoryId = :categoryId)
              and (:search = '' or lower(p.productName) like lower(concat('%', :search, '%'))
                   or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Product> searchPublic(
            @Param("status") ProductStatus status,
            @Param("search") String search,
            @Param("categoryId") Integer categoryId,
            Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("""
            select p from Product p
            where (:categoryId is null or p.category.categoryId = :categoryId)
              and (:search = '' or lower(p.productName) like lower(concat('%', :search, '%'))
                   or lower(coalesce(p.description, '')) like lower(concat('%', :search, '%')))
            """)
    Page<Product> searchAdmin(
            @Param("search") String search,
            @Param("categoryId") Integer categoryId,
            Pageable pageable);
}
