package ru.just.personalaccountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.just.dtolib.kafka.users.UpdateUserAction;
import ru.just.personalaccountservice.dto.UpdateUserDto;
import ru.just.personalaccountservice.dto.UserDto;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserIntegrationService {
    private final RestTemplate restTemplate;
    private final ThreadLocalTokenService tokenService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${service-discovery.users-service.name}")
    private String usersServiceName;
    @Value("${topics.user-update-topic}")
    private String userUpdateTopic;

    public void sendUpdateUserDataMessage(UpdateUserDto updateUserDto) {
        final UpdateUserAction updateUserAction = UpdateUserAction.builder()
                .id(tokenService.getUserId())
                .phone(updateUserDto.getPhone())
                .firstName(updateUserDto.getFirstName())
                .lastName(updateUserDto.getLastName())
                .build();
        // todo: DLT топик и обработка при ошибке
        kafkaTemplate.send(userUpdateTopic, updateUserAction);
    }

    public Optional<UserDto> getUserData() {
        final String uriTemplate = String.format("https://%s/api/v1/users/byId", usersServiceName);
        final HttpHeaders headers = buildHeaders();
        final RequestEntity<Void> requestEntity = RequestEntity.get(uriTemplate)
                .headers(headers)
                .build();

        try {
            final UserDto userDto = restTemplate.exchange(requestEntity, UserDto.class).getBody();
            return Optional.ofNullable(userDto);
        } catch (RestClientException e) {
            log.error("Error while requesting user service get users info by ids", e);
            throw new IllegalStateException();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, tokenService.getDecodedToken().getToken());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }
}
