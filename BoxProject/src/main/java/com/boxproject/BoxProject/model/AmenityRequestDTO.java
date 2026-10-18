package com.boxproject.BoxProject.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AmenityRequestDTO {

    @NotBlank(message = "Amenity name is required.")
    @Size(max = 100, message = "Amenity name must not exceed 100 characters.")
    private String name;

    private Boolean active;
}
