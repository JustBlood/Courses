package ru.just.monolithmvp.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.just.monolithmvp.dto.course.CourseDto;
import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.model.Course;

@Mapper(componentModel = "spring")
public interface CourseMapper {
    @Mapping(target = "sectionId", source = "section.id")
    @Mapping(target = "sectionTitle", source = "section.title")
    @Mapping(target = "sectionPriority", source = "section.priority")
    CourseDto toDto(Course course);

    default CourseSummaryDto toSummaryDto(Course course) {
        return new CourseSummaryDto(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCoverFilePath(),
                course.getSection().getId(),
                course.getSection().getTitle(),
                course.getSection().getPriority(),
                course.getLessons().stream().filter(l -> l.getLessonType().isTheory()).count(),
                course.getLessons().stream().filter(l -> l.getLessonType().isPractice()).count()
        );
    }
}
