package ru.just.communicationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.just.communicationservice.dto.ChatDto;
import ru.just.communicationservice.model.Chat;
import ru.just.communicationservice.repository.ChatRepository;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final ThreadLocalTokenService tokenService;

    public ChatDto createChat() {
        Chat chat = new Chat();
        chat.setId(UUID.randomUUID());
        log.info("Создается чат с пользователем: {}", tokenService.getUserId());
        chat.setMemberIds(List.of(tokenService.getUserId()));
        chat = chatRepository.save(chat);

        final ChatDto chatDto = new ChatDto();
        chatDto.setChatId(chat.getId());
        chatDto.setMembers(chat.getMemberIds());
        return chatDto;
    }

    public List<ChatDto> getUserChats(Pageable pageable) {
        //todo: получить чаты пользователя
        Long userId = tokenService.getUserId();
        return chatRepository.findAllByMemberIdsContains(userId, pageable)
                .map(chat -> {
                    final ChatDto chatDto = new ChatDto();
                    chatDto.setChatId(chat.getId());
                    chatDto.setMembers(chat.getMemberIds());
                    return chatDto;
                }).toList();
    }

    public boolean isUserInChat(Long userId, UUID chatId) {
        return chatRepository.existsByIdAndMemberIdsContains(chatId, userId);
    }

    public void removeUserFromChat(Long userId, UUID chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(); // todo: exception
        chat.getMemberIds().remove(userId);
        chatRepository.save(chat);
    }

    public void addUserToChat(UUID chatId, Long invitingUserId) {
        if (!isUserInChat(tokenService.getUserId(), chatId)) {
            throw new IllegalStateException("Current user not in specified chat");
        }
        Chat chat = chatRepository.findById(chatId).orElseThrow(); // todo: exception
        chat.getMemberIds().add(invitingUserId);
        chatRepository.save(chat);
    }
}
