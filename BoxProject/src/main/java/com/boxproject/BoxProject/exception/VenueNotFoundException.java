package com.boxproject.BoxProject.exception;

public class VenueNotFoundException extends RuntimeException {
    public VenueNotFoundException(Long venueId) {
        super("Venue not found: " + venueId);
    }
}
