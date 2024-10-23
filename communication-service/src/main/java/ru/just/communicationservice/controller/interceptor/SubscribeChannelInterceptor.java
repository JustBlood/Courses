package ru.just.communicationservice.controller.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import ru.just.communicationservice.service.ChatService;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class SubscribeChannelInterceptor implements ChannelInterceptor {
    private final ChatService chatService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                && Objects.requireNonNull(accessor.getDestination()).startsWith("/topic/chat/")){
            log.info("subscribing message: {}",message);
            log.info("subscribing channel: {}", channel);
            log.info("subscribing accessor: {}", accessor);
            UUID chatId = UUID.fromString(accessor.getDestination().replace("/topic/chat/", ""));
            Long userId = Long.parseLong(((Map<String, String>) message.getHeaders().get("simpSessionAttributes")).get("userId"));
            if (!chatService.isUserInChat(userId, chatId)) {
                throw new AccessDeniedException("User not in chat or chat doesn't exists");
            }
        }
        return message;
    }
}
