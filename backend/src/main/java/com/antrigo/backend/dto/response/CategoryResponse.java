package com.antrigo.backend.dto.response;

import com.antrigo.backend.domain.entity.Category;

public record CategoryResponse(Long id, String name, String slug, int sortOrder) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getSortOrder());
    }
}
