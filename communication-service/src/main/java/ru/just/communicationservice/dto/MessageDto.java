package ru.just.communicationservice.dto;

import lombok.Data;

@Data
public class MessageDto {
    private String content;
    private MessageType type;

    public enum MessageType {
        CHAT,
        JOIN,
        LEAVE,
        ERROR
    }
}
