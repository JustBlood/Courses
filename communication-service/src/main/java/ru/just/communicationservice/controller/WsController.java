package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import ru.just.communicationservice.dto.MessageDto;
import ru.just.communicationservice.dto.MessageEventDto;
import ru.just.communicationservice.dto.MessageNotificationDto;
import ru.just.communicationservice.model.Message;
import ru.just.communicationservice.service.ChatService;
import ru.just.communicationservice.service.MessageService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WsController {
    private final MessageService messageService;
    private final ChatService chatService;
    @Lazy
    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    @MessageMapping("/{chatId}/chat.sendMessage")
    @SendTo("/topic/chat/{chatId}")
    public MessageEventDto<MessageEventDto.ChatMessageBody> sendMessage(
            @Payload MessageDto messageDto,
            @DestinationVariable UUID chatId,
            SimpMessageHeaderAccessor headerAccessor
    ) {
        log.info("Получено сообщение из чата {}", chatId);
        final Map<String, Object> sessionAttributes = Objects.requireNonNull(
                headerAccessor.getSessionAttributes());

        Long userId = Long.parseLong(sessionAttributes.get("userId").toString());
        Message.SimpleMessageBody body = new Message.SimpleMessageBody(messageDto.getContent());
        messageService.saveMessage(chatId, userId, body);

        sendNotificationsToChatMembers(messageDto, chatId, userId);

        final MessageEventDto.ChatMessageBody chatMessageBody = new MessageEventDto
                .ChatMessageBody(userId, messageDto.getContent());
        return new MessageEventDto<>(chatMessageBody, chatMessageBody.getType());
    }

    @MessageMapping("/topic/user/{userId}")
    @SendTo("/topic/user/{userId}")
    public MessageNotificationDto process(@Payload MessageNotificationDto messageNotificationDto,
                                          @DestinationVariable Long userId) {
        return messageNotificationDto;
    }

    private void sendNotificationsToChatMembers(MessageDto messageDto, UUID chatId, Long userId) {
        int messageLength = Math.min(messageDto.getContent().length(), 50);
        chatService.getChatUsers(chatId).stream()
                .filter(u -> !Objects.equals(u.getId(), userId))
                .forEach(u -> messagingTemplate.convertAndSend(
                        "/topic/user/" + u.getId(),
                        new MessageNotificationDto(chatId, userId, messageDto.getContent().substring(0, messageLength))
                ));
    }
}
