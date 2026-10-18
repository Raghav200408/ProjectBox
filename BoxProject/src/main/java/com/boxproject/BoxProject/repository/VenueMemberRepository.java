package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.VenueMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VenueMemberRepository extends JpaRepository<VenueMember, Long> {
    Optional<VenueMember> findByIdAndVenueId(Long memberId, Long venueId);
    List<VenueMember> findAllByVenueIdOrderByCreatedAtAscIdAsc(Long venueId);
    List<VenueMember> findAllByVenueIdAndActiveTrueOrderByCreatedAtAscIdAsc(Long venueId);
}
