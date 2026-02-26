package ru.just.monolithmvp.dto.course;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

public record UserInNotInRequest(
        @ArraySchema(schema = @Schema(description = "ID сущности, входящей в список", example = "1"))
        Set<Long> idsIn,
        @ArraySchema(schema = @Schema(description = "ID сущности, не входящей в список", example = "1"))
        Set<Long> idsNotIn

) {
    public UserInNotInRequest {
        idsIn = idsIn == null ? Set.of() : Set.copyOf(idsIn);
        idsNotIn = idsNotIn == null ? Set.of() : Set.copyOf(idsNotIn);
    }
}
