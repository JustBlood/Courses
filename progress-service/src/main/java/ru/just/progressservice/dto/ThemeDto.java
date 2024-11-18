package ru.just.progressservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ThemeDto {
    private Long id;
    private String title;
    private String description;
    private Long moduleId;
    private Integer ordinalNumber;
    private List<ShortLessonDto> lessons;
}
