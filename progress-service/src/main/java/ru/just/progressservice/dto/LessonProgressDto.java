package ru.just.progressservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LessonProgressDto {
    private Long lessonId;
    private Boolean completed;
    private ZonedDateTime completedAt;
}
