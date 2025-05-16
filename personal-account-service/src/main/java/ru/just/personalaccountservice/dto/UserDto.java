package ru.just.personalaccountservice.dto;

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
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String photoUrl;
    private LocalDate registrationDate;
    private UserStatus userStatus;
}
