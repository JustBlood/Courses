package ru.just.dtolib.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private String message;
    private ZonedDateTime zonedDateTime = ZonedDateTime.now();

    public ApiResponse(String message) {
        this.message = message;
    }
}
