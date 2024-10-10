package ru.just.mentorcatalogservice.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class Mentor {
    // id записи
    private Long id;
    // id пользователя из user-service
    private Long userId;
    private String shortAboutMe;
    private String longAboutMe;
    private String avatarUrl;
    private Set<Long> studentsIds;
    private Set<Specialization> specializations;

    public interface Column {
        String ID = "id";
        String USER_ID = "user_id";
        String SHORT_ABOUT_ME = "short_about_me";
        String LONG_ABOUT_ME = "long_about_me";
        String AVATAR_URL = "avatar_url";
        String STUDENT_ID = "student_id";
        String SPECIALIZATION_ID = "specialization_id";
        String SPECIALIZATION_NAME = "specialization_name";
    }
}
