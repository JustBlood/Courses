package ru.just.userservice.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.just.dtolib.error.ApiError;
import ru.just.userservice.exception.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = {EntityNotFoundException.class})
    public ResponseEntity<ApiError> entityNotFoundExceptionHandler(EntityNotFoundException ex) {
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiError> handleException(Exception e) {
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), e.toString()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNoSuchElementException(NoSuchElementException e) {
        return new ResponseEntity<>(new ApiError(LocalDateTime.now(), e.getMessage()),
                HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> handleUsernameAlreadyExistsException(MethodArgumentNotValidException ex) {
        StringBuilder errorMsgBuilder = new StringBuilder().append("validation is failed!");
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            errorMsgBuilder.append("\n").append(error.getDefaultMessage());
        }
        return new ResponseEntity<>(
                new ApiError(LocalDateTime.now(), errorMsgBuilder.toString()), HttpStatus.BAD_REQUEST);
    }
}
