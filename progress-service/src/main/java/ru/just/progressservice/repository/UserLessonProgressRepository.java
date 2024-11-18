package ru.just.progressservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.just.progressservice.model.user.UserLessonProgress;

import java.util.Optional;

public interface UserLessonProgressRepository extends JpaRepository<UserLessonProgress, Long> {
    Optional<UserLessonProgress> findByLessonIdAndUserId(Long lessonId, Long userId);

    @Query("SELECT lp FROM UserLessonProgress lp WHERE lp.themeProgress.moduleProgress.courseProgress.id = :courseProgressId AND lp.completed = false ORDER BY lp.themeProgress.moduleProgress.ordinalNumber, lp.themeProgress.ordinalNumber, lp.ordinalNumber ASC")
    Optional<UserLessonProgress> findNextLesson(@Param("courseProgressId") Long courseProgressId);

    @Query("SELECT lp FROM UserLessonProgress lp WHERE lp.themeProgress.moduleProgress.courseProgress.courseId = :courseId AND lp.userId = :userId")
    Optional<UserLessonProgress> findLastVisitedLesson(@Param("userId") Long userId, @Param("courseId") Long courseId);
}

