package ru.just.courses.service.lesson;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.dto.lesson.CreateLessonDto;
import ru.just.courses.dto.lesson.LessonDto;
import ru.just.courses.mapper.LessonMapper;
import ru.just.courses.model.theme.lesson.HtmlLesson;
import ru.just.courses.repository.exception.EntityNotFoundException;
import ru.just.courses.repository.lesson.HtmlLessonRepository;
import ru.just.courses.repository.lesson.LessonRepository;
import ru.just.courses.repository.projection.HtmlContentProjection;

import java.io.*;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final HtmlLessonRepository htmlLessonRepository;
    private final LessonMapper lessonMapper;
    private final EntityManager entityManager;

    public LessonDto createLesson(CreateLessonDto createLessonDto) {
        return lessonMapper.apply(lessonRepository.save(createLessonDto.getModel()));
    }

    public Optional<LessonDto> getLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId).map(lessonMapper);
    }

    public void loadHtmlContentToLesson(Long lessonId, InputStream inputStream, long length) {
        // checkCourseAuthor(lessonId);
        Session session = entityManager.unwrap(Session.class);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        Clob html = session.getLobHelper().createClob(br, length);
        HtmlLesson htmlLesson = (HtmlLesson) lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Html lesson with specified id not found"));
        htmlLesson.setHtml(html);
        lessonRepository.save(htmlLesson);
    }

    public void checkCourseAuthor(Long themeId) {
        // todo
//        Theme theme = themeRepository.findById(themeId)
//                .orElseThrow(() -> new EntityNotFoundException("Theme with specified id not found"));
//        if (!theme.getModule().getCourse().getAuthorId().equals(tokenService.getUserId())) {
//            throw new MethodNotAllowedException("You are not a course author");
//        }
    }

    @Transactional
    public void writeTextThemeContentToResponse(Long themeId, OutputStream out) {
        HtmlContentProjection themeContent = htmlLessonRepository.findHtmlByLessonId(themeId)
                .orElseThrow(() -> new EntityNotFoundException("Не удалось передать текстовый контент"));
        writeResponse(themeContent, out);
    }

    private void writeResponse(HtmlContentProjection themeContent, OutputStream out) {
        byte[] buffer = new byte[1024];
        try (BufferedInputStream br = new BufferedInputStream(themeContent.getHtml().getAsciiStream())) {
            int realSize;
            while ((realSize = br.read(buffer)) != -1) {
                out.write(buffer, 0, realSize);
            }
            out.flush();
        } catch (IOException | SQLException e) {
            throw new EntityNotFoundException("Не удалось передать текстовый контент");
        }
    }
}
