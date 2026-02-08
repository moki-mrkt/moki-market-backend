package ua.moki.modules.products.utils.mappers;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ua.moki.modules.products.domains.Product;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.ProductImageDTO;
import ua.moki.modules.products.dtos.ProductRequestDTO;
import ua.moki.modules.products.dtos.ProductResponseDTO;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ProductImageMapper.class})
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "creationTime", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "salesCount", ignore = true)
    @Mapping(target = "images", ignore = true)
    Product toEntity(ProductRequestDTO productRequestDTO);

    ProductResponseDTO toResponseDTO(Product product);

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
}
