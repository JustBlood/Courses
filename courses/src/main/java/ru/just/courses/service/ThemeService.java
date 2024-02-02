package ru.just.courses.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.just.courses.dto.CreateThemeDto;
import ru.just.courses.dto.ThemeDto;
import ru.just.courses.model.theme.Theme;
import ru.just.courses.model.theme.content.TextThemeContent;
import ru.just.courses.repository.ModuleRepository;
import ru.just.courses.repository.TextThemeContentRepository;
import ru.just.courses.repository.ThemeRepository;
import ru.just.courses.repository.exceptions.EntityNotFoundException;
import ru.just.courses.repository.projection.TextThemeContentProjection;

import java.io.*;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ThemeService {
    private final ThemeRepository themeRepository;
    private final ModuleRepository moduleRepository;
    private final TextThemeContentRepository textContentRepository;
    private final EntityManager entityManager;

    public Optional<ThemeDto> findThemeById(Long themeId) {
        return themeRepository.findById(themeId).map(new ThemeDto()::fromEntity);
    }

    public ThemeDto createTheme(CreateThemeDto dto) {
        if (!moduleRepository.existsById(dto.getModuleId())) {
            throw new NoSuchElementException("module with specified id doesn't exists");
        }
        final Theme theme = themeRepository.save(dto.toEntity());
        return new ThemeDto().fromEntity(theme);
    }

    public void deleteById(Long themeId) {
        themeRepository.deleteById(themeId);
    }

    public void addTextContentToTheme(Long themeId, InputStream inputStream, Integer textLength) {
        Session session = entityManager.unwrap(Session.class);
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        Clob text = session.getLobHelper().createClob(br, textLength);
        textContentRepository.save(new TextThemeContent(text, themeId));
    }

    @Transactional
    public void writeTextThemeContentToResponse(Long themeId, OutputStream out) {
        TextThemeContentProjection themeContent = textContentRepository.findTextThemeContentByThemeId(themeId)
                .orElseThrow(() -> new EntityNotFoundException("Не удалось передать текстовый контент"));
        writeResponse(themeContent, out);
    }

    private void writeResponse(TextThemeContentProjection themeContent, OutputStream out) {
        byte[] buffer = new byte[1024];
        try (BufferedInputStream br = new BufferedInputStream(themeContent.getText().getAsciiStream())){
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
