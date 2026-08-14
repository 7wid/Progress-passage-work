package cn.edu.techgroup.outsourcing.modules.category.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAdminCategoryCommand(
        @NotBlank @Size(max = 80) String name,
        @NotNull @Min(0) @Max(9999) Integer sortOrder,
        @NotBlank @Size(min = 5, max = 500) String reason) {}
