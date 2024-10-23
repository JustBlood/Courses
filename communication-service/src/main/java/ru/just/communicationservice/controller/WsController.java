package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import ru.just.communicationservice.dto.MessageDto;
import ru.just.communicationservice.dto.MessageEventDto;
import ru.just.communicationservice.model.Message;
import ru.just.communicationservice.service.MessageService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WsController {
    private final MessageService messageService;

    @MessageMapping("/{chatId}/chat.sendMessage")
    @SendTo("/topic/chat/{chatId}")
    public MessageEventDto<MessageEventDto.ChatMessageBody> sendMessage(
            @Payload MessageDto messageDto,
            @DestinationVariable UUID chatId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        log.info("Пришло в контроллер");
        final Map<String, Object> sessionAttributes = Objects.requireNonNull(headerAccessor.getSessionAttributes());
        log.info("Атрибуты сессии {}", sessionAttributes);

        log.info("Берется userId");
        Long userId = Long.parseLong(sessionAttributes.get("userId").toString());
        Message.SimpleMessageBody body = new Message.SimpleMessageBody(messageDto.getContent());
        messageService.saveMessage(chatId, userId, body);
        final MessageEventDto.ChatMessageBody chatMessageBody = new MessageEventDto.ChatMessageBody(userId, messageDto.getContent());
        return new MessageEventDto<>(chatMessageBody, chatMessageBody.getType());
    }
}
