package ru.just.progressservice.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.just.progressservice.dto.*;
import ru.just.progressservice.mapper.CourseProgressMapper;
import ru.just.progressservice.mapper.LessonProgressMapper;
import ru.just.progressservice.mapper.TaskDtoConverter;
import ru.just.progressservice.model.user.UserCourseProgress;
import ru.just.progressservice.model.user.UserLessonProgress;
import ru.just.progressservice.model.user.UserModuleProgress;
import ru.just.progressservice.model.user.UserThemeProgress;
import ru.just.progressservice.repository.UserCourseProgressRepository;
import ru.just.progressservice.repository.UserLessonProgressRepository;
import ru.just.progressservice.repository.UserModuleProgressRepository;
import ru.just.progressservice.repository.UserThemeProgressRepository;
import ru.just.progressservice.service.integration.CourseServiceClient;
import ru.just.progressservice.service.integration.TaskCheckerServiceClient;
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
    private final TransactionTemplate transactionTemplate;

    private final CourseServiceClient courseServiceClient;
    private final TaskCheckerServiceClient taskCheckerServiceClient;

    private final TaskDtoConverter taskDtoConverter;
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
                .orElseThrow(() -> new EntityNotFoundException("No visited lessons"));

        return courseServiceClient.getLessonById(lastLesson.getLessonId()); // Пример интеграции с CourseService
    }


    // Завершение урока
    public TaskResult completeLesson(Long lessonId, UserAnswerDto answerDto) {
        // проверка задания в task-checker
        var lessonDto = courseServiceClient.getLessonById(lessonId);
        var taskDto = taskDtoConverter.convertToTaskDto(lessonDto, answerDto);

        TaskResult taskResult = taskCheckerServiceClient.solveLesson(taskDto);

        transactionTemplate.executeWithoutResult(transactionStatus -> {
            UserLessonProgress lessonProgress = lessonProgressRepository.findByLessonIdAndUserId(lessonId, tokenService.getUserId())
                    .orElseThrow(() -> new EntityNotFoundException("Lesson progress not found"));
            if (!lessonProgress.getCompleted() && taskResult.isCorrect()) {
                lessonProgress.setCompleted(true);
                lessonProgress.setCompletedAt(ZonedDateTime.now());
                lessonProgressRepository.save(lessonProgress);

                // Обновляем прогресс иерархии
                updateProgressHierarchy(lessonProgress);
            }
        });
        return taskResult;
    }

    // Получение следующего урока
    public LessonProgressDto getNextLesson(Long courseId) {
        UserCourseProgress courseProgress = courseProgressRepository.findByUserIdAndCourseId(tokenService.getUserId(), courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course progress not found"));

        return lessonProgressRepository.findNextLesson(courseProgress.getId())
                .map(lessonProgressMapper::toDto)
                .orElseThrow(() -> new EntityNotFoundException("No lessons available"));
    }

    // TODO: заменить на 1 запрос / как-то оптимизировать
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
        // Проверяем существование курса через Feign-клиент к CourseService
        // todo: в 1 запрос эту херню бы
        // todo: проверить, что текущий юзер = ментор и владелец курса
        if (!tokenService.getUserId().equals(courseServiceClient.getCourseById(courseId).getAuthorId())) {
            throw new IllegalStateException("Текущий пользователь - не создатель курса");
        }
        List<ModuleDto> moduleDtos = courseServiceClient.getModules(courseId);
        List<ThemeDto> themeDtos = courseServiceClient.getThemes(courseId);

        // Проверяем, не записан ли пользователь уже на курс
        if (courseProgressRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new IllegalStateException("User is already assigned to this course");
        }

        // Создаем новую запись о прогрессе курса
        UserCourseProgress courseProgress = new UserCourseProgress();
        courseProgress.setUserId(userId);
        courseProgress.setCourseId(courseId);
        courseProgress.setTotalLessons(themeDtos.stream().map(t -> t.getLessons().size()).reduce(Integer::sum).orElseThrow());
        courseProgress.setCompletedLessons(0);
        courseProgress.setCreatedAt(ZonedDateTime.now());

        courseProgressRepository.save(courseProgress);

        // Создаем прогресс модулей и тем
        moduleDtos.forEach(module -> {
            UserModuleProgress moduleProgress = new UserModuleProgress();
            moduleProgress.setCourseProgress(courseProgress);
            moduleProgress.setModuleId(module.getId());
            moduleProgress.setTotalThemes(themeDtos.size());
            moduleProgress.setCompletedThemes(0);
            moduleProgress.setUserId(userId);
            moduleProgress.setCompleted(false);
            moduleProgress.setOrdinalNumber(module.getOrdinalNumber());

            moduleProgressRepository.save(moduleProgress);

            themeDtos.forEach(theme -> {
                UserThemeProgress themeProgress = new UserThemeProgress();
                themeProgress.setModuleProgress(moduleProgress);
                themeProgress.setThemeId(theme.getId());
                themeProgress.setTotalLessons(theme.getLessons().size());
                themeProgress.setCompletedLessons(0);
                themeProgress.setUserId(userId);
                themeProgress.setOrdinalNumber(theme.getOrdinalNumber());
                themeProgress.setCompleted(false);

                themeProgressRepository.save(themeProgress);

                theme.getLessons().forEach(lesson -> {
                    UserLessonProgress lessonProgress = new UserLessonProgress();
                    lessonProgress.setThemeProgress(themeProgress);
                    lessonProgress.setLessonId(lesson.getLessonId());
                    lessonProgress.setCompleted(false);
                    lessonProgress.setUserId(userId);
                    lessonProgress.setCompleted(false);

                    lessonProgressRepository.save(lessonProgress);
                });
            });
        });
    }
}


