package ru.just.mentorcatalogservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.just.mentorcatalogservice.dto.CreateMentorDto;
import ru.just.mentorcatalogservice.dto.StudentDto;
import ru.just.mentorcatalogservice.model.Mentor;
import ru.just.mentorcatalogservice.repository.mapper.MentorResultSetExtractor;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MentorRepository {
    private final NamedParameterJdbcTemplate namedTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final MentorResultSetExtractor mentorResultSetExtractor;

    public Page<Mentor> findMentorsBySpecialization(List<String> specializations, Pageable pageable) {
        final MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("specializations", specializations.stream().map(s -> "%" + s + "%").toList());
        mapSqlParameterSource.addValue("offset", pageable.getOffset());
        mapSqlParameterSource.addValue("limit", pageable.getPageSize());
        // TODO: убрать нахуй этот sql в ресурсы и переработать
        final String sql =
                """
          select m.id as "id", m.user_id as "user_id", m.short_about_me as "short_about_me",
                 m.long_about_me as "long_about_me",
                 (select count(*) from mentor_student mst where mst.mentor_id = m.id) as "students_count",
                 STRING_AGG(s.name, ',') as "specializations"
          from mentor m
              join mentor_specialization ms on m.id = ms.mentor_id
              join specialization s on s.id = ms.specialization_id
          where m.id IN (
              SELECT m2.id
              FROM mentor m2
              join mentor_specialization ms2 on m2.id = ms2.mentor_id
              join specialization s2 on s2.id = ms2.specialization_id
              where s2.name ILIKE any (array[:specializations])  -- фильтрация по специализациям
              order by m2.id
              OFFSET :offset LIMIT :limit
          )
          group by m.id, m.user_id, m.short_about_me, m.long_about_me
          order by m.id;
                """;
        return namedTemplate.query(sql, mapSqlParameterSource, mentorResultSetExtractor);
    }

    @Transactional
    public Mentor createMentor(CreateMentorDto createMentorDto) {
        String sql = "INSERT INTO mentor (user_id, short_about_me, long_about_me) VALUES(:userId, :shortAboutMe, :longAboutMe) RETURNING *";
        MapSqlParameterSource mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("userId", createMentorDto.getUserId());
        mapSqlParameterSource.addValue("shortAboutMe", createMentorDto.getShortAboutMe());
        mapSqlParameterSource.addValue("longAboutMe", createMentorDto.getLongAboutMe());
        Mentor mentor = namedTemplate.queryForObject(sql, mapSqlParameterSource, new BeanPropertyRowMapper<>(Mentor.class));
        mentor.setStudentsIds(new ArrayList<>());
        final List<String> specializations = createMentorDto.getSpecializations().stream().toList();
        mentor.setSpecializations(specializations);

        insertNewSpecializations(specializations);

        String insertMentorSpecializationSql = "INSERT INTO mentor_specialization(mentor_id, specialization_id) SELECT :mentorId, s.id FROM specialization AS s where s.name in (:mentorSpecializations)";
        mapSqlParameterSource = new MapSqlParameterSource();
        mapSqlParameterSource.addValue("mentorId", mentor.getId());
        mapSqlParameterSource.addValue("mentorSpecializations", createMentorDto.getSpecializations());
        namedTemplate.update(insertMentorSpecializationSql, mapSqlParameterSource);

        return mentor;
    }

    private void insertNewSpecializations(List<String> specializations) {
        String insertNewSpecializationsSql = "INSERT INTO specialization(name) VALUES (?) ON CONFLICT DO NOTHING";
        jdbcTemplate.batchUpdate(insertNewSpecializationsSql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, specializations.get(i));
            }

            @Override
            public int getBatchSize() {
                return specializations.size();
            }
        });
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
