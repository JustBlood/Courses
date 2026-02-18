package ru.just.monolithmvp.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ru.just.monolithmvp.dto.group.CreateGroupRequest;
import ru.just.monolithmvp.dto.group.GroupDto;
import ru.just.monolithmvp.dto.group.GroupUsersDto;
import ru.just.monolithmvp.exception.BadRequestException;
import ru.just.monolithmvp.exception.NotFoundException;
import ru.just.monolithmvp.mapper.UserMapper;
import ru.just.monolithmvp.model.AppUser;
import ru.just.monolithmvp.model.GroupMembership;
import ru.just.monolithmvp.model.GroupType;
import ru.just.monolithmvp.model.LearningGroup;
import ru.just.monolithmvp.repository.AppUserRepository;
import ru.just.monolithmvp.repository.GroupMembershipRepository;
import ru.just.monolithmvp.repository.LearningGroupRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final LearningGroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final AppUserRepository userRepository;
    private final UserMapper userMapper;

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
            }
            return;
        }

        List<GroupMembership> typedMemberships = membershipRepository
                .findByUserIdInAndGroup_Type(uniqueUserIds, targetType);

        List<GroupMembership> membershipsToDelete = new ArrayList<>();
        Map<Long, Boolean> hasTargetGroupMembership = new HashMap<>();
        for (Long userId : uniqueUserIds) {
            hasTargetGroupMembership.put(userId, false);
        }

        for (GroupMembership membership : typedMemberships) {
            Long userId = membership.getUser().getId();
            if (membership.getGroup().getId().equals(groupId)) {
                hasTargetGroupMembership.put(userId, true);
                continue;
            }
            membershipsToDelete.add(membership);
        }

        if (!membershipsToDelete.isEmpty()) {
            membershipRepository.deleteAll(membershipsToDelete);
        }

        membershipsToCreate = users.stream()
                .filter(user -> !Boolean.TRUE.equals(hasTargetGroupMembership.get(user.getId())))
                .map(user -> {
                    GroupMembership membership = new GroupMembership();
                    membership.setGroup(group);
                    membership.setUser(user);
                    return membership;
                }).toList();

        if (!membershipsToCreate.isEmpty()) {
            membershipRepository.saveAll(membershipsToCreate);
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
}
