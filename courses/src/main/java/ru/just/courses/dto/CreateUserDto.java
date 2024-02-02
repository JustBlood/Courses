package ru.just.courses.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import ru.just.courses.model.audit.UserChangeEvent;
import ru.just.courses.model.user.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
public class CreateUserDto extends Dto<User> {
    @NotBlank(message = "username mustn't be empty")
    private String username;
    @NotBlank(message = "username mustn't be empty") //todo: BCrypt password
    private String password;
    @NotBlank(message = "username mustn't be empty")
    private String firstName;
    @NotBlank(message = "username mustn't be empty")
    private String lastName;
    @Email(message = "email must be correct")
    @NotNull(message = "email must be specified")
    private String mail;
    @Pattern(regexp = "(^8|7|\\+7)((\\d{10})|(\\s\\(\\d{3}\\)\\s\\d{3}\\s\\d{2}\\s\\d{2}))", message = "specify valid phone number")
    private String phone;
    @JsonIgnore
    private List<UserChangeEvent> userChangeEvents = new ArrayList<>();
    @JsonIgnore
    private LocalDate registrationDate = LocalDate.now();

    @Override
    public CreateUserDto fromEntity(User entity) {
        username = entity.getUsername();
        password = entity.getPassword();
        firstName = entity.getFirstName();
        lastName = entity.getLastName();
        mail = entity.getMail();
        phone = entity.getPhone();
        userChangeEvents = entity.getUserChangeEvents();
        registrationDate = entity.getRegistrationDate();
        return this;
    }

    @Override
    public User toEntity() {
        return new User()
                .withUsername(username)
                .withPassword(password)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withMail(mail)
                .withPhone(phone)
                .withIsAdmin(false)
                .withUserChangeEvents(userChangeEvents)
                .withRegistrationDate(registrationDate);
    }
}
