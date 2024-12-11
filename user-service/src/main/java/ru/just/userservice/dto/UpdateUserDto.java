package ru.just.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class UpdateUserDto {
    @NotBlank(message = "username mustn't be empty")
    private String username;
    @NotBlank(message = "firstName mustn't be empty")
    private String firstName;
    @NotBlank(message = "lastName mustn't be empty")
    private String lastName;
    @Pattern(regexp = "(^8|7|\\+7)((\\d{10})|(\\s\\(\\d{3}\\)\\s\\d{3}\\s\\d{2}\\s\\d{2}))", message = "specify valid phone number")
    private String phone;
}