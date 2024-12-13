package ru.just.communicationservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
import ru.just.dtolib.response.media.FileIdDto;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.lang.String.format;

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

    public Page<UserDto> getChatUsers(UUID chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow(() -> new IllegalArgumentException("Чата не существует"));
        List<Long> userIds = chat.getMemberIds();
        return new PageImpl<>(userIntegrationService.getUsersData(userIds));
    }

    public Page<ChatDto> getUserChats(Pageable pageable) {
        Long userId = tokenService.getUserId();
        return chatRepository.findAllByMemberIdsContains(userId, pageable)
                .map(chat -> {
                    final ChatDto chatDto = new ChatDto();
                    chatDto.setChatId(chat.getId());
                    chatDto.setMembers(chat.getMemberIds());
                    return chatDto;
                });
    }

    public boolean isUserInChat(Long userId, UUID chatId) {
        return chatRepository.existsByIdAndMemberIdsContains(chatId, userId);
    }

    public void removeUserFromChat(Long userId, UUID chatId) {
        Chat chat = chatRepository.findById(chatId).orElseThrow();
        if (!chat.getAuthorId().equals(tokenService.getUserId()) || chat.getAuthorId().equals(userId)) {
            throw new IllegalArgumentException(format("У вас нет прав на удаление пользователя %s из чата", userId));
        }
        chat.getMemberIds().remove(userId);
        final MessageEventDto.LeaveMessageBody body = new MessageEventDto.LeaveMessageBody(userId);
        messagingTemplate.convertAndSend("/topic/chat/" + chat, new MessageEventDto<>(body, body.getType()));
        chatRepository.save(chat);
    }

    public void addUserToChat(UUID chatId, Long invitingUserId) {
        if (!isUserInChat(tokenService.getUserId(), chatId)) {
            throw new IllegalStateException("Current user not in specified chat");
        }
        Optional<UserDto> userDto = userIntegrationService.getUserData(tokenService.getUserId());
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
        FileIdDto fileIdDto = mediaIntegrationService.saveChatAttachmentPhoto(file);
        String presignedUrl = mediaIntegrationService.getPresignedUrlForAttachment(fileIdDto.getFileId());

        final Message.AttachmentMessageBody messageBody = new Message.AttachmentMessageBody(presignedUrl,
                fileIdDto.getFileId());
        messageService.saveMessage(chatId, tokenService.getUserId(), messageBody);

        MessageEventDto.AttachmentMessageBody body = new MessageEventDto.AttachmentMessageBody(
                tokenService.getUserId(), presignedUrl);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, new MessageEventDto<>(body, body.getType()));
    }
}
