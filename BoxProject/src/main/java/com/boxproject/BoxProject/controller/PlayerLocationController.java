package com.boxproject.BoxProject.controller;

import com.boxproject.BoxProject.model.PlayerLocationRequestDTO;
import com.boxproject.BoxProject.model.VenueResponseDTO;
import com.boxproject.BoxProject.service.PlayerLocationService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/players/me")
public class PlayerLocationController {
    private final PlayerLocationService locations;
    public PlayerLocationController(PlayerLocationService locations) { this.locations = locations; }

    @PutMapping("/location")
    public ResponseEntity<Void> save(Authentication account, @Valid @RequestBody PlayerLocationRequestDTO fix) {
        locations.save(Long.parseLong(account.getName()), fix);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @GetMapping("/location")
    public ResponseEntity<PlayerLocationRequestDTO> latest(Authentication account) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(locations.latest(Long.parseLong(account.getName())));
    }

    @PostMapping("/nearby")
    public ResponseEntity<List<VenueResponseDTO>> nearby(Authentication account, @Valid @RequestBody PlayerLocationRequestDTO fix) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(locations.nearby(Long.parseLong(account.getName()), fix));
    }
}
