package cn.edu.techgroup.outsourcing.modules.category.vo;

import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;
import java.time.Instant;

public record AdminCategoryVO(
        String id,
        String name,
        int sortOrder,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt) {

    public static AdminCategoryVO from(CategoryEntity category) {
        return new AdminCategoryVO(
                category.getId().toString(),
                category.getName(),
                category.getSortOrder(),
                Boolean.TRUE.equals(category.getEnabled()),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
