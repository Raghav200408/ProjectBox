package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.Amenity;
import com.boxproject.BoxProject.exception.AmenityNotFoundException;
import com.boxproject.BoxProject.model.AmenityRequestDTO;
import com.boxproject.BoxProject.model.AmenityResponseDTO;
import com.boxproject.BoxProject.repository.AmenityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AmenityService {

    private final AmenityRepository amenityRepository;

    public AmenityService(AmenityRepository amenityRepository) {
        this.amenityRepository = amenityRepository;
    }

    public AmenityResponseDTO createAmenity(AmenityRequestDTO request) {
        return toResponse(amenityRepository.create(request));
    }

    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getAmenities(boolean activeOnly) {
        return amenityRepository.findAll(activeOnly)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AmenityResponseDTO getAmenity(Long amenityId) {
        return amenityRepository.findById(amenityId)
                .map(this::toResponse)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
    }

    public AmenityResponseDTO updateAmenity(Long amenityId, AmenityRequestDTO request) {
        return amenityRepository.update(amenityId, request)
                .map(this::toResponse)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
    }

    public void deleteAmenity(Long amenityId) {
        if (!amenityRepository.deactivate(amenityId)) {
            throw new AmenityNotFoundException(amenityId);
        }
    }

    private AmenityResponseDTO toResponse(Amenity amenity) {
        return new AmenityResponseDTO(
                amenity.getId(),
                amenity.getName(),
                amenity.isActive()
        );
    }
}
