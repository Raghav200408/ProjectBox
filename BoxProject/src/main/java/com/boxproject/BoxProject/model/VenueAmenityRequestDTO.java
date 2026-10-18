package com.boxproject.BoxProject.model;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueAmenityRequestDTO {

    @NotNull(message = "Amenity ID is required.")
    private Long amenityId;
}
