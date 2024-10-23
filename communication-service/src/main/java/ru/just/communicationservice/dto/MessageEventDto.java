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
        MESSAGE,
        ATTACHMENT,
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
        private Long userId;
        private String message;

        @Override
        public MessageType getType() {
            return MessageType.MESSAGE;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    @AllArgsConstructor
    public static class AttachmentMessageBody extends MessageBody {
        private Long userId;
        private String attachmentUrl;

        @Override
        public MessageType getType() {
            return MessageType.ATTACHMENT;
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Data
    @AllArgsConstructor
    public static class JoinMessageBody extends MessageBody {
        private Long userId;

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
