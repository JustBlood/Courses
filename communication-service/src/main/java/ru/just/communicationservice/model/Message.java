package ru.just.communicationservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
    private MessageBody content;       // Текст сообщения
    private MessageType messageType = MessageType.SIMPLE_MESSAGE;
    private LocalDateTime sentAt; // Время отправки сообщения

    public enum MessageType {
        SIMPLE_MESSAGE,
        ATTACHMENT
    }

    public abstract static class MessageBody { public abstract MessageType getType(); }
    @EqualsAndHashCode(callSuper = true)
    @AllArgsConstructor
    @Data
    public static class SimpleMessageBody extends MessageBody {
        String message;

        @Override
        public MessageType getType() {
            return MessageType.SIMPLE_MESSAGE;
        }
    }
    @EqualsAndHashCode(callSuper = true)
    @AllArgsConstructor
    @Data
    public static class AttachmentMessageBody extends MessageBody {
        String presignedUrl;
        String fullPathToObject;

        @Override
        public MessageType getType() {
            return MessageType.ATTACHMENT;
        }
    }
}
