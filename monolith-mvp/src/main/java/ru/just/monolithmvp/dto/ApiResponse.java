package ru.just.monolithmvp.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Стандартный ответ API с сообщением")
public record ApiResponse(String message) {
}
