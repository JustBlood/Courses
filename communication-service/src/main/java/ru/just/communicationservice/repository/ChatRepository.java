package ru.just.communicationservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.just.communicationservice.model.Chat;

import java.util.UUID;

public interface ChatRepository extends MongoRepository<Chat, UUID> {
    Page<Chat> findAllByMemberIdsContains(Long memberId, Pageable pageable);
    Boolean existsByIdAndMemberIdsContains(UUID chatId, Long userId);
}
