package ru.just.userservice.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean("protobufRestTemplate")
    @LoadBalanced
    public RestTemplate restTemplate(List<RestTemplateCustomizer> customizers) {
        return new RestTemplateBuilder()
                .customizers(customizers)
                .messageConverters(new ProtobufHttpMessageConverter())
                .build();
    }
}
