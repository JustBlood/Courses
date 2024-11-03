package ru.just.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.course.Course;
import ru.just.dtolib.base.Dto;

@Getter
@Setter
@NoArgsConstructor
public class UpdateCourseDto extends Dto<Course>  {
    @NotBlank(message = "title shouldn't be blank")
    private String title;
    @NotBlank(message = "description shouldn't be blank")
    private String description;
    @NotNull(message = "course completion time must be specified")
    private Integer completionTimeInHours;

    @Override
    public UpdateCourseDto fromEntity(Course entity) {
        title = entity.getTitle();
        description = entity.getDescription();
        completionTimeInHours = entity.getCompletionTimeInHours();
        return this;
    }

    @Override
    public Course toEntity() {
        return new Course()
                .withTitle(title)
                .withDescription(description)
                .withCompletionTimeInHours(completionTimeInHours);
    }
}
