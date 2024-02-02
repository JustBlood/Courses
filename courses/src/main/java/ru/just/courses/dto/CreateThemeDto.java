package ru.just.courses.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.dto.validation.EnumValue;
import ru.just.courses.model.Module;
import ru.just.courses.model.theme.ContentType;
import ru.just.courses.model.theme.Theme;

@Getter
@Setter
@NoArgsConstructor
public class CreateThemeDto extends Dto<Theme> {
    @NotBlank(message = "title should not be blank")
    private String title;
    @NotNull(message = "description should not be null")
    private String description;
    @NotNull(message = "module should be specified")
    private Long moduleId;
    @NotNull(message = "theme order in module should be specified")
    private Integer themeOrder;
    @NotNull(message = "content type should be specified")
    @EnumValue(enumClass = ContentType.class, message = "incorrect content type. You can use: TEXT, VIDEO, AUDIO")
    private String contentType;

    @Override
    public CreateThemeDto fromEntity(Theme entity) {
        title = entity.getTitle();
        description = entity.getDescription();
        moduleId = entity.getModule().getId();
        themeOrder = entity.getThemeOrder();
        contentType = entity.getContentType().name();
        return this;
    }

    @Override
    public Theme toEntity() {
        return new Theme()
                .withTitle(title)
                .withDescription(description)
                .withModule(new Module().withId(moduleId))
                .withThemeOrder(themeOrder)
                .withContentType(ContentType.valueOf(contentType));
    }
}
