package ru.just.progressservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExtendedCourseProgressDto {
    private Long courseId;
    private Integer completedLessons;
    private Integer totalLessons;
    private Double progressPercentage;
    private Boolean completed;

    private List<ModuleProgressDto> moduleProgresses;
}

