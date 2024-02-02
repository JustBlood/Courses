package ru.just.courses.controller.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@AllArgsConstructor
@Data
@Builder
public class ApiError {
    private OffsetDateTime dateOccurred;
    private String message;
}
