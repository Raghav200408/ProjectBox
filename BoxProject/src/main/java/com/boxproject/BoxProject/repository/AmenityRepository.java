package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, Long> {
    Optional<Amenity> findByIdAndActiveTrue(Long amenityId);
    List<Amenity> findAllByOrderByNameAscIdAsc();
    List<Amenity> findAllByActiveTrueOrderByNameAscIdAsc();
}
