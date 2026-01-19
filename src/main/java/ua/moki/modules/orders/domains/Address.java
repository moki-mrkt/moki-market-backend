package ua.moki.modules.orders.domains;

import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

public class Address {

    @Column(nullable = false)
    String city;
    String region;
    String department;
    String street;
    String houseNumber;
    String apartment;

}
