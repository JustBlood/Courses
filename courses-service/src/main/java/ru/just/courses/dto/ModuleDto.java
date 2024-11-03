package ru.just.courses.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.Module;
import ru.just.courses.model.course.Course;
import ru.just.dtolib.base.Dto;

@Getter
@Setter
@NoArgsConstructor
public class ModuleDto extends Dto<Module> {
    private Long id;
    private String title;
    private String description;
    private Integer ordinalNumber;
    private Long courseId;

    @Override
    public ModuleDto fromEntity(Module entity) {
        id = entity.getId();
        title = entity.getTitle();
        description = entity.getDescription();
        courseId = entity.getCourse().getId();
        ordinalNumber = entity.getOrdinalNumber();
        return this;
    }

    @Override
    public Module toEntity() {
        return new Module()
                .withId(id)
                .withTitle(title)
                .withDescription(description)
                .withOrdinalNumber(ordinalNumber)
                .withCourse(new Course().withId(courseId));
    }
}
