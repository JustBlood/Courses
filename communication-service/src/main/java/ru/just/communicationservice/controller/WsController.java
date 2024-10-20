package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import ru.just.communicationservice.dto.MessageDto;
import ru.just.communicationservice.dto.PageDto;
import ru.just.communicationservice.dto.integration.UserDto;
import ru.just.communicationservice.model.Message;
import ru.just.communicationservice.service.ChatService;
import ru.just.communicationservice.service.MessageService;
import ru.just.communicationservice.service.integration.UserIntegrationService;

import java.util.*;

@Slf4j
@CrossOrigin
@Controller
@RequiredArgsConstructor
public class WsController {
    private final MessageService messageService;
    private final ChatService chatService;
    private final UserIntegrationService userIntegrationService;

    @MessageMapping("/{chatId}/chat.sendMessage")
    @SendTo("/topic/chat/{chatId}")  // Сообщения в конкретную комнату (по chatId)
    public MessageDto sendMessage(@Payload MessageDto messageDto, @DestinationVariable UUID chatId, SimpMessageHeaderAccessor headerAccessor) {
        // todo: на стратегию
        log.info("Пришло в контроллер");
        final Map<String, Object> sessionAttributes = Objects.requireNonNull(headerAccessor.getSessionAttributes());
        log.info("Атрибуты сессии {}", sessionAttributes);
        if (MessageDto.MessageType.JOIN.equals(messageDto.getType())
                && sessionAttributes.keySet().containsAll(Set.of("userId", "username"))) {
            log.info("Оработка подключения №2 от того же пользователя");
            messageDto.setType(MessageDto.MessageType.ERROR);
            messageDto.setContent("Пользователь уже подключен к чату");
            return messageDto;
        }
        if (!sessionAttributes.keySet().containsAll(Set.of("userId", "username"))) {
            log.info("Запрос в users_service с токеном {}", headerAccessor.getSessionAttributes().get("jwt"));
            UserDto userDto = userIntegrationService.getUserData((String) headerAccessor.getSessionAttributes().get("jwt"))
                    .orElseThrow(() -> new IllegalStateException("user not found"));
            sessionAttributes.put("username", userDto.getUsername());
            sessionAttributes.put("userId", userDto.getId());
        }
        log.info("Берется userId");
        Long userId = (Long) sessionAttributes.get("userId");
        String username = (String) sessionAttributes.get("username");
        switch (messageDto.getType()) {
            case CHAT -> messageService.saveMessage(chatId, userId, messageDto);
            case JOIN -> {
                log.info("Присоединение к чату");
                if (!chatService.isUserInChat(userId, chatId)) {
                    throw new SecurityException("User not in specified chat");
                }
                messageDto.setContent("Пользователь " + username + " присоединился к чату");
            }
            case LEAVE -> {
                chatService.removeUserFromChat(userId, chatId);

                messageDto.setContent("Пользователь " + username + " вышел из чата");
            } default -> throw new IllegalArgumentException("Incorrect messageType");
        }
        log.info("Отправка сообщения в топик");
        return messageDto;  // Отправляем обратно всем пользователям в этой комнате
    }

    @MessageMapping("/chat.loadMessages")
    @SendTo("/topic/chat/{chatId}")
    public List<Message> loadMessages(@DestinationVariable UUID chatId, PageDto pageDto) {
        // Загружаем последние 20 сообщений
        log.info("Загружатся 20 последних сообщений");
        final Page<Message> messages = messageService.loadMessages(chatId, pageDto);
        return messages.toList();
    }
}
