package ru.just.securityservice.dto;

import lombok.Getter;

import java.util.UUID;

@Getter
public class DeviceIdPayload {
    private UUID deviceId;
}
