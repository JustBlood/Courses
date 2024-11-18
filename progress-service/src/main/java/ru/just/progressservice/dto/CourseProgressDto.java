package ru.just.progressservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CourseProgressDto {
    private Long courseId;
    private Integer completedLessons;
    private Integer totalLessons;
    private Double progressPercentage;
    private Boolean completed;
}

