package cn.edu.techgroup.outsourcing.modules.progress.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record CreateProgressCommand(

        @NotNull
        @PositiveOrZero
        Integer requestVersion,

        @NotNull
        @Min(0)
        @Max(100)
        Integer progress,

        @NotBlank
        @Size(min = 5, max = 2000)
        String content,

        @Size(max = 2000)
        String nextPlan,

        @Future
        Instant nextUpdateAt,

        @NotNull
        Boolean visibleToRequester) {

    public CreateProgressCommand {
        content = trim(content);
        nextPlan = trimToNull(nextPlan);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        String trimmed = trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
