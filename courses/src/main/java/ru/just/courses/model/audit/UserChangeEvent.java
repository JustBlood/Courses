package ru.just.courses.model.audit;

import jakarta.persistence.*;
import lombok.*;
import ru.just.courses.model.user.User;

import java.time.ZonedDateTime;

@With
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_user_change_type_change_time", columnList = "user_id ASC, changeType ASC, changeTime DESC")})
@Entity
public class UserChangeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @SequenceGenerator(name = "id_seq", sequenceName = "user_change_event_id_seq", allocationSize = 1)
    private Long id;
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    @Column(nullable = false)
    private ZonedDateTime changeTime = ZonedDateTime.now();
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
