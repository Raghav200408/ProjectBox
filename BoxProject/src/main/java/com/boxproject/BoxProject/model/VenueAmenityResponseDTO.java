package com.boxproject.BoxProject.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class VenueAmenityResponseDTO {
    private Long id;
    private Long venueId;
    private Long amenityId;
    private String name;
    private boolean active;
}
