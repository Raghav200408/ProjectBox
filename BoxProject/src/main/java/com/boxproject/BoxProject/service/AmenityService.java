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
        Amenity amenity = new Amenity();
        applyRequest(amenity, request);
        return toResponse(amenityRepository.save(amenity));
    }

    @Transactional(readOnly = true)
    public List<AmenityResponseDTO> getAmenities(boolean activeOnly) {
        List<Amenity> amenities = activeOnly
                ? amenityRepository.findAllByActiveTrueOrderByNameAscIdAsc()
                : amenityRepository.findAllByOrderByNameAscIdAsc();
        return amenities.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AmenityResponseDTO getAmenity(Long amenityId) {
        return amenityRepository.findById(amenityId)
                .map(this::toResponse)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
    }

    public AmenityResponseDTO updateAmenity(Long amenityId, AmenityRequestDTO request) {
        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        applyRequest(amenity, request);
        return toResponse(amenityRepository.save(amenity));
    }

    public void deleteAmenity(Long amenityId) {
        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new AmenityNotFoundException(amenityId));
        amenity.setActive(false);
        amenityRepository.save(amenity);
    }

    private static void applyRequest(Amenity amenity, AmenityRequestDTO request) {
        amenity.setName(request.getName().trim());
        amenity.setActive(request.getActive() == null || request.getActive());
    }

    private AmenityResponseDTO toResponse(Amenity amenity) {
        return new AmenityResponseDTO(
                amenity.getId(),
                amenity.getName(),
                amenity.isActive()
        );
    }
}
