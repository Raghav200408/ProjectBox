package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.Amenity;
import com.boxproject.BoxProject.entity.Venue;
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
        VenueAmenity assignment = new VenueAmenity();
        assignment.setVenue(requireVenue(venueId));
        assignment.setAmenity(requireActiveAmenity(request.getAmenityId()));
        return toResponse(venueAmenityRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<VenueAmenityResponseDTO> getAmenities(
            Long venueId,
            boolean activeOnly
    ) {
        requireVenue(venueId);
        List<VenueAmenity> assignments = activeOnly
                ? venueAmenityRepository
                        .findAllByVenueIdAndAmenityActiveTrueOrderByAmenityNameAscAmenityIdAsc(
                                venueId
                        )
                : venueAmenityRepository
                        .findAllByVenueIdOrderByAmenityNameAscAmenityIdAsc(venueId);
        return assignments.stream().map(this::toResponse).toList();
    }

    public void removeAmenity(Long venueId, Long amenityId) {
        requireVenue(venueId);
        if (venueAmenityRepository.deleteByVenueIdAndAmenityId(venueId, amenityId) == 0) {
            throw new AmenityNotFoundException(amenityId);
        }
    }

    private Venue requireVenue(Long venueId) {
        return venueRepository.findByIdAndDeletedAtIsNull(venueId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private Amenity requireActiveAmenity(Long amenityId) {
        return amenityRepository.findByIdAndActiveTrue(amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
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
