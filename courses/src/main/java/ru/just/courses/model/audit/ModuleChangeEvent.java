package ru.just.courses.model.audit;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.just.courses.model.Module;
import ru.just.courses.model.user.User;
import ru.just.dtolib.audit.ChangeType;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_module_change_type_change_time", columnList = "module_id ASC, changeType ASC, changeTime DESC")})
@Entity
public class ModuleChangeEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_seq")
    @SequenceGenerator(name = "id_seq", sequenceName = "module_change_event_id_seq", allocationSize = 1)
    private Long id;
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;
    @Column(nullable = false)
    private LocalDateTime changeTime = LocalDateTime.now();
    @ManyToOne
    @JoinColumn(name = "module_id")
    private Module module;
}
