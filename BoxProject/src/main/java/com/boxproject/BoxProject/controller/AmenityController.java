package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.AmenityRequestDTO;
import com.boxproject.BoxProject.model.AmenityResponseDTO;
import com.boxproject.BoxProject.service.AmenityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/amenities")
public class AmenityController {

    private final AmenityService amenityService;

    public AmenityController(AmenityService amenityService) {
        this.amenityService = amenityService;
    }

    @PostMapping
    public ResponseEntity<AmenityResponseDTO> createAmenity(
            @Valid @RequestBody AmenityRequestDTO request
    ) {
        AmenityResponseDTO amenity = amenityService.createAmenity(request);
        return ResponseEntity
                .created(URI.create("/api/amenities/" + amenity.getId()))
                .body(amenity);
    }

    @GetMapping
    public ResponseEntity<List<AmenityResponseDTO>> getAmenities(
            @RequestParam(defaultValue = "true") boolean activeOnly
    ) {
        return ResponseEntity.ok(amenityService.getAmenities(activeOnly));
    }

    @GetMapping("/{amenityId}")
    public ResponseEntity<AmenityResponseDTO> getAmenity(
            @PathVariable Long amenityId
    ) {
        return ResponseEntity.ok(amenityService.getAmenity(amenityId));
    }

    @PutMapping("/{amenityId}")
    public ResponseEntity<AmenityResponseDTO> updateAmenity(
            @PathVariable Long amenityId,
            @Valid @RequestBody AmenityRequestDTO request
    ) {
        return ResponseEntity.ok(amenityService.updateAmenity(amenityId, request));
    }

    @DeleteMapping("/{amenityId}")
    public ResponseEntity<Void> deleteAmenity(@PathVariable Long amenityId) {
        amenityService.deleteAmenity(amenityId);
        return ResponseEntity.noContent().build();
    }
}
