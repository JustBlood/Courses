package ru.just.courses.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.just.courses.repository.exception.EntityNotFoundException;
import ru.just.dtolib.error.ApiError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class CoursesExceptionHandler {
    @ExceptionHandler(value = {EntityNotFoundException.class})
    public ResponseEntity<ApiError> entityNotFoundExceptionHandler(EntityNotFoundException ex) {
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {NoSuchElementException.class})
    public ResponseEntity<ApiError> entityNotFoundExceptionHandler(NoSuchElementException ex) {
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler
    public ResponseEntity<ApiError> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException ex) {
        String errorMsg = "validation is failed!";
        if (ex.getErrorCount() > 0) {
            List<String> errorDetails = new ArrayList<>();
            for (ObjectError error : ex.getBindingResult().getAllErrors()) {
                errorDetails.add(error.getDefaultMessage());
            }
            if (!errorDetails.isEmpty()) errorMsg = errorDetails.getFirst();
        }
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), errorMsg), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {MethodNotAllowedException.class})
    public ResponseEntity<ApiError> methodNotAllowedExceptionHandler(MethodNotAllowedException ex) {
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), ex.getMessage()), HttpStatus.FORBIDDEN);
    }
}
