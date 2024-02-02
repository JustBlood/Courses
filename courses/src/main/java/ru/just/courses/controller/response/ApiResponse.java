package ru.just.courses.controller.response;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Data
public class ApiResponse {
    private final String message;
    private final ZonedDateTime zonedDateTime = ZonedDateTime.now();
}
