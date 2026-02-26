package ru.just.monolithmvp.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record EnrollmentRequest(
        @ArraySchema(schema = @Schema(description = "ID сущности", example = "1"))
        List<Long> idsToEnroll,
        @ArraySchema(schema = @Schema(description = "ID сущности", example = "1"))
        List<Long> idsToUnenroll

) {
    public EnrollmentRequest {
        idsToEnroll = idsToEnroll == null ? List.of() : List.copyOf(idsToEnroll);
        idsToUnenroll = idsToUnenroll == null ? List.of() : List.copyOf(idsToUnenroll);
    }
}
