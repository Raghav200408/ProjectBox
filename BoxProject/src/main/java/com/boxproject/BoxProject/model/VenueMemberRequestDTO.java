package com.boxproject.BoxProject.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VenueMemberRequestDTO {

    @NotNull(message = "User ID is required.")
    private Long userId;

    @NotNull(message = "Member type is required.")
    @Pattern(regexp = "owner|staff", message = "Member type must be owner or staff.")
    private String memberType;

    private Boolean active;
}
