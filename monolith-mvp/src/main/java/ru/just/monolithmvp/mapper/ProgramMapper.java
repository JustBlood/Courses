package ru.just.monolithmvp.mapper;

import org.mapstruct.Mapper;
import ru.just.monolithmvp.dto.program.ProgramSummaryDto;
import ru.just.monolithmvp.model.LearningProgram;

@Mapper(componentModel = "spring")
public interface ProgramMapper {

    ProgramSummaryDto toSummaryDto(LearningProgram program);

}
