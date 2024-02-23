package ru.just.securityservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.just.dtolib.kafka.users.UserDeliverStatus;

import java.util.HashSet;
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
    private Long userId;
    @Column(nullable = false, unique = true)
    private String login;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    public String email;
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserDeliverStatus userDeliverStatus;

    @ManyToMany(mappedBy = "users")
    private Set<Role> roles = new HashSet<>();
}
