package ua.moki.modules.users.domains;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.Getter;
import ua.moki.modules.orders.utils.enums.DeliveryType;

@Data
@Embeddable
public class UserDeliveryInfo {

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_type", length = 32)
    private DeliveryType deliveryType;

    @Column(name = "post_office", length = 128)
    private String postOffice;

    @Column(name = "region", length = 128)
    private String region;

    @Column(name = "delivery_city", length = 128)
    private String city;

    @Column(name = "delivery_street", length = 128)
    private String street;

    @Column(name = "delivery_house", length = 16)
    private String house;

}
