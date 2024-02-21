package ru.just.securityservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

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
    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_user_id_seq")
    @SequenceGenerator(name = "user_user_id_seq", sequenceName = "user_user_id_seq", allocationSize = 1)
    private Long userId;
    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column
    public String email;

    @ManyToMany(mappedBy = "userPrincipal")
    private Set<Role> roles = new HashSet<>();
}