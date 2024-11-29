package ru.just.communicationservice.service.integration;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import ru.just.communicationservice.dto.integration.UserDto;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserIntegrationService {
    private final RestTemplate restTemplate;
    private final ThreadLocalTokenService tokenService;

    @Value("http://${service-discovery.users-service.name}/api/v1/users")
    private String usersServiceUri;

    public Optional<UserDto> getUserData(String jwtToken) {
        final String uriTemplate = usersServiceUri + "/byId";
        final RequestEntity<Void> requestEntity = RequestEntity.get(uriTemplate)
                .headers(buildHeaders(jwtToken))
                .build();

        try {
            final UserDto userDto = restTemplate.exchange(requestEntity, UserDto.class).getBody();
            return Optional.ofNullable(userDto);
        } catch (RestClientException e) {
            log.error("Error while requesting user service get users info by ids", e);
            throw new IllegalStateException();
        }
    }

    private HttpHeaders buildHeaders(@Nullable String jwt) {
        HttpHeaders headers = new HttpHeaders();
        if (jwt != null) {
            headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + jwt);
        }
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return headers;
    }

    public List<UserDto> getUsersData(List<Long> userIds) {

        final String uriTemplate = usersServiceUri + "?ids=" + StringUtils.join(userIds, ",");
        final RequestEntity<Void> requestEntity = RequestEntity.get(uriTemplate)
                .headers(buildHeaders(null))
                .build();
        try {
            return restTemplate.exchange(requestEntity, new ParameterizedTypeReference<List<UserDto>>() {}).getBody();
        } catch (RestClientException e) {
            log.error("Error while requesting user service get users info by ids", e);
            throw new IllegalStateException();
        }
    }
}
