package ru.just.communicationservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.just.communicationservice.dto.ChatDto;
import ru.just.communicationservice.dto.integration.UserDto;
import ru.just.communicationservice.service.ChatService;

import java.util.UUID;

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
    @GetMapping
    public ResponseEntity<Page<ChatDto>> getUserChats(Pageable pageable) {
        return ResponseEntity.ok(chatService.getUserChats(pageable));
    }

    @GetMapping("/{chatId}/users")
    public ResponseEntity<Page<UserDto>> getChatUsers(@PathVariable UUID chatId) {
        return ResponseEntity.ok(chatService.getChatUsers(chatId));
    }

    @PostMapping("/{chatId}/attachments/upload")
    public ResponseEntity<Void> uploadAttachment(
            @PathVariable UUID chatId,
            @RequestParam("file") MultipartFile file
    ) {
        chatService.uploadAttachment(chatId, file);
        return ResponseEntity.ok().build();
    }
}
