package ua.moki.modules.products.dtos;

import java.math.BigDecimal;

public record ProductWeightOptionDTO(
        Long id,
        Integer weightValue,
        BigDecimal price,
        Boolean isDefault,
        Boolean isExported
) {
}
