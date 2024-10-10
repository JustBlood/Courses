package ru.just.mentorcatalogservice.repository.mapper;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import ru.just.mentorcatalogservice.model.Mentor;
import ru.just.mentorcatalogservice.model.Specialization;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static ru.just.mentorcatalogservice.model.Mentor.Column.*;

@Component
public class MentorResultSetExtractor implements ResultSetExtractor<Page<Mentor>> {

    @Override
    public Page<Mentor> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<Long, Mentor> extractedMentors = new HashMap<>();

        while (rs.next()) {
            long mentorId = rs.getLong(ID);

            Mentor mentor = extractedMentors.get(mentorId);
            if (mentor == null) {
                mentor = Mentor.builder()
                        .id(mentorId)
                        .userId(rs.getLong(USER_ID))
                        .avatarUrl(rs.getString(AVATAR_URL))
                        .shortAboutMe(rs.getString(SHORT_ABOUT_ME))
                        .longAboutMe(rs.getString(LONG_ABOUT_ME))
                        .specializations(new HashSet<>())
                        .studentsIds(new HashSet<>())
                        .build();
                extractedMentors.put(mentorId, mentor);
            }

            mentor.getSpecializations().add(new Specialization(null, rs.getString(SPECIALIZATION_NAME)));
            mentor.getStudentsIds().add(rs.getLong(STUDENT_ID));
        }

        return new PageImpl<>(extractedMentors.values().stream().toList());
    }
}
