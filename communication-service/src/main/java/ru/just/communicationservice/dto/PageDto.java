package ru.just.communicationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageDto {
    int page = 0;
    int size = 20;
}
