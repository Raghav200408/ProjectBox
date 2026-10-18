package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.VenueMemberRequestDTO;
import com.boxproject.BoxProject.model.VenueMemberResponseDTO;
import com.boxproject.BoxProject.service.VenueMemberService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/venues/{venueId}/members")
public class VenueMemberController {

    private final VenueMemberService memberService;

    public VenueMemberController(VenueMemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<VenueMemberResponseDTO> createMember(
            @PathVariable Long venueId,
            @Valid @RequestBody VenueMemberRequestDTO request
    ) {
        VenueMemberResponseDTO member = memberService.createMember(venueId, request);
        URI location = URI.create(
                "/api/venues/" + venueId + "/members/" + member.getId()
        );
        return ResponseEntity.created(location).body(member);
    }

    @GetMapping
    public ResponseEntity<List<VenueMemberResponseDTO>> getMembers(
            @PathVariable Long venueId,
            @RequestParam(defaultValue = "false") boolean activeOnly
    ) {
        return ResponseEntity.ok(memberService.getMembers(venueId, activeOnly));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<VenueMemberResponseDTO> getMember(
            @PathVariable Long venueId,
            @PathVariable Long memberId
    ) {
        return ResponseEntity.ok(memberService.getMember(venueId, memberId));
    }

    @PutMapping("/{memberId}")
    public ResponseEntity<VenueMemberResponseDTO> updateMember(
            @PathVariable Long venueId,
            @PathVariable Long memberId,
            @Valid @RequestBody VenueMemberRequestDTO request
    ) {
        return ResponseEntity.ok(
                memberService.updateMember(venueId, memberId, request)
        );
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> deleteMember(
            @PathVariable Long venueId,
            @PathVariable Long memberId
    ) {
        memberService.deleteMember(venueId, memberId);
        return ResponseEntity.noContent().build();
    }
}
