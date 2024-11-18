package ru.just.progressservice.mapper;

import org.springframework.stereotype.Component;
import ru.just.progressservice.dto.LessonProgressDto;
import ru.just.progressservice.model.user.UserLessonProgress;

@Component
public class LessonProgressMapper {
    public LessonProgressDto toDto(UserLessonProgress progress) {
        return new LessonProgressDto(
                progress.getLessonId(),
                progress.getCompleted(),
                progress.getCompletedAt()
        );
    }
}

