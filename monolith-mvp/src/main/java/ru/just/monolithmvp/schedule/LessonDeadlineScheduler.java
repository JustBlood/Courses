package ru.just.monolithmvp.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.just.monolithmvp.model.LessonSubmission;
import ru.just.monolithmvp.model.SubmissionStatus;
import ru.just.monolithmvp.repository.LessonSubmissionRepository;
import ru.just.monolithmvp.service.CourseProgressService;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonDeadlineScheduler {
    private final LessonSubmissionRepository lessonSubmissionRepository;
    private final CourseProgressService courseProgressService;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    public void checkLessonDeadline() {
        final List<LessonSubmission> submissionsInProgress = lessonSubmissionRepository
                .findAllByStatusIn(List.of(SubmissionStatus.STARTED, SubmissionStatus.REWORKING));
        submissionsInProgress.stream()
                .filter(s -> s.getLesson().getLessonType().isPractice())
                .filter(s -> s.getStartedAt() != null && s.getLesson().getTimeLimitMinutes() != null)
                .filter(s -> LocalDateTime.now(Clock.systemUTC()).isAfter(s.getStartedAt().plusMinutes(s.getLesson().getTimeLimitMinutes())))
                .forEach(expiredSubmission -> {
                    boolean isLastAttempt = false;
                    expiredSubmission.setAttemptCounter(Optional.ofNullable(expiredSubmission.getAttemptCounter()).orElse(0) + 1);
                    if (expiredSubmission.getLesson().getAttemptLimit() != null) {
                        isLastAttempt = expiredSubmission.getAttemptCounter() >= expiredSubmission.getLesson().getAttemptLimit();
                    }
                    expiredSubmission.setStatus(isLastAttempt ? SubmissionStatus.INCOMPLETED : SubmissionStatus.REWORKING);
                    expiredSubmission.setSubmittedAt(LocalDateTime.now(Clock.systemUTC()));
                    expiredSubmission.setStartedAt(null);
                    log.info("Нашлось зачисление на урок, у которого истек дедлайн. Урок: {}", expiredSubmission);
                    lessonSubmissionRepository.saveAndFlush(expiredSubmission);
                    courseProgressService.recalcCourseProgressByUserInNewTransaction(expiredSubmission.getStudent().getId(), expiredSubmission.getLesson().getCourse().getId());
                });
    }
}
