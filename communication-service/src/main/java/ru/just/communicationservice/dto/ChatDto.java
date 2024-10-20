package ru.just.communicationservice.dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class ChatDto {
    private UUID chatId;
    private List<Long> members;
}
