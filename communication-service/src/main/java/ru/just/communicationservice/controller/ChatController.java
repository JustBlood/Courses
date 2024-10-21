package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.communicationservice.dto.ChatDto;
import ru.just.communicationservice.dto.integration.UserDto;
import ru.just.communicationservice.service.ChatService;

import java.util.List;
import java.util.UUID;

@CrossOrigin
@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController { // TODO: добавить логику работы с webSocket

    private final ChatService chatService;

    // Создать чат
    @PostMapping
    public ResponseEntity<ChatDto> createChat() {
        return ResponseEntity.ok(chatService.createChat());
    }

    @PostMapping("/{chatId}/invite")
    public ResponseEntity<Void> addUserToChat(@PathVariable UUID chatId,
                                              @RequestParam("userId") Long userId
    ) {
        chatService.addUserToChat(chatId, userId);
        return ResponseEntity.ok().build();
    }

    // Получить чаты пользователя
    @GetMapping("/user")
    public ResponseEntity<List<ChatDto>> getUserChats(Pageable pageable) {
        return ResponseEntity.ok(chatService.getUserChats(pageable));
    }

    @GetMapping("/{chatId}/users")
    public ResponseEntity<List<UserDto>> getChatUsers(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatUsers(chatId));
    }
}
