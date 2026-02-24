package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.GroupMembership;
import ru.just.monolithmvp.model.GroupType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    boolean existsByGroupIdAndUserId(UUID groupId, Long userId);
    boolean existsByUserIdAndGroupId(Long userId, UUID groupId);
    List<GroupMembership> findByGroupId(UUID groupId);
    List<GroupMembership> findByGroupIdAndUserIdIn(UUID groupId, List<Long> userIds);
    void deleteByGroupIdAndUserId(UUID groupId, Long userId);
    void deleteByGroupId(UUID groupId);
    List<GroupMembership> findByUserId(Long userId);
    List<GroupMembership> findByUserIdIn(List<Long> userIds);
    List<GroupMembership> findByUserIdInAndGroup_Type(List<Long> userIds, GroupType type);
    Optional<GroupMembership> findByUserIdAndGroup_Type(Long userId, GroupType type);
    long countByUserIdAndGroup_Type(Long userId, GroupType type);
}
