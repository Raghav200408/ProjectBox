package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT v.* FROM venues v
            WHERE v.status = 'active' AND v.deleted_at IS NULL
              AND ST_DWithin(v.location, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography), 15000)
            ORDER BY ST_Distance(v.location, CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)), v.id
            """, nativeQuery = true)
    List<Venue> findNearbyActive(@org.springframework.data.repository.query.Param("latitude") double latitude,
                               @org.springframework.data.repository.query.Param("longitude") double longitude);
    Optional<Venue> findByIdAndDeletedAtIsNull(Long venueId);
    boolean existsByIdAndDeletedAtIsNull(Long venueId);
    List<Venue> findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc();
    List<Venue> findAllByCityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(String city);
    List<Venue> findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(String status);
    List<Venue> findAllByCityIgnoreCaseAndStatusAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
            String city,
            String status
    );
}
