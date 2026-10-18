package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.VenueRequestDTO;
import com.boxproject.BoxProject.model.VenueResponseDTO;
import com.boxproject.BoxProject.service.VenueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @PostMapping
    public ResponseEntity<VenueResponseDTO> createVenue(
            @Valid @RequestBody VenueRequestDTO request
    ) {
        VenueResponseDTO venue = venueService.createVenue(request);
        return ResponseEntity
                .created(URI.create("/api/venues/" + venue.getId()))
                .body(venue);
    }

    @GetMapping
    public ResponseEntity<List<VenueResponseDTO>> getVenues(
            @RequestParam(required = false) String city,
            @RequestParam(required = false)
            @Pattern(
                    regexp = "draft|pending_approval|active|suspended|closed",
                    message = "Invalid venue status."
            ) String status
    ) {
        return ResponseEntity.ok(venueService.getVenues(city, status));
    }

    @GetMapping("/{venueId}")
    public ResponseEntity<VenueResponseDTO> getVenue(@PathVariable Long venueId) {
        return ResponseEntity.ok(venueService.getVenue(venueId));
    }

    @PutMapping("/{venueId}")
    public ResponseEntity<VenueResponseDTO> updateVenue(
            @PathVariable Long venueId,
            @Valid @RequestBody VenueRequestDTO request
    ) {
        return ResponseEntity.ok(venueService.updateVenue(venueId, request));
    }

    @DeleteMapping("/{venueId}")
    public ResponseEntity<Void> deleteVenue(@PathVariable Long venueId) {
        venueService.deleteVenue(venueId);
        return ResponseEntity.noContent().build();
    }
}
