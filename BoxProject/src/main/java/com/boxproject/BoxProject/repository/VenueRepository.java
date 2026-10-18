package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {
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
