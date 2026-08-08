package cn.edu.techgroup.outsourcing.modules.category.vo;

import cn.edu.techgroup.outsourcing.modules.category.entity.CategoryEntity;

public record CategoryOptionVO(
        String id,
        String name) {

    public static CategoryOptionVO from(CategoryEntity entity) {
        return new CategoryOptionVO(
                entity.getId().toString(),
                entity.getName());
    }
}