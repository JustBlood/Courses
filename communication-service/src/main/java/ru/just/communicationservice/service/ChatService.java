package ru.just.communicationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.just.communicationservice.dto.ChatDto;
import ru.just.communicationservice.dto.MessageEventDto;
import ru.just.communicationservice.dto.integration.UserDto;
import ru.just.communicationservice.model.Chat;
import ru.just.communicationservice.model.Message;
import ru.just.communicationservice.repository.ChatRepository;
import ru.just.communicationservice.service.integration.MediaIntegrationService;
import ru.just.communicationservice.service.integration.UserIntegrationService;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageService messageService;
    private final ThreadLocalTokenService tokenService;
    private final UserIntegrationService userIntegrationService;
    private final MediaIntegrationService mediaIntegrationService;
    @Lazy
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

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

    public List<UserDto> getChatUsers(UUID chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Чата не существует"));
        List<Long> userIds = chat.getMemberIds();
        return userIntegrationService.getUsersData(userIds);
    }

    public List<ChatDto> getUserChats(Pageable pageable) {
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
        Chat chat = chatRepository.findById(chatId).orElseThrow();
        chat.getMemberIds().remove(userId);
        final MessageEventDto.LeaveMessageBody body = new MessageEventDto.LeaveMessageBody(userId);
        messagingTemplate.convertAndSend("/topic/chat/" + chat, new MessageEventDto<>(body, body.getType()));
        chatRepository.save(chat);
    }

    public void addUserToChat(UUID chatId, Long invitingUserId) {
        if (!isUserInChat(tokenService.getUserId(), chatId)) {
            throw new IllegalStateException("Current user not in specified chat");
        }
        Optional<UserDto> userDto = userIntegrationService.getUserData(tokenService.getDecodedToken().getToken());
        if (userDto.isEmpty()) {
            throw new IllegalArgumentException("Пользователя не существует");
        }
        Chat chat = chatRepository.findById(chatId).orElseThrow(); // todo: exception
        chat.getMemberIds().add(invitingUserId);
        log.info("Запрос в users_service с токеном {}", tokenService.getDecodedToken().getToken());

        chatRepository.save(chat);

        final MessageEventDto.JoinMessageBody body = new MessageEventDto.JoinMessageBody(userDto.get().getId());
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, new MessageEventDto<>(body, body.getType()));
    }

    public void uploadAttachment(UUID chatId, MultipartFile file) {
        String fullPathToAttachment = mediaIntegrationService.saveChatAttachmentPhoto(chatId, file);
        final Long userId = tokenService.getUserId();
        String presignedUrl = mediaIntegrationService.getPresignedUrlForAttachment(chatId, fullPathToAttachment);

        final Message.AttachmentMessageBody messageBody = new Message.AttachmentMessageBody(presignedUrl, fullPathToAttachment);
        messageService.saveMessage(chatId, tokenService.getUserId(), messageBody);

        MessageEventDto.AttachmentMessageBody body = new MessageEventDto.AttachmentMessageBody(userId, presignedUrl);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, new MessageEventDto<>(body, body.getType()));
    }
}
