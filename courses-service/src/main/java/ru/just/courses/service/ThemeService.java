package ru.just.courses.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.controller.exception.MethodNotAllowedException;
import ru.just.courses.dto.CreateThemeDto;
import ru.just.courses.dto.ThemeDto;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.theme.content.TextThemeContent;
import ru.just.courses.repository.TextThemeContentRepository;
import ru.just.courses.repository.ThemeRepository;
import ru.just.courses.repository.exception.EntityNotFoundException;
import ru.just.courses.repository.projection.TextThemeContentProjection;
import ru.just.securitylib.service.ThreadLocalTokenService;

import java.io.*;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ThemeService {
    private final ThemeRepository themeRepository;
    private final ModuleService moduleService;
    private final TextThemeContentRepository textContentRepository;
    private final ThreadLocalTokenService tokenService;
    private final EntityManager entityManager;

    public Optional<ThemeDto> findThemeById(Long themeId) {
        return themeRepository.findById(themeId).map(new ThemeDto()::fromEntity);
    }

    public ThemeDto createTheme(CreateThemeDto dto) {
        moduleService.checkCourseAuthor(dto.getModuleId());

        final Theme theme = themeRepository.save(dto.toEntity());
        return new ThemeDto().fromEntity(theme);
    }

    public void deleteById(Long themeId) {
        checkCourseAuthor(themeId);
        themeRepository.deleteById(themeId);
    }

    public void addTextContentToTheme(Long themeId, InputStream inputStream, long length, Integer ordinalNumber) {
        checkCourseAuthor(themeId);
        Session session = entityManager.unwrap(Session.class);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        Clob text = session.getLobHelper().createClob(br, length);
        textContentRepository.save(new TextThemeContent(text, themeId, ordinalNumber));
    }

    @Transactional
    public void writeTextThemeContentToResponse(Long themeId, OutputStream out) {
        TextThemeContentProjection themeContent = textContentRepository.findTextThemeContentByThemeId(themeId)
                .orElseThrow(() -> new EntityNotFoundException("Не удалось передать текстовый контент"));
        writeResponse(themeContent, out);
    }

    private void writeResponse(TextThemeContentProjection themeContent, OutputStream out) {
        byte[] buffer = new byte[1024];
        try (BufferedInputStream br = new BufferedInputStream(themeContent.getText().getAsciiStream())) {
            int realSize;
            while ((realSize = br.read(buffer)) != -1) {
                out.write(buffer, 0, realSize);
            }
            out.flush();
        } catch (IOException | SQLException e) {
            throw new EntityNotFoundException("Не удалось передать текстовый контент");
        }
    }

    public void checkCourseAuthor(Long themeId) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new EntityNotFoundException("Theme with specified id not found"));
        if (!theme.getModule().getCourse().getAuthorId().equals(tokenService.getUserId())) {
            throw new MethodNotAllowedException("You are not a course author");
        }
    }
}
