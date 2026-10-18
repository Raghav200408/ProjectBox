package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.exception.UserNotFoundException;
import com.boxproject.BoxProject.exception.VenueMemberNotFoundException;
import com.boxproject.BoxProject.exception.VenueNotFoundException;
import com.boxproject.BoxProject.entity.VenueMember;
import com.boxproject.BoxProject.model.VenueMemberRequestDTO;
import com.boxproject.BoxProject.model.VenueMemberResponseDTO;
import com.boxproject.BoxProject.repository.UserRepository;
import com.boxproject.BoxProject.repository.VenueMemberRepository;
import com.boxproject.BoxProject.repository.VenueRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class VenueMemberService {

    private final VenueRepository venueRepository;
    private final VenueMemberRepository memberRepository;
    private final UserRepository userRepository;

    public VenueMemberService(
            VenueRepository venueRepository,
            VenueMemberRepository memberRepository,
            UserRepository userRepository
    ) {
        this.venueRepository = venueRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
    }

    public VenueMemberResponseDTO createMember(
            Long venueId,
            VenueMemberRequestDTO request
    ) {
        requireVenue(venueId);
        requireUser(request.getUserId());
        return toResponse(memberRepository.create(venueId, request));
    }

    @Transactional(readOnly = true)
    public List<VenueMemberResponseDTO> getMembers(
            Long venueId,
            boolean activeOnly
    ) {
        requireVenue(venueId);
        return memberRepository.findAll(venueId, activeOnly)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VenueMemberResponseDTO getMember(Long venueId, Long memberId) {
        requireVenue(venueId);
        return memberRepository.findById(venueId, memberId)
                .map(this::toResponse)
                .orElseThrow(() -> new VenueMemberNotFoundException(memberId));
    }

    public VenueMemberResponseDTO updateMember(
            Long venueId,
            Long memberId,
            VenueMemberRequestDTO request
    ) {
        requireVenue(venueId);
        requireUser(request.getUserId());
        return memberRepository.update(venueId, memberId, request)
                .map(this::toResponse)
                .orElseThrow(() -> new VenueMemberNotFoundException(memberId));
    }

    public void deleteMember(Long venueId, Long memberId) {
        requireVenue(venueId);
        if (memberRepository.findById(venueId, memberId).isEmpty()) {
            throw new VenueMemberNotFoundException(memberId);
        }
        memberRepository.deactivate(venueId, memberId);
    }

    private void requireVenue(Long venueId) {
        if (!venueRepository.existsActiveById(venueId)) {
            throw new VenueNotFoundException(venueId);
        }
    }

    private void requireUser(Long userId) {
        if (userRepository.findByUserIdAndDeletedAtIsNull(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
    }

    private VenueMemberResponseDTO toResponse(VenueMember member) {
        return new VenueMemberResponseDTO(
                member.getId(),
                member.getVenue().getId(),
                member.getUser().getUserId(),
                member.getMemberType(),
                member.isActive(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
