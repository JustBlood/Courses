package ru.just.mediaservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfiguration {
    private final MinioProperties minioProperties;

    @Bean
    public MinioClient minioClientDev() throws Exception {
        MinioClient client = MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
        log.info("Creating minio client");
        for (var bucket : minioProperties.getBucketValues()) {
            log.info("Client correctly connected");
            if (client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) continue;
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        return client;
    }
}
