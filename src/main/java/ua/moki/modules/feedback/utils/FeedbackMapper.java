package ua.moki.modules.feedback.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ua.moki.modules.feedback.domains.Feedback;
import ua.moki.modules.feedback.dtos.FeedbackResponseDTO;

@Mapper(componentModel = "spring")
public interface FeedbackMapper {

    @Mapping(target = "firstNameUser", source = "user.firstName")
    @Mapping(target = "userImageUrl", source = "user.imageId")
    FeedbackResponseDTO toDto(Feedback feedback);
}
