package ru.just.monolithmvp.dto.course;

import ru.just.monolithmvp.dto.user.UserDto;

import java.util.List;

public record CourseEnrollmentListsDto(
        List<UserDto> enrolled,
        List<UserDto> notEnrolled
) {
}