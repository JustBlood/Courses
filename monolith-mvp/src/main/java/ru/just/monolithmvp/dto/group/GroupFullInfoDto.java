package ru.just.monolithmvp.dto.group;

import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.dto.program.ProgramSummaryDto;
import ru.just.monolithmvp.dto.user.UserDto;

import java.util.List;

public record GroupFullInfoDto(
        GroupDto group,
        List<UserDto> users,
        List<CourseSummaryDto> courses,
        List<ProgramSummaryDto> programs
) {
}
