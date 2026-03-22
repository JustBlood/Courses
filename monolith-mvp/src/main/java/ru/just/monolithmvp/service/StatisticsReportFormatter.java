package ru.just.monolithmvp.service;

import org.springframework.stereotype.Component;
import ru.just.monolithmvp.model.CourseProgress;
import ru.just.monolithmvp.model.CourseProgressStatus;
import ru.just.monolithmvp.model.GroupMembership;
import ru.just.monolithmvp.model.GroupType;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class StatisticsReportFormatter {

    public String fmt(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public String safe(String value) {
        return value == null ? "" : value;
    }

    public String enrollmentStatus(CourseProgress progress) {
        if (progress != null && progress.getStatus() == CourseProgressStatus.COMPLETED) {
            return "Завершен";
        }
        if (progress != null && (progress.getStartedAt() != null || progress.getStatus() == CourseProgressStatus.IN_PROGRESS)) {
            return "В процессе";
        }
        return "Назначен";
    }

    public boolean isCourseCompleted(CourseProgress progress) {
        return progress != null && progress.getStatus() == CourseProgressStatus.COMPLETED;
    }

    public LocalDateTime startedAt(CourseProgress progress) {
        return progress == null ? null : progress.getStartedAt();
    }

    public LocalDateTime completedAt(CourseProgress progress) {
        return progress == null ? null : progress.getCompletedAt();
    }

    public String groupTitleByType(List<GroupMembership> memberships, GroupType type) {
        return memberships.stream()
                .filter(membership -> membership.getGroup().getType() == type)
                .map(membership -> membership.getGroup().getTitle())
                .findFirst()
                .orElse("");
    }

    public String calcDeadline(LocalDateTime enrolledAt, Integer deadlineDays) {
        if (enrolledAt == null || deadlineDays == null) {
            return "";
        }
        return fmt(enrolledAt.plusDays(deadlineDays));
    }

    public String formatSpentTime(LocalDateTime startedAt, LocalDateTime completedAt) {
        if (startedAt == null || completedAt == null || completedAt.isBefore(startedAt)) {
            return "";
        }

        Duration duration = Duration.between(startedAt, completedAt);
        long totalMinutes = duration.toMinutes();
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return String.format("%dд %02d:%02d", days, hours, minutes);
        }
        return String.format("%02d:%02d", hours, minutes);
    }

    public String datePart(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    public String timePart(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("HH.mm.ss"));
    }

    public String extractLogin(String email) {
        if (email == null || email.isBlank()) {
            return "";
        }
        int at = email.indexOf('@');
        return at > 0 ? email.substring(0, at) : email;
    }

    public int roundToInt(double value) {
        return (int) Math.round(value);
    }

    public String formatEfficiency(Integer efficiencyPercent) {
        return String.format(Locale.US, "%.2f", (double) efficiencyPercent);
    }

    public String formatProgressPercent(double progressPercent) {
        return String.format(Locale.US, "%.2f%%", progressPercent);
    }
}
