package ru.just.progressservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThemeProgressDto {
    private Long themeId;
    private Boolean completed;
}

