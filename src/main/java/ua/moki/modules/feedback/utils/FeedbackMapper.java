package ua.moki.modules.feedback.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.moki.modules.feedback.domains.Feedback;
import ua.moki.modules.feedback.domains.ProductFeedback;
import ua.moki.modules.feedback.dtos.FeedbackResponseDTO;
import ua.moki.modules.feedback.dtos.ProductFeedbackResponseDTO;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "firstNameUser", source = "user.firstName")
    @Mapping(target = "secondNameUser", source = "user.secondName")
    @Mapping(target = "userImageUrl", source = "user.imageId")
    @Mapping(target = "type",
            expression = "java(feedback instanceof ua.moki.modules.feedback.domains.ProductFeedback ? \"PRODUCT\" : \"STORE\")")
    FeedbackResponseDTO toDto(Feedback feedback);

    @Mapping(target = "firstNameUser", source = "user.firstName")
    @Mapping(target = "secondNameUser", source = "user.secondName")
    @Mapping(target = "userImageUrl", source = "user.imageId")
    @Mapping(target = "productName", source = "product.name")
    @Mapping(target = "productSlug", source = "product.slug")
    ProductFeedbackResponseDTO toProductFeedbackDto(ProductFeedback feedback);
}
