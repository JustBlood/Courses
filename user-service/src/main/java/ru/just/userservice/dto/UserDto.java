package ru.just.userservice.dto;

import lombok.*;

import java.time.LocalDate;

@With
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String mail;
    private String phone;
    private LocalDate registrationDate;
    private Boolean isAdmin;
}
