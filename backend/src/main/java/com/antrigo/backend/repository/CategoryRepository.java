package com.antrigo.backend.repository;

import com.antrigo.backend.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByActiveTrueOrderBySortOrderAsc();
    boolean existsBySlug(String slug);
}
