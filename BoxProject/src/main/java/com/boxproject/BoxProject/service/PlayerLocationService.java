package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.PlayerLocation;
import com.boxproject.BoxProject.model.PlayerLocationRequestDTO;
import com.boxproject.BoxProject.model.VenueResponseDTO;
import com.boxproject.BoxProject.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class PlayerLocationService {
    private final PlayerLocationRepository locations;
    private final UserRepository users;
    private final VenueRepository venues;
    private final VenueService venueService;

    public PlayerLocationService(PlayerLocationRepository locations, UserRepository users,
                                 VenueRepository venues, VenueService venueService) {
        this.locations = locations; this.users = users; this.venues = venues; this.venueService = venueService;
    }

    public void save(long userId, PlayerLocationRequestDTO fix) {
        validateFresh(fix, Instant.now());
        var user = users.lockActiveLocationUser(userId)
                .filter(u -> "active".equals(u.getStatus()) && "PLAYER".equals(u.getRole().getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
        var location = locations.findById(userId).orElseGet(() -> {
            var created = new PlayerLocation(); created.setUser(user); return created;
        });
        // Do not let an older, delayed request replace a newer fix.
        if (location.getCapturedAt() != null && location.getCapturedAt().isAfter(fix.capturedAt())) return;
        location.setLatitude(fix.latitude()); location.setLongitude(fix.longitude());
        location.setAccuracyMeters(fix.accuracyMeters()); location.setCapturedAt(fix.capturedAt());
        location.setUpdatedAt(Instant.now()); locations.saveAndFlush(location);
    }

    @Transactional(readOnly = true)
    public PlayerLocationRequestDTO latest(long userId) {
        var location = locations.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No location saved yet."));
        return new PlayerLocationRequestDTO(location.getLatitude(), location.getLongitude(),
                location.getAccuracyMeters(), location.getCapturedAt());
    }

    public List<VenueResponseDTO> nearby(long userId, PlayerLocationRequestDTO fix) {
        save(userId, fix);
        // Use the fresh request, not a previous signup location.
        return venues.findNearbyActive(fix.latitude(), fix.longitude()).stream().map(venueService::toResponse).toList();
    }

    static void validateFresh(PlayerLocationRequestDTO fix, Instant now) {
        if (fix == null || fix.latitude() == null || !Double.isFinite(fix.latitude()) || Math.abs(fix.latitude()) > 90
                || fix.longitude() == null || !Double.isFinite(fix.longitude()) || Math.abs(fix.longitude()) > 180
                || fix.accuracyMeters() == null || !Double.isFinite(fix.accuracyMeters())
                || fix.accuracyMeters() < 0 || fix.accuracyMeters() > 1000 || fix.capturedAt() == null
                || fix.capturedAt().isBefore(now.minusSeconds(120)) || fix.capturedAt().isAfter(now.plusSeconds(30))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Use a fresh location accurate to 1 km or better, or choose an area manually.");
        }
    }
}
