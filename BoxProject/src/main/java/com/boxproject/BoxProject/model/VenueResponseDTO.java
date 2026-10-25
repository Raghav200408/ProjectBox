package com.boxproject.BoxProject.model;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
public class VenueResponseDTO {
    @Setter
    private Long ownerUserId;
    @Setter
    private List<Long> sportTypeIds;
    @Setter
    private List<Long> amenityIds;
    private final Long id;
    private final String name;
    private final String description;
    private final String phone;
    private final String email;
    private final String addressLine1;
    private final String addressLine2;
    private final String locality;
    private final String city;
    private final String state;
    private final String postalCode;
    private final String locationUrl;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final String timezone;
    private final String status;
    private final Instant createdAt;
    private final Instant updatedAt;
}
