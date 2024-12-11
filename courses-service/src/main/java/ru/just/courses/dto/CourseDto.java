package ru.just.courses.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.course.Course;
import ru.just.dtolib.base.Dto;

import java.time.ZonedDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseDto extends Dto<Course> {
    private Long id;
    private String title;
    private String description;
    private Integer completionTimeInHours;
    private ZonedDateTime createdAt;
    private Long authorId;

    @Override
    public CourseDto fromEntity(Course entity) {
        id = entity.getId();
        authorId = entity.getAuthorId();
        title = entity.getTitle();
        description = entity.getDescription();
        completionTimeInHours = entity.getCompletionTimeInHours();
        createdAt = entity.getCreatedAt();
        return this;
    }

    @Override
    public Course toEntity() {
        return new Course()
                .withId(id)
                .withTitle(title)
                .withDescription(description)
                .withCompletionTimeInHours(completionTimeInHours)
                .withCreatedAt(createdAt)
                .withAuthorId(authorId);
    }

}