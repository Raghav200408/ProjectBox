package com.boxproject.BoxProject.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class VenueResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String phone;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String locality;
    private String city;
    private String state;
    private String postalCode;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String timezone;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
}
