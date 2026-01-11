package ua.moki.modules.products.dtos;

import jakarta.validation.constraints.NotBlank;

public record ProductImageDTO(
        @NotBlank String imageId,
        boolean isMain,
        int sortOrder,
        String altText
) {
}
