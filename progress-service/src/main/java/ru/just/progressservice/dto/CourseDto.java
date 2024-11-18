package ru.just.progressservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseDto {
    private Long id;
    private String title;
    private String description;
    private Integer completionTimeInHours;
    private ZonedDateTime createdAt;
    private Long author_id;
}
