package ru.just.monolithmvp.service;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.just.monolithmvp.dto.course.CourseSummaryDto;
import ru.just.monolithmvp.mapper.CourseMapper;
import ru.just.monolithmvp.model.GroupCourseAssignment;
import ru.just.monolithmvp.model.GroupProgramAssignment;
import ru.just.monolithmvp.model.LearningProgram;
import ru.just.monolithmvp.repository.CourseRepository;
import ru.just.monolithmvp.repository.GroupCourseAssignmentRepository;
import ru.just.monolithmvp.repository.GroupProgramAssignmentRepository;
import ru.just.monolithmvp.repository.LearningProgramRepository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupAssignmentService {

    private final CourseRepository courseRepository;
    private final LearningProgramRepository programRepository;
    private final GroupCourseAssignmentRepository groupCourseAssignmentRepository;
    private final CourseAssignmentService courseAssignmentService;
    private final ProgramService programService;
    private final GroupProgramAssignmentRepository groupProgramAssignmentRepository;
    private final CourseMapper courseMapper;

    @Transactional
    public List<CourseSummaryDto> findCoursesNotAssignedToGroup(UUID groupId) {
        return courseRepository.findAllNotAssignedToGroup(groupId).stream()
                .map(courseMapper::toSummaryDto).toList();
    }

    public List<GroupCourseAssignment> findCourseAssignmentsByGroupId(UUID groupId) {
        return groupCourseAssignmentRepository.findByGroupId(groupId);
    }

    public List<GroupProgramAssignment> findProgramAssignmentsByGroupId(UUID groupId) {
        return groupProgramAssignmentRepository.findByGroupId(groupId);
    }

    @Transactional
    public void assignCoursesToGroup(UUID groupId, @NotEmpty List<Long> courseIds) {
        courseIds.forEach(courseId -> courseAssignmentService.assignGroupToCourse(courseId, groupId));
    }

    @Transactional
    public void unassignCoursesFromGroup(UUID groupId, @NotEmpty List<Long> courseIds) {
        courseIds.forEach(courseId -> courseAssignmentService.unassignGroupFromCourse(courseId, groupId));
    }

    public List<LearningProgram> findProgramsNotAssignedToGroup(UUID groupId) {
        return programRepository.findAllNotAssignedToGroup(groupId);
    }

    @Transactional
    public void assignProgramsToGroup(UUID groupId, @NotEmpty List<Long> programIds) {
        programIds.forEach(programId -> programService.assignGroups(programId, Set.of(groupId), Set.of()));
    }

    public void unassignProgramsFromGroup(UUID groupId, @NotEmpty List<Long> programIds) {
        programIds.forEach(programId -> programService.assignGroups(programId, Set.of(), Set.of(groupId)));
    }
}
