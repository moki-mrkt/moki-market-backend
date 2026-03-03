package ua.moki.modules.users.utils.mappers;

import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageMapper {

    @Value("${s3.public_url}")
    private String minioUrl;

    @Named("toFullUrl")
    public String toFullUrl(String imageId) {
        if (imageId == null || imageId.isBlank()) {
            return null;
        }

        return minioUrl.endsWith("/") ? minioUrl + imageId : minioUrl + "/" + imageId;
    }
}
