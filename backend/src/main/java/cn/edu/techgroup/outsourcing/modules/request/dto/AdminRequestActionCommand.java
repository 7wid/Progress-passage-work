package cn.edu.techgroup.outsourcing.modules.request.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminRequestActionCommand(
        @NotNull @Min(0) Integer expectedVersion,
        @NotBlank @Size(min = 5, max = 500) String reason) {}
