package ru.just.monolithmvp.dto.program;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public record ProgramGroupAssignRequest(
        @ArraySchema(schema = @Schema(description = "UUID группы в idsIn", example = "550e8400-e29b-41d4-a716-446655440000"))
        Set<UUID> idsIn,
        @ArraySchema(schema = @Schema(description = "UUID группы в idsNotIn", example = "550e8400-e29b-41d4-a716-446655440000"))
        Set<UUID> idsNotIn
) {
        public ProgramGroupAssignRequest(@ArraySchema(schema = @Schema(description = "UUID группы в idsIn", example = "550e8400-e29b-41d4-a716-446655440000"))
                                         Set<UUID> idsIn, @ArraySchema(schema = @Schema(description = "UUID группы в idsNotIn", example = "550e8400-e29b-41d4-a716-446655440000"))
                                         Set<UUID> idsNotIn) {
                this.idsIn = idsIn == null ? new HashSet<>() : idsIn;
                this.idsNotIn = idsNotIn == null ? new HashSet<>() : idsNotIn;
        }
}
