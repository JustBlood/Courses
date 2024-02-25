package ru.just.securityservice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@NoArgsConstructor
@Getter
public class LoginDto {
    private UUID deviceId;
}
