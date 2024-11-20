package ru.just.securityservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@OpenAPIDefinition
@Configuration
public class OpenAPIConfig {

    @Value("${api-gateway.host:localhost:8080}")
    private String gatewayHost;

    @Bean
    public OpenAPI customOpenAPI() {
        final String bearerScheme = "bearerAuth";
        final String basicScheme = "basic";
        return new OpenAPI()
                .servers(List.of(new Server().url("http://" + gatewayHost)))
                .info(new Info().title("Product Service API").version("1.0.0"))
                .addSecurityItem(new SecurityRequirement()
                        .addList(bearerScheme))
                .components(new Components()
                        .addSecuritySchemes(bearerScheme, new SecurityScheme()
                                .name(bearerScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSecuritySchemes(basicScheme, new SecurityScheme()
                                .name(basicScheme)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .in(SecurityScheme.In.HEADER)));
    }
}
