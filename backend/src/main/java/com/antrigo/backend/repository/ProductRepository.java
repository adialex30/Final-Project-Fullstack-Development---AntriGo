package com.antrigo.backend.repository;

import com.antrigo.backend.domain.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
        SELECT p FROM Product p
        WHERE p.active = true
        AND (:categoryId IS NULL OR p.category.id = :categoryId)
        AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY p.name ASC
    """)
    Page<Product> search(@Param("search") String search, @Param("categoryId") Long categoryId, Pageable pageable);

    boolean existsBySlug(String slug);

    /** Pessimistic write lock — dipakai saat checkout untuk mencegah race condition pada stok. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock <= :threshold ORDER BY p.stock ASC")
    List<Product> findLowStock(@Param("threshold") int threshold);
}
