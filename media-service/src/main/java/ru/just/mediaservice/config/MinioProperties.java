package ru.just.mediaservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint = "http://localhost:9000";
    private String accessKey;
    private String secretKey;

    private Map<String, String> buckets;

    public List<String> getBucketValues() {
        return new ArrayList<>(buckets.values());
    }
}
