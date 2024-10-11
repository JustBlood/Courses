package ru.just.personalaccountservice.dto;

import lombok.Data;

@Data
public class UpdateUserDto {
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
}
