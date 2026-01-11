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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
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

        Map<String, ProductImage> existingMap = entity.getImages().stream()
                .collect(Collectors.toMap(ProductImage::getImageId, Function.identity()));

        for (ProductImageDTO productImageDTO : dto.images()) {

            ProductImage image = existingMap.computeIfAbsent(productImageDTO.imageId(), id -> {
                ProductImage newImg = new ProductImage();
                newImg.setImageId(id);
                newImg.setProduct(entity);
                entity.addImage(newImg);
                return newImg;
            });

            image.updateDetails(productImageDTO.isMain(), productImageDTO.sortOrder(), productImageDTO.altText());
        }
    }

    @AfterMapping
    default void linkImages(@MappingTarget Product product, ProductRequestDTO dto) {
        if (dto.images() != null) {
            List<ProductImage> imageEntities = dto.images().stream()
                    .map(this::toImageEntity)
                    .peek(image -> image.setProduct(product))
                    .toList();

            product.setImages(imageEntities);
        }
    }
}
