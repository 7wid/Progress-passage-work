package cn.edu.techgroup.outsourcing.modules.evaluation.dto;

import cn.edu.techgroup.outsourcing.modules.evaluation.enums.EvaluationConclusion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateEvaluationCommand(

        @NotNull
        @PositiveOrZero
        Integer requestVersion,

        @NotNull
        EvaluationConclusion conclusion,

        @NotBlank
        @Size(min = 10, max = 5000)
        String publicComment,

        @Size(max = 5000)
        String solutionSummary,

        @DecimalMin("0.01")
        @Digits(integer = 6, fraction = 2)
        BigDecimal estimatedWorkload,

        @Future
        Instant estimatedFinishAt,

        @Size(max = 500)
        String requiredSkills,

        @Size(max = 5000)
        String risks,

        @Size(max = 5000)
        String internalNote) {

    public CreateEvaluationCommand {
        publicComment = trim(publicComment);
        solutionSummary = trimToNull(solutionSummary);
        requiredSkills = trimToNull(requiredSkills);
        risks = trimToNull(risks);
        internalNote = trimToNull(internalNote);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}