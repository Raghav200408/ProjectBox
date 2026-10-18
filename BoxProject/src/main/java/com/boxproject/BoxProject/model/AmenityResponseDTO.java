package com.boxproject.BoxProject.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AmenityResponseDTO {
    private Long id;
    private String name;
    private boolean active;
}
