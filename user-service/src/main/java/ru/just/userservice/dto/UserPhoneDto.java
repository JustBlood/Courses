package ru.just.userservice.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class UserPhoneDto {
    @Pattern(regexp = "(^8|7|\\+7)((\\d{10})|(\\s\\(\\d{3}\\)\\s\\d{3}\\s\\d{2}\\s\\d{2}))",
            message = "specify valid phone number")
    private String phone;
}
