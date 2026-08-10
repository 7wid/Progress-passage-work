package cn.edu.techgroup.outsourcing.modules.evaluation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ConfirmRejectionCommand(

        @NotNull
        @PositiveOrZero
        Integer requestVersion) {
}