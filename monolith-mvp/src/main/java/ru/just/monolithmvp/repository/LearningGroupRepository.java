package ru.just.monolithmvp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.just.monolithmvp.model.GroupType;
import ru.just.monolithmvp.model.LearningGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningGroupRepository extends JpaRepository<LearningGroup, UUID> {

    Optional<LearningGroup> findByTitleAndType(String title, GroupType type);
    List<LearningGroup> findAllByOrderByTitleAsc();
    List<LearningGroup> findByTitleContainingIgnoreCaseOrderByTitleAsc(String title);
}
