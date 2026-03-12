package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.monolithmvp.dto.stat.CourseStudentStatDto;
import ru.just.monolithmvp.dto.stat.StudentCourseStatDto;
import ru.just.monolithmvp.security.SecurityUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsService {
    private final StatisticsReportService statisticsReportService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<StudentCourseStatDto> myCourseStats() {
        return statisticsReportService.userCourseStats(securityUtils.currentUserId());
    }

    @Transactional(readOnly = true)
    public List<StudentCourseStatDto> userCourseStats(Long userId) {
        return statisticsReportService.userCourseStats(userId);
    }

    @Transactional(readOnly = true)
    public List<CourseStudentStatDto> courseStats(Long courseId) {
        return statisticsReportService.courseStats(courseId);
    }

    @Transactional(readOnly = true)
    public String summaryReportCsv() {
        return statisticsReportService.summaryReportCsv();
    }

    @Transactional(readOnly = true)
    public String summaryReportCsv(Long courseId) {
        return statisticsReportService.summaryReportCsv(courseId);
    }
}
