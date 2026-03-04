package ru.just.monolithmvp.dto.student;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = false)
public record UpdateMyProfileRequest(
        @Size(max = 255) String fullName,
        @Size(max = 255) String phone,
        @Size(max = 2000) String comment
) {
}