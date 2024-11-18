package ru.just.progressservice.mapper;

import org.springframework.stereotype.Component;
import ru.just.progressservice.dto.CourseProgressDto;
import ru.just.progressservice.model.user.UserCourseProgress;

@Component
public class CourseProgressMapper {
    public CourseProgressDto toDto(UserCourseProgress progress) {
        return new CourseProgressDto(
                progress.getCourseId(),
                progress.getCompletedLessons(),
                progress.getTotalLessons(),
                calculatePercentage(progress.getCompletedLessons(), progress.getTotalLessons()),
                progress.getCompletedAt() != null
        );
    }

    private Double calculatePercentage(Integer completed, Integer total) {
        return total == 0 ? 0.0 : (completed / (double) total) * 100.0;
    }
}

