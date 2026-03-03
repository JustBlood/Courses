package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.dto.group.CreateGroupRequest;
import ru.just.monolithmvp.dto.group.GroupDto;
import ru.just.monolithmvp.dto.group.GroupUsersDto;
import ru.just.monolithmvp.dto.group.UpdateGroupRequest;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.*;
import ru.just.monolithmvp.repository.*;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final LearningGroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final GroupCourseAssignmentRepository groupCourseAssignmentRepository;
    private final AppUserRepository userRepository;
    private final UserMapper userMapper;
    private final CourseService courseService;

    @Transactional
    public GroupDto createGroup(CreateGroupRequest request) {
        groupRepository.findByTitleAndType(request.title(), request.type()).ifPresent(g -> {
            throw new BadRequestException("Group already exists for this title and type");
        });
        LearningGroup group = new LearningGroup();
        group.setTitle(request.title());
        group.setType(request.type());
        group = groupRepository.save(group);
        return new GroupDto(group.getId(), group.getTitle(), group.getType());
    }

    @Transactional
    public GroupDto updateGroup(UUID groupId, UpdateGroupRequest request) {
        LearningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        groupRepository.findByTitleAndType(request.title(), request.type())
                .filter(found -> !found.getId().equals(groupId))
                .ifPresent(found -> {
                    throw new BadRequestException("Group already exists for this title and type");
                });

        GroupType oldType = group.getType();
        GroupType newType = request.type();

        if (oldType != newType && newType != GroupType.GENERAL) {
            List<Long> memberUserIds = membershipRepository.findByGroupId(groupId).stream()
                    .map(membership -> membership.getUser().getId())
                    .distinct()
                    .toList();

            if (!memberUserIds.isEmpty()) {
                List<Long> conflictingUserIds = membershipRepository
                        .findByUserIdInAndGroup_Type(memberUserIds, newType)
                        .stream()
                        .filter(membership -> !membership.getGroup().getId().equals(groupId))
                        .map(membership -> membership.getUser().getId())
                        .distinct()
                        .toList();

                if (!conflictingUserIds.isEmpty()) {
                    throw new BadRequestException("Users already belong to another " + newType + " group: " + conflictingUserIds);
                }
            }
        }

        group.setTitle(request.title());
        group.setType(newType);
        group = groupRepository.save(group);

        return new GroupDto(group.getId(), group.getTitle(), group.getType());
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getGroups() {
        return groupRepository.findAll().stream()
                .map(g -> new GroupDto(g.getId(), g.getTitle(), g.getType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GroupDto> getUserGroups(Long userId) {
        return membershipRepository.findByUserId(userId).stream()
                .map(GroupMembership::getGroup)
                .distinct()
                .map(g -> new GroupDto(g.getId(), g.getTitle(), g.getType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupUsersDto getGroupUsers(UUID groupId) {
        LearningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        return toGroupUsersDto(group);
    }

    @Transactional(readOnly = true)
    public List<GroupUsersDto> getGroupUsersByTitle(String title) {
        List<LearningGroup> groups;
        if (title == null || title.isBlank()) {
            groups = groupRepository.findAllByOrderByTitleAsc();
        } else {
            groups = groupRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc(title.trim());
        }

        return groups.stream().map(this::toGroupUsersDto).toList();
    }

    @Transactional(readOnly = true)
    public GroupUsersDto getGroupUsersForStudent(UUID groupId, Long studentId) {
        LearningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        if (!membershipRepository.existsByUserIdAndGroupId(studentId, groupId)) {
            throw new AccessDeniedException("Access forbidden for this group");
        }

        return toGroupUsersDto(group);
    }

    @Transactional(readOnly = true)
    public List<GroupUsersDto> getMyGroupUsersByTitle(Long studentId, String title) {
        String normalizedTitle = title == null ? "" : title.trim().toLowerCase();

        return membershipRepository.findByUserId(studentId).stream()
                .map(GroupMembership::getGroup)
                .distinct()
                .filter(group -> normalizedTitle.isBlank()
                        || group.getTitle().toLowerCase().contains(normalizedTitle))
                .sorted(Comparator.comparing(LearningGroup::getTitle, String.CASE_INSENSITIVE_ORDER))
                .map(this::toGroupUsersDto)
                .toList();
    }

    @Transactional
    public void addUsersToGroup(UUID groupId, List<Long> userIds) {
        LearningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        if (CollectionUtils.isEmpty(userIds)) {
            throw new BadRequestException("User ids must not be empty");
        }
        if (userIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("User ids must not contain null values");
        }

        List<Long> uniqueUserIds = new ArrayList<>(new LinkedHashSet<>(userIds));
        List<AppUser> users = userRepository.findAllById(uniqueUserIds);

        Set<Long> foundUserIds = new HashSet<>();
        users.forEach(user -> foundUserIds.add(user.getId()));

        List<Long> missingUserIds = uniqueUserIds.stream()
                .filter(userId -> !foundUserIds.contains(userId))
                .toList();
        if (!missingUserIds.isEmpty()) {
            throw new NotFoundException("Users not found: " + missingUserIds);
        }

        GroupType targetType = group.getType();

        List<GroupMembership> membershipsToCreate = new ArrayList<>();

        if (targetType == GroupType.GENERAL) {
            Set<Long> alreadyInGroupUserIds = membershipRepository
                    .findByGroupIdAndUserIdIn(groupId, uniqueUserIds)
                    .stream()
                    .map(m -> m.getUser().getId())
                    .collect(java.util.stream.Collectors.toSet());

            for (AppUser user : users) {
                if (alreadyInGroupUserIds.contains(user.getId())) {
                    continue;
                }
                GroupMembership membership = new GroupMembership();
                membership.setGroup(group);
                membership.setUser(user);
                membershipsToCreate.add(membership);
            }

            if (!membershipsToCreate.isEmpty()) {
                membershipRepository.saveAll(membershipsToCreate);
                applyAssignmentsToNewMembers(groupId, membershipsToCreate);
            }
            return;
        }

        List<GroupMembership> typedMemberships = membershipRepository
                .findByUserIdInAndGroup_Type(uniqueUserIds, targetType);

        List<Long> conflictingUserIds = typedMemberships.stream()
                .filter(membership -> !membership.getGroup().getId().equals(groupId))
                .map(membership -> membership.getUser().getId())
                .distinct()
                .toList();

        if (!conflictingUserIds.isEmpty()) {
            throw new BadRequestException("Users already belong to another " + targetType + " group: " + conflictingUserIds);
        }

        Set<Long> alreadyInTargetUserIds = typedMemberships.stream()
                .filter(membership -> membership.getGroup().getId().equals(groupId))
                .map(membership -> membership.getUser().getId())
                .collect(java.util.stream.Collectors.toSet());

        membershipsToCreate = users.stream()
                .filter(user -> !alreadyInTargetUserIds.contains(user.getId()))
                .map(user -> {
                    GroupMembership membership = new GroupMembership();
                    membership.setGroup(group);
                    membership.setUser(user);
                    return membership;
                }).toList();

        if (!membershipsToCreate.isEmpty()) {
            membershipRepository.saveAll(membershipsToCreate);
            applyAssignmentsToNewMembers(groupId, membershipsToCreate);
        }
    }

    @Transactional
    public void removeUsersFromGroup(UUID groupId, List<Long> userIds) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));

        if (CollectionUtils.isEmpty(userIds)) {
            throw new BadRequestException("User ids must not be empty");
        }
        if (userIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("User ids must not contain null values");
        }

        userIds.forEach(userId -> membershipRepository.deleteByGroupIdAndUserId(groupId, userId));
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        LearningGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found: " + groupId));
        membershipRepository.deleteByGroupId(groupId);
        groupRepository.delete(group);
    }

    private GroupUsersDto toGroupUsersDto(LearningGroup group) {
        List<ru.just.monolithmvp.dto.user.UserDto> users = membershipRepository.findByGroupId(group.getId()).stream()
                .map(GroupMembership::getUser)
                .distinct()
                .map(userMapper::toDto)
                .toList();

        return new GroupUsersDto(group.getId(), group.getTitle(), group.getType(), users);
    }

    private void applyAssignmentsToNewMembers(UUID groupId, List<GroupMembership> newMemberships) {
        List<Long> studentIds = newMemberships.stream()
                .map(GroupMembership::getUser)
                .filter(user -> user.getRole() == Role.STUDENT)
                .map(AppUser::getId)
                .distinct()
                .toList();

        if (studentIds.isEmpty()) {
            return;
        }

        List<Long> courseIds = groupCourseAssignmentRepository.findByGroupId(groupId).stream()
                .map(assignment -> assignment.getCourse().getId())
                .distinct()
                .toList();

        for (Long courseId : courseIds) {
            for (Long studentId : studentIds) {
                courseService.enrollStudentToCourse(courseId, studentId);
            }
        }
    }
}
