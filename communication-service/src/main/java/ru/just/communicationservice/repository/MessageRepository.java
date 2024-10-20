package ru.just.communicationservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.just.communicationservice.model.Message;

import java.util.UUID;

public interface MessageRepository extends MongoRepository<Message, String> {
    // Загрузка последних 20 сообщений по chatId, отсортированных по времени
    Page<Message> findByChatIdOrderBySentAtDesc(UUID chatId, Pageable pageable);
}
