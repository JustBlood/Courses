package ru.just.mentorcatalogservice.repository.mapper;

import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;
import ru.just.mentorcatalogservice.model.Mentor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Stream;

import static ru.just.mentorcatalogservice.model.Mentor.Column.*;

@Component
public class MentorResultSetExtractor implements ResultSetExtractor<Page<Mentor>> {

    @Override
    public Page<Mentor> extractData(ResultSet rs) throws SQLException, DataAccessException {
        List<Mentor> mentors = new ArrayList<>();
        while (rs.next()) {
            long mentorId = rs.getLong(ID);

            final long studentsCount = rs.getLong(STUDENTS_COUNT);
            List<Long> fakeStudentIdsForCounting = new ArrayList<>();
            for (int i = 0; i < studentsCount; i++) {
                fakeStudentIdsForCounting.add(studentsCount);
            }

            Mentor mentor = Mentor.builder()
                    .id(mentorId)
                    .userId(rs.getLong(USER_ID))
                    .shortAboutMe(rs.getString(SHORT_ABOUT_ME))
                    .longAboutMe(rs.getString(LONG_ABOUT_ME))
                    .specializations(Arrays.stream(rs.getString(SPECIALIZATIONS).split(",")).toList())
                    .studentsIds(fakeStudentIdsForCounting)
                    .build();
            mentors.add(mentor);
        }

        return new PageImpl<>(mentors);
    }
}
