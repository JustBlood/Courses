package ru.just.mentorcatalogservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Mentor {
    // id записи
    private Long id;
    // id пользователя из user-service
    private Long userId;
    private String shortAboutMe;
    private String longAboutMe;
    private List<Long> studentsIds;
    private List<String> specializations;

    public interface Column {
        String ID = "id";
        String USER_ID = "user_id";
        String SHORT_ABOUT_ME = "short_about_me";
        String LONG_ABOUT_ME = "long_about_me";
        String STUDENT_ID = "student_id";
        String SPECIALIZATION_NAME = "specialization_name";
        String SPECIALIZATIONS = "specializations";
        String STUDENTS_COUNT = "students_count";
    }
}
