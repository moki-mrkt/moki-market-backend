package ua.moki.modules.products.utils.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import ua.moki.modules.products.domains.ProductImage;
import ua.moki.modules.products.dtos.ProductImageResponseDTO;

@Mapper(componentModel = "spring")
public abstract class ProductImageMapper {

    @Value("${s3.public_url}")
    protected String storageUrl;

    @Mapping(target = "imageUrl", source = "imageId", qualifiedByName = "mapToUrl")
    public abstract ProductImageResponseDTO toImageResponseDTO(ProductImage image);

    @Named("mapToUrl")
    protected String mapToUrl(String imageId) {
        if (imageId == null) return null;
        return storageUrl + imageId;
    }
}