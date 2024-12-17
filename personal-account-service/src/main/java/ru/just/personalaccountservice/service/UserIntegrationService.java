package ru.just.personalaccountservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.just.dtolib.kafka.users.UpdateUserAction;
import ru.just.dtolib.response.ApiResponse;
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

    @Value("http://${service-discovery.users-service.name}/api/v1/users/internal")
    private String usersServiceUri;
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
        final String uriTemplate = usersServiceUri + "/" + tokenService.getUserId();
        final RequestEntity<Void> requestEntity = RequestEntity.get(uriTemplate)
                .headers(buildHeaders())
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
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    public ApiResponse addAvatar(Long userId, MultipartFile file) {
        final String uriTemplate = usersServiceUri + "/" + userId + "/photo";
        LinkedMultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
        parts.add("file", file.getResource());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<LinkedMultiValueMap<String, Object>> httpEntity = new HttpEntity<>(parts, httpHeaders);

        try {
            return restTemplate.postForEntity(uriTemplate, httpEntity, ApiResponse.class).getBody();
        } catch (RestClientException e) {
            log.error("Error while requesting user service get users info by ids", e);
            throw new IllegalStateException();
        }
    }
}
