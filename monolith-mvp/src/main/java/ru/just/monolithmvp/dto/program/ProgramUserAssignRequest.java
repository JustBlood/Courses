package ru.just.monolithmvp.dto.program;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.HashSet;
import java.util.Set;

public record ProgramUserAssignRequest(
        @ArraySchema(schema = @Schema(description = "ID сущности, входящей в список", example = "1"))
        Set<Long> idsIn,
        @ArraySchema(schema = @Schema(description = "ID сущности, не входящей в список", example = "1"))
        Set<Long> idsNotIn
) {
        public ProgramUserAssignRequest(@ArraySchema(schema = @Schema(description = "ID сущности, входящей в список", example = "1"))
                                        Set<Long> idsIn, @ArraySchema(schema = @Schema(description = "ID сущности, не входящей в список", example = "1"))
                                        Set<Long> idsNotIn) {
                this.idsIn = idsIn == null ? new HashSet<>() : idsIn;
                this.idsNotIn = idsNotIn == null ? new HashSet<>() : idsNotIn;
        }
}
