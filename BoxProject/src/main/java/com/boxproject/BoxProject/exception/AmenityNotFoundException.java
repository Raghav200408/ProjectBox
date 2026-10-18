package com.boxproject.BoxProject.exception;

public class AmenityNotFoundException extends RuntimeException {
    public AmenityNotFoundException(Long amenityId) {
        super("Amenity not found: " + amenityId);
    }
}
