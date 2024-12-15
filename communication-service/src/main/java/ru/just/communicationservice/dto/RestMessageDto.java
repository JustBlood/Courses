package ru.just.communicationservice.dto;

import lombok.Data;
import ru.just.communicationservice.model.Message;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class RestMessageDto {
    private UUID chatId;
    private Long senderId;
    private Map<String, Object> content;
    private Message.MessageType messageType;
    private LocalDateTime sentAt;

    public RestMessageDto(Message message, Map<String, Object> content) {
        this.chatId = message.getChatId();
        this.senderId = message.getSenderId();
        this.messageType = message.getMessageType();
        this.sentAt = message.getSentAt();
        this.content = content;
    }
}
