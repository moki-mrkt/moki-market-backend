package ua.moki.modules.products.utils.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.context.i18n.LocaleContextHolder;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.*;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", uses = {ProductImageMapper.class})
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationTime", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "salesCount", ignore = true)
    @Mapping(target = "images", ignore = true)
    Product toEntity(ProductRequestDTO productRequestDTO);

    @Mapping(target = "name", expression = "java(getLocalizedName(product))")
    @Mapping(target = "description", expression = "java(getLocalizedDescription(product))")
    @Mapping(target = "characteristics", expression = "java(getLocalizedCharacteristics(product))")
    @Mapping(target = "subcategory", expression = "java(getLocalizedSubcategory(product))")
    @Mapping(target = "isFavorite", constant = "false")
    ProductResponseDTO toResponseDTO(Product product);

    @Mapping(target = "name", expression = "java(getLocalizedName(product))")
    @Mapping(target = "description", expression = "java(getLocalizedDescription(product))")
    @Mapping(target = "characteristics", expression = "java(getLocalizedCharacteristics(product))")
    @Mapping(target = "subcategory", expression = "java(getLocalizedSubcategory(product))")
    @Mapping(target = "isFavorite", constant = "false")
    ProductResponseDTO toResponseDTO(Product product, boolean isFavorite);

    ProductAdminResponseDTO toAdminResponseDTO(Product product);

    ProductPublicDTO toPublicDTO(Product product);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "salesCount", ignore = true)
    @Mapping(target = "creationTime", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateEntityFromDto(ProductRequestDTO dto, @MappingTarget Product entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "main", source = "isMain")
    ProductImage toImageEntity(ProductImageDTO dto);

    @Mapping(target = "isMain", source = "main")
    ProductImageDTO toImageDTO(ProductImage image);

    @AfterMapping
    default void updateImages(ProductRequestDTO dto, @MappingTarget Product entity) {
        if (dto.images() == null) return;

        List<ProductImage> incomingImages = dto.images().stream()
                .map(this::toImageEntity)
                .toList();

        entity.syncImages(incomingImages);
    }

    default String getLocalizedName(Product product) {
        if (product == null) return null;
        return "ru".equals(LocaleContextHolder.getLocale().getLanguage())
                    && !(product.getNameRu() == null || product.getNameRu().isBlank())
                ? product.getNameRu()
                : product.getName();
    }

    default String getLocalizedSubcategory(Product product) {
        if (product == null) return null;
        return "ru".equals(LocaleContextHolder.getLocale().getLanguage())
                && !(product.getSubcategoryRu() == null || product.getSubcategoryRu().isBlank())
                ? product.getSubcategoryRu()
                : product.getSubcategory();
    }

    default String getLocalizedDescription(Product product) {
        if (product == null) return null;
        return "ru".equals(LocaleContextHolder.getLocale().getLanguage())
                && !(product.getDescriptionRu() == null || product.getDescriptionRu().isBlank())
                ? product.getDescriptionRu()
                : product.getDescription();
    }

    default Map<String, String> getLocalizedCharacteristics(Product product) {
        if (product == null) return null;
        return "ru".equals(LocaleContextHolder.getLocale().getLanguage())
                    && product.getCharacteristicsRu() != null
                ? product.getCharacteristicsRu()
                : product.getCharacteristics();
    }
}
