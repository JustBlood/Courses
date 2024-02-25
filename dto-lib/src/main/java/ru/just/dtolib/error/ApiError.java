package ru.just.dtolib.error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@AllArgsConstructor
@Data
@Builder
public class ApiError {
    private LocalDateTime occured;
    private String message;
}
