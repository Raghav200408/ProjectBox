package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.User;
import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.entity.VenueMember;
import com.boxproject.BoxProject.exception.UserNotFoundException;
import com.boxproject.BoxProject.exception.VenueMemberNotFoundException;
import com.boxproject.BoxProject.exception.VenueNotFoundException;
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
        VenueMember member = new VenueMember();
        member.setVenue(requireVenue(venueId));
        member.setUser(requireUser(request.getUserId()));
        applyRequest(member, request);
        return toResponse(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public List<VenueMemberResponseDTO> getMembers(
            Long venueId,
            boolean activeOnly
    ) {
        requireVenue(venueId);
        List<VenueMember> members = activeOnly
                ? memberRepository.findAllByVenueIdAndActiveTrueOrderByCreatedAtAscIdAsc(venueId)
                : memberRepository.findAllByVenueIdOrderByCreatedAtAscIdAsc(venueId);
        return members.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VenueMemberResponseDTO getMember(Long venueId, Long memberId) {
        requireVenue(venueId);
        return findMember(venueId, memberId)
                .map(this::toResponse)
                .orElseThrow(() -> new VenueMemberNotFoundException(memberId));
    }

    public VenueMemberResponseDTO updateMember(
            Long venueId,
            Long memberId,
            VenueMemberRequestDTO request
    ) {
        requireVenue(venueId);
        VenueMember member = findMember(venueId, memberId)
                .orElseThrow(() -> new VenueMemberNotFoundException(memberId));
        member.setUser(requireUser(request.getUserId()));
        applyRequest(member, request);
        return toResponse(memberRepository.save(member));
    }

    public void deleteMember(Long venueId, Long memberId) {
        requireVenue(venueId);
        VenueMember member = findMember(venueId, memberId)
                .orElseThrow(() -> new VenueMemberNotFoundException(memberId));
        member.setActive(false);
        memberRepository.save(member);
    }

    private java.util.Optional<VenueMember> findMember(Long venueId, Long memberId) {
        return memberRepository.findByIdAndVenueId(memberId, venueId);
    }

    private Venue requireVenue(Long venueId) {
        return venueRepository.findByIdAndDeletedAtIsNull(venueId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private User requireUser(Long userId) {
        return userRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private static void applyRequest(
            VenueMember member,
            VenueMemberRequestDTO request
    ) {
        member.setMemberType(request.getMemberType());
        member.setActive(request.getActive() == null || request.getActive());
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
