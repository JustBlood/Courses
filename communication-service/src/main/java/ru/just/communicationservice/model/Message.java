package ru.just.communicationservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private UUID chatId;        // Идентификатор чата (может быть как личный чат, так и групповой)
    private Long senderId;      // ID отправителя
    private String content;       // Текст сообщения
    private LocalDateTime sentAt; // Время отправки сообщения
}
