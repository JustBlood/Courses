package ru.just.dtolib.users;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UsersInfoByIdsDto {
    @Size(min = 1, max = 20, message = "size of id's should be in range [1,20]")
    List<Long> userIds;
}
