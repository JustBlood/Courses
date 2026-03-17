package ru.just.monolithmvp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.just.monolithmvp.dto.ApiResponse;
import ru.just.monolithmvp.dto.section.CreateSectionRequest;
import ru.just.monolithmvp.dto.section.SectionDto;
import ru.just.monolithmvp.dto.section.UpdateSectionRequest;
import ru.just.monolithmvp.service.SectionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin: Sections", description = "Управление разделами каталога курсов")
@SecurityRequirement(name = "bearerAuth")
@ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Не аутентифицирован", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Нет прав ADMIN", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
})
public class SectionsController {
    private final SectionService sectionService;

    @PostMapping
    @Operation(summary = "Создать раздел", description = "Создает новый раздел каталога курсов")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Раздел создан", content = @Content(schema = @Schema(implementation = SectionDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SectionDto> create(@Valid @RequestBody CreateSectionRequest request) {
        return new ResponseEntity<>(sectionService.create(request), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Получить список разделов", description = "Возвращает все разделы, отсортированные по приоритету")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Список разделов", content = @Content(array = @ArraySchema(schema = @Schema(implementation = SectionDto.class))))
    })
    public ResponseEntity<List<SectionDto>> getAll() {
        return ResponseEntity.ok(sectionService.getAll());
    }

    @GetMapping("/{sectionId}")
    @Operation(summary = "Получить раздел по ID", description = "Возвращает раздел каталога по идентификатору")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Раздел найден", content = @Content(schema = @Schema(implementation = SectionDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Раздел не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SectionDto> getById(@PathVariable Long sectionId) {
        return ResponseEntity.ok(sectionService.getById(sectionId));
    }

    @PutMapping("/{sectionId}")
    @Operation(summary = "Обновить раздел", description = "Обновляет название, описание и приоритет раздела")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Раздел обновлен", content = @Content(schema = @Schema(implementation = SectionDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Ошибка валидации", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Раздел не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<SectionDto> update(@PathVariable Long sectionId,
                                             @Valid @RequestBody UpdateSectionRequest request) {
        return ResponseEntity.ok(sectionService.update(sectionId, request));
    }

    @DeleteMapping("/{sectionId}")
    @Operation(summary = "Удалить раздел", description = "Удаляет раздел и переносит его курсы в раздел по умолчанию")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Раздел удален", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Раздел не найден", content = @Content(schema = @Schema(implementation = ru.just.monolithmvp.dto.ApiResponse.class)))
    })
    public ResponseEntity<ApiResponse> delete(@PathVariable Long sectionId) {
        sectionService.delete(sectionId);
        return ResponseEntity.ok(new ApiResponse("Section deleted"));
    }
}
