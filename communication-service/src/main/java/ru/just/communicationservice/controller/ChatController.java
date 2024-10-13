package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.communicationservice.dto.ChatDto;
import ru.just.communicationservice.dto.CreateChatDto;
import ru.just.communicationservice.service.ChatService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
@RequiredArgsConstructor
public class ChatController { // TODO: добавить логику работы с webSocket

    private final ChatService chatService;

    // Создать чат
    @PostMapping
    public ResponseEntity<ChatDto> createChat(@RequestBody CreateChatDto createChatDto) {
        return ResponseEntity.ok(chatService.createChat(createChatDto));
    }

    // Получить чаты пользователя
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ChatDto>> getUserChats(@PathVariable String userId) {
        return ResponseEntity.ok(chatService.getUserChats(userId));
    }

    // Получить чат по ID
    @GetMapping("/{chatId}")
    public ResponseEntity<ChatDto> getChat(@PathVariable String chatId) {
        return ResponseEntity.ok(chatService.getChat(chatId));
    }
}
