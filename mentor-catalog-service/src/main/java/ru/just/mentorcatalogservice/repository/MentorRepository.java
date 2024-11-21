package ru.just.mentorcatalogservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.just.mentorcatalogservice.dto.CreateMentorDto;
import ru.just.mentorcatalogservice.dto.MentorDto;
import ru.just.mentorcatalogservice.dto.StudentDto;
import ru.just.mentorcatalogservice.model.Mentor;
import ru.just.mentorcatalogservice.repository.mapper.MentorResultSetExtractor;

@Repository
@RequiredArgsConstructor
public class MentorRepository {
    private final NamedParameterJdbcTemplate namedTemplate;
    private final MentorResultSetExtractor mentorResultSetExtractor;

    public Page<Mentor> findMentorsBySpecialization(String specialization, Pageable pageable) {
        final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("specialization", specialization);
        mapSqlParameterSource.addValue("offset", pageable.getOffset());
        mapSqlParameterSource.addValue("limit", pageable.getPageSize());
        // TODO: убрать нахуй этот sql в ресурсы и переработать
        final String sql = """
                select m.id as "id", m.user_id as "user_id", m.short_about_me as "short_about_me", m.long_about_me as "long_about_me", st.student_id as "student_id", s.name as "specialization_name"
                from mentor m join mentor_specialization ms on m.id = ms.mentor_id join specialization s on s.id = ms.specialization_id join mentor_student as st on m.id = st.mentor_id
                where m.id IN (SELECT m2.id FROM mentor m2 ORDER BY m2.id OFFSET :offset LIMIT :limit)
                    AND s.name ILIKE :specialization
                """;
        return namedTemplate.query(sql, mapSqlParameterSource, mentorResultSetExtractor);
    }

    public MentorDto createMentor(CreateMentorDto createMentorDto) {
        return null;
    }

    public void addStudentToMentor(Long mentorId, StudentDto studentDto) {
        String sql = "INSERT INTO mentor_student (id, mentor_id) VALUES(:studentId, :mentor_id)";
        final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("studentId", studentDto.getStudentId());
        mapSqlParameterSource.addValue("mentorId", mentorId);

        namedTemplate.update(sql, mapSqlParameterSource);
    }

    public boolean isStudentHasMentor(Long studentId, Long mentorId) {
        final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("studentId", studentId);
        mapSqlParameterSource.addValue("mentorId", mentorId);
        final String query = "SELECT COUNT(*) FROM mentor_student WHERE student_id = :studentId AND mentor_id = :mentorId LIMIT 1";
        Integer count = namedTemplate.queryForObject(query, mapSqlParameterSource, Integer.class);
        return count != null && count > 1;
    }

    public Boolean isMentorExists(Long userId) {
        String sql = "SELECT COUNT(*) FROM mentor WHERE user_id = :userId";
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("userId", userId);
        final Integer returnedCount = namedTemplate.queryForObject(sql, mapSqlParameterSource, Integer.class);
        return returnedCount != null && returnedCount == 1;
    }
}
