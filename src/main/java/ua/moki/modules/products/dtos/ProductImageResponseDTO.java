package ua.moki.modules.products.dtos;

import jakarta.validation.constraints.NotBlank;

public record ProductImageResponseDTO(
        @NotBlank String imageId,
        String imageUrl,
        boolean isMain,
        int sortOrder,
        String altText
) {
}
