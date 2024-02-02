package ru.just.courses.dto;

public abstract class Dto<T> {
    public abstract Dto<T> fromEntity(T entity);
    public abstract T toEntity();
}
