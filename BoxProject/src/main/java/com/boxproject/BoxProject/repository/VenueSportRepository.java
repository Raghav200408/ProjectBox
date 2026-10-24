package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.VenueSport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VenueSportRepository extends JpaRepository<VenueSport, Long> {
    List<VenueSport> findAllByVenueIdOrderBySportTypeIdAsc(Long venueId);
}
