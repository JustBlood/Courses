package ru.just.communicationservice.dto;

import lombok.Data;

@Data
public class PageDto {
    int page = 0;
    int size = 20;
}
