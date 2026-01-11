package ua.moki.modules.users.dtos;

public record UserResponseDTO(
        String id,
        String firstName,
        String secondName,
        String email,
        String phoneNumber,
        String imageUrl,
        String roleType
) {
}
