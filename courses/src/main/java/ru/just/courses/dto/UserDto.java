package ru.just.courses.dto;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.user.User;

import java.time.LocalDate;

@NoArgsConstructor
@Getter
@Setter
public class UserDto extends Dto<User> {
    private Long id;
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String mail;
    private String phone;
    private LocalDate registrationDate;
    private Boolean isAdmin;

    @Override
    public UserDto fromEntity(User entity) {
        id = entity.getId();
        username = entity.getUsername();
        password = entity.getPassword();
        firstName = entity.getFirstName();
        lastName = entity.getLastName();
        mail = entity.getMail();
        phone = entity.getPhone();
        registrationDate = entity.getRegistrationDate();
        isAdmin = entity.getIsAdmin();
        return this;
    }

    @Override
    public User toEntity() {
        return new User()
                .withId(id)
                .withUsername(username)
                .withPassword(password)
                .withFirstName(firstName)
                .withLastName(lastName)
                .withMail(mail)
                .withPhone(phone)
                .withRegistrationDate(registrationDate)
                .withIsAdmin(isAdmin);
    }
}
