package ru.just.communicationservice.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import ru.just.communicationservice.controller.interceptor.SubscribeChannelInterceptor;
import ru.just.communicationservice.controller.interceptor.WebSocketAuthInterceptor;
import ru.just.communicationservice.service.ChatService;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final ChatService chatService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Включаем простой брокер сообщений для пересылки сообщений пользователям
        config.enableSimpleBroker("/topic");
        // Префикс для сообщений, отправляемых с клиента
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Регистрируем конечную точку WebSocket
        registry.addEndpoint("/ws/chat")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns("http://127.0.0.1:5502")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration){
        registration.interceptors(new SubscribeChannelInterceptor(chatService));
    }
}
