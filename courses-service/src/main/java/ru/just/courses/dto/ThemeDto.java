package ru.just.courses.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.dto.lesson.ShortLessonDto;
import ru.just.courses.model.Module;
import ru.just.courses.model.theme.Theme;
import ru.just.dtolib.base.Dto;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ThemeDto extends Dto<Theme> {
    private Long id;
    private String title;
    private String description;
    private Long moduleId;
    private Integer ordinalNumber;
    private List<ShortLessonDto> lessons;

    @Override
    public ThemeDto fromEntity(Theme entity) {
        id = entity.getId();
        title = entity.getTitle();
        description = entity.getDescription();
        moduleId = entity.getModule().getId();
        ordinalNumber = entity.getOrdinalNumber();
        lessons = entity.getLessons().stream().map(lesson -> new ShortLessonDto().fromEntity(lesson)).toList();
        return this;
    }

    @Override
    public Theme toEntity() {
        return new Theme()
                .withId(id)
                .withTitle(title)
                .withDescription(description)
                .withModule(new Module().withId(moduleId))
                .withOrdinalNumber(ordinalNumber);
    }
}
