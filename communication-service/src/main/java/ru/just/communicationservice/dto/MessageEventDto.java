package ru.just.communicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@AllArgsConstructor
public class MessageEventDto<T extends MessageEventDto.MessageBody> {
    private T body;
    private MessageType type;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        ERROR
    }

    @Data
    public static abstract class MessageBody {
        public abstract MessageType getType();
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    @AllArgsConstructor
    public static class ChatMessageBody extends MessageBody {
        private String message;

        @Override
        public MessageType getType() {
            return MessageType.CHAT;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    @AllArgsConstructor
    public static class JoinMessageBody extends MessageBody {
        private Long userId;
        private String username;
        private String photoUrl;
        private String profileUrl;

        @Override
        public MessageType getType() {
            return MessageType.JOIN;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    @AllArgsConstructor
    public static class LeaveMessageBody extends MessageBody {
        private Long userId;

        @Override
        public MessageType getType() {
            return MessageType.LEAVE;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    @AllArgsConstructor
    public static class ErrorMessageBody extends MessageBody {
        private String message;

        @Override
        public MessageType getType() {
            return MessageType.ERROR;
        }
    }
}
