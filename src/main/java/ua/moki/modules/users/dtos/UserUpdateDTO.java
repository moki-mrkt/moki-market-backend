package ua.moki.modules.users.dtos;

import java.time.LocalDate;

public record UserUpdateDTO(
        String firstName,
        String secondName,
        String phoneNumber,
        LocalDate dateOfBirth
) {
}
