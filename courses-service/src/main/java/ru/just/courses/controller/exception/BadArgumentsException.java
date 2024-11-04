package ru.just.courses.controller.exception;

public class BadArgumentsException extends RuntimeException {
    public BadArgumentsException(String message) {
        super(message);
    }
}
