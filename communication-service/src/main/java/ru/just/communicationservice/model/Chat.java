package ru.just.communicationservice.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.UUID;

@Data
@Document(collection = "chats")
public class Chat {
    @Id
    private UUID id;            // Идентификатор чата
    private Long authorId;
    private List<Long> memberIds;
}
