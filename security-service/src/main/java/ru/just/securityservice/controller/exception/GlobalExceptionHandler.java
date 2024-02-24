package ru.just.securityservice.controller.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.just.dtolib.error.ApiError;

import java.time.OffsetDateTime;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNoSuchElementException(NoSuchElementException e) {
        return new ResponseEntity<>(new ApiError(OffsetDateTime.now(), e.getMessage()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDeniedException(AccessDeniedException e) {
        return new ResponseEntity<>(new ApiError(OffsetDateTime.now(), e.getMessage()), HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(value = UsernameNotFoundException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameNotFoundException e) {
        return new ResponseEntity<>(new ApiError(OffsetDateTime.now(), e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(value = UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUsernameNotFoundException(UsernameAlreadyExistsException e) {
        return new ResponseEntity<>(new ApiError(OffsetDateTime.now(), e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> handleUsernameAlreadyExistsException(MethodArgumentNotValidException ex) {
        StringBuilder errorMsgBuilder = new StringBuilder().append("validation is failed!");
        for (ObjectError error : ex.getBindingResult().getAllErrors()) {
            errorMsgBuilder.append("\n").append(error.getDefaultMessage());
        }
        return new ResponseEntity<>(
                new ApiError(OffsetDateTime.now(), errorMsgBuilder.toString()), HttpStatus.BAD_REQUEST);
    }
}
