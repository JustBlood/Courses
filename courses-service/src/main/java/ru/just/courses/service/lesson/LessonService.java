package ru.just.courses.service.lesson;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.courses.dto.lesson.CreateLessonDto;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.mapper.LessonMapper;
import ru.just.courses.repository.lesson.LessonRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;

    public LessonDto createLesson(CreateLessonDto createLessonDto) {
        return lessonMapper.apply(lessonRepository.save(createLessonDto.getModel()));
    }

    public Optional<LessonDto> getLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId).map(lessonMapper);
    }
}
