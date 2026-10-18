package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.VenueAmenityRequestDTO;
import com.boxproject.BoxProject.model.VenueAmenityResponseDTO;
import com.boxproject.BoxProject.service.VenueAmenityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/venues/{venueId}/amenities")
public class VenueAmenityController {

    private final VenueAmenityService venueAmenityService;

    public VenueAmenityController(VenueAmenityService venueAmenityService) {
        this.venueAmenityService = venueAmenityService;
    }

    @PostMapping
    public ResponseEntity<VenueAmenityResponseDTO> addAmenity(
            @PathVariable Long venueId,
            @Valid @RequestBody VenueAmenityRequestDTO request
    ) {
        VenueAmenityResponseDTO assignment = venueAmenityService.addAmenity(venueId, request);
        URI location = URI.create(
                "/api/venues/" + venueId + "/amenities/" + assignment.getAmenityId()
        );
        return ResponseEntity.created(location).body(assignment);
    }

    @GetMapping
    public ResponseEntity<List<VenueAmenityResponseDTO>> getAmenities(
            @PathVariable Long venueId,
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(venueAmenityService.getAmenities(venueId, activeOnly));
    }

    @DeleteMapping("/{amenityId}")
    public ResponseEntity<Void> removeAmenity(
            @PathVariable Long venueId,
            @PathVariable Long amenityId
    ) {
        venueAmenityService.removeAmenity(venueId, amenityId);
        return ResponseEntity.noContent().build();
    }
}
