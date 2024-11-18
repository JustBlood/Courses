package ru.just.progressservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.progressservice.dto.*;
import ru.just.progressservice.mapper.CourseProgressMapper;
import ru.just.progressservice.mapper.LessonProgressMapper;
import ru.just.progressservice.model.user.UserCourseProgress;
import ru.just.progressservice.model.user.UserLessonProgress;
import ru.just.progressservice.model.user.UserModuleProgress;
import ru.just.progressservice.model.user.UserThemeProgress;
import ru.just.progressservice.repository.UserCourseProgressRepository;
import ru.just.progressservice.repository.UserLessonProgressRepository;
import ru.just.progressservice.repository.UserModuleProgressRepository;
import ru.just.progressservice.repository.UserThemeProgressRepository;
import ru.just.progressservice.service.integration.CourseServiceClient;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgressService {
    private final UserCourseProgressRepository courseProgressRepository;
    private final UserModuleProgressRepository moduleProgressRepository;
    private final UserThemeProgressRepository themeProgressRepository;
    private final UserLessonProgressRepository lessonProgressRepository;

    private final CourseServiceClient courseServiceClient;
    private final CourseProgressMapper courseProgressMapper;
    private final LessonProgressMapper lessonProgressMapper;
    private final ThreadLocalTokenService tokenService;

    // Получение всех курсов пользователя
    public List<CourseProgressDto> getUserCourses() {
        return courseProgressRepository.findByUserId(tokenService.getUserId())
                .stream()
                .map(courseProgressMapper::toDto)
                .toList();
    }

    public ExtendedCourseProgressDto getExtendedCourseProgress(Long courseId) {
        UserCourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(tokenService.getUserId(), courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course progress not found"));

        List<ModuleProgressDto> moduleProgresses = moduleProgressRepository.findByCourseProgressId(courseProgress.getId())
                .stream()
                .map(this::mapModuleProgress)
                .toList();

        return new ExtendedCourseProgressDto(
                courseProgress.getCourseId(),
                courseProgress.getCompletedLessons(),
                courseProgress.getTotalLessons(),
                calculatePercentage(courseProgress.getCompletedLessons(), courseProgress.getTotalLessons()),
                courseProgress.getCompletedAt() != null,
                moduleProgresses
        );
    }

    private Double calculatePercentage(Integer completed, Integer total) {
        return total == 0 ? 0.0 : (completed / (double) total) * 100.0;
    }

    private ModuleProgressDto mapModuleProgress(UserModuleProgress moduleProgress) {
        List<ThemeProgressDto> themeProgresses = themeProgressRepository.findByModuleProgressId(moduleProgress.getId())
                .stream()
                .map(theme -> new ThemeProgressDto(
                        theme.getThemeId(),
                        theme.getCompleted()
                ))
                .toList();

        return new ModuleProgressDto(
                moduleProgress.getModuleId(),
                moduleProgress.getCompleted(),
                themeProgresses
        );
    }

    public LessonDto getLastVisitedLesson(Long courseId) {
        UserLessonProgress lastLesson = lessonProgressRepository.findLastVisitedLesson(tokenService.getUserId(), courseId)
                .orElseThrow(() -> new EntityNotFoundException("No lessons visited"));

        return courseServiceClient.getLessonById(lastLesson.getLessonId()); // Пример интеграции с CourseService
    }


    // Завершение урока
    public void completeLesson(Long lessonId) {
        UserLessonProgress lessonProgress = lessonProgressRepository.findByLessonIdAndUserId(lessonId, tokenService.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Lesson progress not found"));

        if (!lessonProgress.getCompleted()) {
            lessonProgress.setCompleted(true);
            lessonProgress.setCompletedAt(ZonedDateTime.now());
            lessonProgressRepository.save(lessonProgress);

            // Обновляем прогресс иерархии
            updateProgressHierarchy(lessonProgress);
        }
    }

    // Получение следующего урока
    public LessonProgressDto getNextLesson(Long courseId) {
        UserCourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(tokenService.getUserId(), courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course progress not found"));

        return lessonProgressRepository.findNextLesson(courseProgress.getId())
                .map(lessonProgressMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("No lessons available"));
    }

    private void updateProgressHierarchy(UserLessonProgress lessonProgress) {
        // Обновляем прогресс темы
        UserThemeProgress themeProgress = lessonProgress.getThemeProgress();
        themeProgress.setCompletedLessons(themeProgress.getCompletedLessons() + 1);
        if (themeProgress.getCompletedLessons().equals(themeProgress.getTotalLessons())) {
            themeProgress.setCompleted(true);
        }
        themeProgressRepository.save(themeProgress);

        // Обновляем прогресс модуля
        UserModuleProgress moduleProgress = themeProgress.getModuleProgress();
        if (themeProgress.getCompleted()) {
            moduleProgress.setCompletedThemes(moduleProgress.getCompletedThemes() + 1);
        }
        if (moduleProgress.getCompletedThemes().equals(moduleProgress.getTotalThemes())) {
            moduleProgress.setCompleted(true);
        }
        moduleProgressRepository.save(moduleProgress);

        // Обновляем прогресс курса
        UserCourseProgress courseProgress = moduleProgress.getCourseProgress();
        if (lessonProgress.getCompleted()) {
            courseProgress.setCompletedLessons(courseProgress.getCompletedLessons() + 1);
        }
        if (courseProgress.getCompletedLessons().equals(courseProgress.getTotalLessons())) {
            courseProgress.setCompletedAt(ZonedDateTime.now());
        }
        courseProgressRepository.save(courseProgress);
    }

    public void assignUser(Long courseId, Long userId) {
        // TODO
    }
}


