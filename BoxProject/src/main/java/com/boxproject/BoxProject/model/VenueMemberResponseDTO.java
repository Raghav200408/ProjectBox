package com.boxproject.BoxProject.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class VenueMemberResponseDTO {
    private Long id;
    private Long venueId;
    private Long userId;
    private String memberType;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
