package ru.just.courses.controller.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
public class WebAppConfig implements WebMvcConfigurer {
    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        WebMvcConfigurer.super.configureMessageConverters(converters);
        converters.add(responseBodyConverter());
    }

    @Bean
    public HttpMessageConverter<InputStream> responseBodyConverter() {
        AbstractHttpMessageConverter<InputStream> converter = new AbstractHttpMessageConverter<>() {
            @Override
            protected boolean supports(Class<?> clazz) {
                return InputStream.class == clazz;
            }

            @Override
            protected InputStream readInternal(Class<? extends InputStream> clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
                return inputMessage.getBody();
            }

            @Override
            protected void writeInternal(InputStream inputStream, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
                byte[] buffer = new byte[1024];
                try (BufferedInputStream br = new BufferedInputStream(inputStream)) {
                    int realSize;
                    while ((realSize = br.read(buffer)) != -1) {
                        outputMessage.getBody().write(buffer, 0, realSize);
                    }
                    outputMessage.getBody().flush();
                }
            }
        };
        converter.setSupportedMediaTypes(List.of(new MediaType("text", "plain", StandardCharsets.UTF_8)));
        return converter;
    }
}
