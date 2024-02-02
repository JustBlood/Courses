package ru.just.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.Module;
import ru.just.courses.model.course.Course;

@Getter
@Setter
@NoArgsConstructor
public class CreateModuleDto extends Dto<Module> {
    @NotBlank(message = "title should not be blank")
    private String title;
    @NotNull(message = "description should not be null")
    private String description;
    @NotNull(message = "course id should be specified")
    private Long courseId;

    @Override
    public CreateModuleDto fromEntity(Module entity) {
        title = entity.getTitle();
        description = entity.getDescription();
        courseId = entity.getCourse().getId();
        return this;
    }

    @Override
    public Module toEntity() {
        return new Module()
                .withTitle(title)
                .withDescription(description)
                .withCourse(new Course().withId(courseId));
    }
}
