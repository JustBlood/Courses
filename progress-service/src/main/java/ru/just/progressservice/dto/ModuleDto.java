package ru.just.progressservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ModuleDto {
    private Long id;
    private String title;
    private String description;
    private Integer ordinalNumber;
    private Long courseId;
}
