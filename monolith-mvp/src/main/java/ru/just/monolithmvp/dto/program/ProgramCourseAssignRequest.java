package ru.just.monolithmvp.dto.program;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

public record ProgramCourseAssignRequest(
        @ArraySchema(schema = @Schema(description = "ID курса, входящего в программу", example = "1"))
        List<Long> idsIn,
        @ArraySchema(schema = @Schema(description = "ID курса, исключаемого из программы", example = "1"))
        List<Long> idsNotIn
) {
    public ProgramCourseAssignRequest(
            @ArraySchema(schema = @Schema(description = "ID курса, входящего в программу", example = "1"))
            List<Long> idsIn,
            @ArraySchema(schema = @Schema(description = "ID курса, исключаемого из программы", example = "1"))
            List<Long> idsNotIn
    ) {
        this.idsIn = idsIn == null ? new ArrayList<>() : idsIn;
        this.idsNotIn = idsNotIn == null ? new ArrayList<>() : idsNotIn;
    }
}
