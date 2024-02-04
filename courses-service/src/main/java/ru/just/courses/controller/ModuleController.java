package ru.just.courses.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.just.courses.dto.CreateModuleDto;
import ru.just.courses.dto.ModuleDto;
import ru.just.courses.service.ModuleService;
import ru.just.dtolib.response.ApiResponse;

@RequiredArgsConstructor
@RequestMapping("/api/v1/modules")
@RestController
public class ModuleController {
    private final ModuleService moduleService;

    @GetMapping("/{moduleId}")
    public ResponseEntity<ModuleDto> getModuleById(@PathVariable Long moduleId) {
        return ResponseEntity.of(moduleService.getModuleById(moduleId));
    }

    @PostMapping
    public ResponseEntity<ModuleDto> createModule(@Valid @RequestBody CreateModuleDto createModuleDto) {
        return new ResponseEntity<>(moduleService.createModule(createModuleDto), HttpStatus.CREATED);
    }

    @DeleteMapping("/{moduleId}")
    public ResponseEntity<ApiResponse> deleteModuleById(@PathVariable Long moduleId) {
        final String message = String.format("Success deleting module with id = %s", moduleId);
        moduleService.deleteModuleById(moduleId);
        ApiResponse apiResponse = new ApiResponse(message);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
