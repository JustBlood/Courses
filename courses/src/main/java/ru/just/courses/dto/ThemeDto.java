package ru.just.courses.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.Module;
import ru.just.courses.model.theme.ContentType;
import ru.just.courses.model.theme.Theme;
import ru.just.dtolib.base.Dto;

@Getter
@Setter
@NoArgsConstructor
public class ThemeDto extends Dto<Theme> {
    private Long id;
    private String title;
    private String description;
    private Long moduleId;
    private Integer themeOrder;
    private ContentType contentType;

    @Override
    public ThemeDto fromEntity(Theme entity) {
        id = entity.getId();
        title = entity.getTitle();
        description = entity.getDescription();
        moduleId = entity.getModule().getId();
        themeOrder = entity.getThemeOrder();
        contentType = entity.getContentType();
        return this;
    }

    @Override
    public Theme toEntity() {
        return new Theme()
                .withId(id)
                .withTitle(title)
                .withDescription(description)
                .withModule(new Module().withId(moduleId))
                .withThemeOrder(themeOrder)
                .withContentType(contentType);
    }
}
