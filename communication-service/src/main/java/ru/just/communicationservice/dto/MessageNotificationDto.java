package ru.just.communicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class MessageNotificationDto {
    private UUID fromChatId;
    private Long fromUserId;
    private String smallContent;
}
