package ru.just.userservice.controller.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.just.dtolib.error.ApiError;
import ru.just.userservice.controller.exception.EntityNotFoundException;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = {EntityNotFoundException.class})
    public ResponseEntity<ApiError> entityNotFoundExceptionHandler(EntityNotFoundException ex) {
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), ex.getMessage()), HttpStatus.NOT_FOUND);
    }
}
