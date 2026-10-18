package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.VenueAmenity;
import com.boxproject.BoxProject.exception.AmenityNotFoundException;
import com.boxproject.BoxProject.exception.VenueNotFoundException;
import com.boxproject.BoxProject.model.VenueAmenityRequestDTO;
import com.boxproject.BoxProject.model.VenueAmenityResponseDTO;
import com.boxproject.BoxProject.repository.AmenityRepository;
import com.boxproject.BoxProject.repository.VenueAmenityRepository;
import com.boxproject.BoxProject.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VenueAmenityService {

    private final VenueRepository venueRepository;
    private final AmenityRepository amenityRepository;
    private final VenueAmenityRepository venueAmenityRepository;

    public VenueAmenityService(
            VenueRepository venueRepository,
            AmenityRepository amenityRepository,
            VenueAmenityRepository venueAmenityRepository
    ) {
        this.venueRepository = venueRepository;
        this.amenityRepository = amenityRepository;
        this.venueAmenityRepository = venueAmenityRepository;
    }

    public VenueAmenityResponseDTO addAmenity(
            Long venueId,
            VenueAmenityRequestDTO request
    ) {
        requireVenue(venueId);
        requireActiveAmenity(request.getAmenityId());
        venueAmenityRepository.create(venueId, request.getAmenityId());
        return venueAmenityRepository.findById(venueId, request.getAmenityId())
                .map(this::toResponse)
                .orElseThrow(() -> new AmenityNotFoundException(request.getAmenityId()));
    }

    @Transactional(readOnly = true)
    public List<VenueAmenityResponseDTO> getAmenities(
            Long venueId,
            boolean activeOnly
    ) {
        requireVenue(venueId);
        return venueAmenityRepository.findAll(venueId, activeOnly)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public void removeAmenity(Long venueId, Long amenityId) {
        requireVenue(venueId);
        if (!venueAmenityRepository.delete(venueId, amenityId)) {
            throw new AmenityNotFoundException(amenityId);
        }
    }

    private void requireVenue(Long venueId) {
        if (!venueRepository.existsActiveById(venueId)) {
            throw new VenueNotFoundException(venueId);
        }
    }

    private void requireActiveAmenity(Long amenityId) {
        if (amenityRepository.findActiveById(amenityId).isEmpty()) {
            throw new AmenityNotFoundException(amenityId);
        }
    }

    private VenueAmenityResponseDTO toResponse(VenueAmenity venueAmenity) {
        return new VenueAmenityResponseDTO(
                venueAmenity.getId(),
                venueAmenity.getVenue().getId(),
                venueAmenity.getAmenity().getId(),
                venueAmenity.getAmenity().getName(),
                venueAmenity.getAmenity().isActive()
        );
    }
}
