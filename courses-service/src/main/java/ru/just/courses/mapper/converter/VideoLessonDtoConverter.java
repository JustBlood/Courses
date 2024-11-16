package ru.just.courses.mapper.converter;

import org.springframework.stereotype.Component;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.model.theme.lesson.LessonType;
import ru.just.courses.model.theme.lesson.VideoLesson;

@Component
public class VideoLessonDtoConverter implements LessonConverter<VideoLesson, LessonDto> {
    @Override
    public LessonDto apply(VideoLesson lesson) {
        return LessonDto.VideoLessonDto.builder()
                .lessonId(lesson.getLessonId())
                .videoUrl(lesson.getVideoUrl())
                .type(lesson.getType())
                .ordinalNumber(lesson.getOrdinalNumber())
                .build();
    }

    @Override
    public boolean canHandle(LessonType lessonType) {
        return LessonType.VIDEO.equals(lessonType);
    }
}
