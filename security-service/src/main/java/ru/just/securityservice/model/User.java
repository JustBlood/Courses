package ru.just.securityservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.just.dtolib.kafka.users.UserDeliverStatus;

import java.util.Set;

@Accessors(chain = true)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq")
    @SequenceGenerator(name = "user_id_seq", sequenceName = "user_id_seq", allocationSize = 1)
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false, unique = true)
    private String login;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    public String email;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserDeliverStatus deliverStatus;

    @ManyToMany
    @JoinTable(name = "user_role",
            joinColumns =
                @JoinColumn(name = "user_id", referencedColumnName = "user_id"),
            inverseJoinColumns =
                @JoinColumn(name = "role_id", referencedColumnName = "role_id"))
    private Set<Role> roles;
}
