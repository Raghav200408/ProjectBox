package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.exception.VenueNotFoundException;
import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.model.VenueRequestDTO;
import com.boxproject.BoxProject.model.VenueResponseDTO;
import com.boxproject.BoxProject.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VenueService {

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public VenueResponseDTO createVenue(VenueRequestDTO request) {
        return toResponse(venueRepository.create(request));
    }

    @Transactional(readOnly = true)
    public VenueResponseDTO getVenue(Long venueId) {
        return venueRepository.findActiveById(venueId)
                .map(this::toResponse)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    @Transactional(readOnly = true)
    public List<VenueResponseDTO> getVenues(
            String city,
            String status
    ) {
        return venueRepository.findAll(city, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public VenueResponseDTO updateVenue(Long venueId, VenueRequestDTO request) {
        return venueRepository.update(venueId, request)
                .map(this::toResponse)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    public void deleteVenue(Long venueId) {
        if (!venueRepository.softDelete(venueId)) {
            throw new VenueNotFoundException(venueId);
        }
    }

    private VenueResponseDTO toResponse(Venue venue) {
        return new VenueResponseDTO(
                venue.getId(),
                venue.getName(),
                venue.getDescription(),
                venue.getPhone(),
                venue.getEmail(),
                venue.getAddressLine1(),
                venue.getAddressLine2(),
                venue.getLocality(),
                venue.getCity(),
                venue.getState(),
                venue.getPostalCode(),
                venue.getLatitude(),
                venue.getLongitude(),
                venue.getTimezone(),
                venue.getStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }
}
