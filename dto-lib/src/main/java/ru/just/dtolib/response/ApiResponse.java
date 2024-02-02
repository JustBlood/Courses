package ru.just.dtolib.response;

import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class ApiResponse {
    private final String message;
    private final ZonedDateTime zonedDateTime = ZonedDateTime.now();
}
