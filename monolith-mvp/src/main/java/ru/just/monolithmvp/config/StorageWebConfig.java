package ru.just.monolithmvp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

@Configuration
public class StorageWebConfig implements WebMvcConfigurer {

    @Value("${app.storage.root-dir:data}")
    private String rootDir;

    @Value("${app.storage.csp-frame-ancestors:}")
    private String cspFrameAncestors;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of(rootDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (cspFrameAncestors == null || cspFrameAncestors.isBlank()) {
            return;
        }

        String normalizedFrameAncestors = Arrays.stream(cspFrameAncestors.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(" "));

        if (normalizedFrameAncestors.isBlank()) {
            return;
        }

        final HandlerInterceptor xFrameOptionsInterceptor = buildAncestorsInterceptor(normalizedFrameAncestors);

        registry.addInterceptor(xFrameOptionsInterceptor).addPathPatterns("/files/**");
    }

    private static HandlerInterceptor buildAncestorsInterceptor(String normalizedFrameAncestors) {
        String contentSecurityPolicyValue = "frame-ancestors " + normalizedFrameAncestors;

        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response,
                                     Object handler) {
                response.setHeader("Content-Security-Policy", contentSecurityPolicyValue);
                return true;
            }
        };
    }
}
