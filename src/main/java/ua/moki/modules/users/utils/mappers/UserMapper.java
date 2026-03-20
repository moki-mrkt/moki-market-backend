package ua.moki.modules.users.utils.mappers;

import org.mapstruct.*;
import ua.moki.modules.users.domains.User;
import ua.moki.modules.users.domains.UserDeliveryInfo;
import ua.moki.modules.users.dtos.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {ImageMapper.class}
)
public interface UserMapper {

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "imageUrl", source = "imageId", qualifiedByName = "toFullUrl")
    UserResponseDTO toResponseDTO(User user);

    @Mapping(target = "id", source = "publicId")
    @Mapping(target = "imageUrl", source = "imageId", qualifiedByName = "toFullUrl")
    UserAdminResponseDTO toAdminResponseDTO(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromDto(UserUpdateDTO dto, @MappingTarget User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateUserFromAdminDto(UserAdminUpdateDTO dto, @MappingTarget User user);

    DeliveryInfoDTO toDeliveryInfoDTO(UserDeliveryInfo deliveryInfo);

    UserDeliveryInfo toUserDeliveryInfo(DeliveryInfoDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDeliveryInfoFromDto(DeliveryInfoDTO dto, @MappingTarget UserDeliveryInfo deliveryInfo);
}
