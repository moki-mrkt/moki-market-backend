package ua.moki.modules.products.dtos;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import ua.moki.modules.products.enums.ProductAvailability;
import ua.moki.modules.products.enums.ProductCategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ProductRequestDTO(
        @NotBlank(message = "name of product should not be empty")
        @Size(min = 2, max = 32,
                message = "name of product must be greater than 2 and less than 32")
        String name,
        ProductCategory productCategory,
        @Size(min = 2, max = 1000,
                message = "description of product must be greater than 2 and less than 1000")
        String description,
        @Min(0)
        @Max(100000)
        BigDecimal price,
        ProductAvailability availability,
        @Min(0)
        @Max(99)
        Integer discount,
        @Min(0)
        @Max(100000)
        BigDecimal purchasePrice,
        @NotBlank(message = "manufacturer of product should not be empty")
        @Size(min = 2, max = 64,
            message = "manufacturer of product must be greater than 2 and less than 64")
        String manufacturerOfTheProduct,
        @NotBlank(message = "subcategory of product should not be empty")
        String subcategory,
        @NotBlank(message = "Init of measure should not be empty")
        @Pattern(regexp = "(г|кг|мл|л|см|м|шт|-)")
        String initOfMeasure,
        @NotNull(message = "Value cannot be null")
        @Min(value = 0, message = "Value must be positive")
        @Max(value = 999, message = "Value must be less than 1000")
        Integer valueOfInitOfMeasure,
        @Nullable
        @Size(max = 4,
                message = "Can't upload more than 4 files")
        List<ProductImageDTO> images,
        @Nullable
        Map<String, String> characteristics
) {
}
