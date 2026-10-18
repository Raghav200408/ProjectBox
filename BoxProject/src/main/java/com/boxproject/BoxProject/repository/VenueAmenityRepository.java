package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.VenueAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VenueAmenityRepository extends JpaRepository<VenueAmenity, Long> {
    Optional<VenueAmenity> findByVenueIdAndAmenityId(Long venueId, Long amenityId);
    List<VenueAmenity> findAllByVenueIdOrderByAmenityNameAscAmenityIdAsc(Long venueId);
    List<VenueAmenity> findAllByVenueIdAndAmenityActiveTrueOrderByAmenityNameAscAmenityIdAsc(
            Long venueId
    );
    long deleteByVenueIdAndAmenityId(Long venueId, Long amenityId);
}
