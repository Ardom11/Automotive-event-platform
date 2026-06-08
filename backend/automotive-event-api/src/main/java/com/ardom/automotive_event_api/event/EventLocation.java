package com.ardom.automotive_event_api.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventLocation {

    @Column(name = "location_place", nullable = false)
    private String place;

    @Column(name = "location_address", nullable = false)
    private String address;

    @Column(name = "location_city", nullable = false)
    private String city;

    @Column(name = "location_country", nullable = false)
    private String country;

    @Column(name = "location_latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "location_longitude", precision = 9, scale = 6)
    private BigDecimal longitude;
}
