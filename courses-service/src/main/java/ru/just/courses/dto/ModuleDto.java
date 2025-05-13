package ru.just.courses.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.Module;
import ru.just.courses.model.course.Course;
import ru.just.dtolib.base.Dto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class ModuleDto extends Dto<Module> {
    private Long id;
    private String title;
    private String description;
    private Integer ordinalNumber;
    private Long courseId;
    private List<ThemeDto> themes;

    @Override
    public ModuleDto fromEntity(Module entity) {
        id = entity.getId();
        title = entity.getTitle();
        description = entity.getDescription();
        courseId = entity.getCourse().getId();
        ordinalNumber = entity.getOrdinalNumber();
        themes = entity.getThemes() == null
                ? new ArrayList<>()
                : entity.getThemes().stream().map(th -> new ThemeDto().fromEntity(th)).collect(Collectors.toList());
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
