package cn.edu.techgroup.outsourcing.modules.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record UpdateRequestMembersCommand(

        @NotNull
        @PositiveOrZero
        Integer requestVersion,

        @NotNull
        @Positive
        Long ownerId,

        @NotNull
        @Size(max = 20)
        List<@NotNull @Positive Long> participantIds,

        @NotBlank
        @Size(min = 5, max = 500)
        String reason) {

    public UpdateRequestMembersCommand {
        participantIds = participantIds == null
                ? null
                : Collections.unmodifiableList(
                        new ArrayList<>(participantIds));

        reason = reason == null
                ? null
                : reason.trim();
    }
}