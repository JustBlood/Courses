package ru.just.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.course.Course;
import ru.just.courses.model.user.User;
import ru.just.dtolib.base.Dto;

import java.time.ZonedDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class CreateCourseDto extends Dto<Course> {
    @NotBlank(message = "title shouldn't be blank")
    private String title;
    @NotBlank(message = "description shouldn't be blank")
    private String description;
    private ZonedDateTime createdAt = ZonedDateTime.now();
    @NotNull(message = "course completion time must be specified")
    private Integer completionTime;
    @NotNull(message = "author id must be specified")
    private Long authorId;

    @Override
    public CreateCourseDto fromEntity(Course entity) {
        title = entity.getTitle();
        description = entity.getDescription();
        createdAt = entity.getCreatedAt();
        completionTime = entity.getCompletionTimeInHours();
        authorId = entity.getAuthor().getId();
        return this;
    }

    @Override
    public Course toEntity() {
        return new Course()
                .withTitle(title)
                .withDescription(description)
                .withCreatedAt(createdAt)
                .withCompletionTimeInHours(completionTime)
                .withAuthor(new User().withId(authorId));
    }
}
