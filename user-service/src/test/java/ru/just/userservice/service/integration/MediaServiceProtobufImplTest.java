package ru.just.userservice.service.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import ru.just.protos.mediaservice.avatargenerate.Username;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MediaServiceProtobufImplTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    MediaServiceProtobuf mediaServiceProtobuf;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void generateAvatar() {
        mediaServiceProtobuf.generateAvatar(Username.newBuilder().setName("test").build());
    }

    @Test
    void getPresignedUrlForAttachment() {
    }

    @Test
    void getPresignedUrlsByUserAvatars() {
    }
}
