package ru.just.progressservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ModuleProgressDto {
    private Long moduleId;
    private Boolean completed;

    private List<ThemeProgressDto> themeProgresses;
}

