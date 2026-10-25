package com.boxproject.BoxProject.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.HashSet;
import com.boxproject.BoxProject.validation.Create;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.AssertTrue;

@Getter
@Setter
public class VenueRequestDTO {
    public interface Update {}

    @NotNull(groups = Create.class, message = "Owner user ID is required.")
    @Positive
    @Null(groups = Update.class, message = "Use the membership API to change the owner.")
    private Long ownerUserId;

    @NotEmpty(groups = Create.class, message = "Select at least one sport.")
    @Null(groups = Update.class, message = "Sports cannot be changed through the profile update API.")
    private List<@NotNull @Positive Long> sportTypeIds;

    @Null(groups = Update.class, message = "Use the amenities API to change amenities.")
    private List<@NotNull @Positive Long> amenityIds;

    @AssertTrue(message = "Sport and amenity IDs must not contain duplicates.")
    public boolean isSelectionUnique() {
        return (sportTypeIds == null || new HashSet<>(sportTypeIds).size() == sportTypeIds.size())
                && (amenityIds == null || new HashSet<>(amenityIds).size() == amenityIds.size());
    }


    @NotBlank(message = "Venue name is required.")
    @Size(max = 150)
    private String name;

    private String description;

    @NotBlank(message = "Phone number is required.")
    @Size(max = 20)
    private String phone;

    @Email(message = "Enter a valid email address.")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Address line 1 is required.")
    @Size(max = 200)
    private String addressLine1;

    @Size(max = 200)
    private String addressLine2;

    @NotBlank(message = "Locality is required.")
    @Size(max = 100)
    private String locality;

    @NotBlank(message = "City is required.")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required.")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Postal code is required.")
    @Size(max = 10)
    private String postalCode;

    @org.hibernate.validator.constraints.URL(protocol = "https", message = "Enter a valid HTTPS location URL.")
    private String locationUrl;

    @NotNull(message = "Latitude is required.")
    @DecimalMin(value = "-90.0")
    @DecimalMax(value = "90.0")
    private BigDecimal latitude;

    @NotNull(message = "Longitude is required.")
    @DecimalMin(value = "-180.0")
    @DecimalMax(value = "180.0")
    private BigDecimal longitude;

    @Size(max = 50)
    private String timezone;

    @Pattern(
            regexp = "draft|pending_approval|active|suspended|closed",
            message = "Status must be draft, pending_approval, active, suspended or closed."
    )
    private String status;
}
