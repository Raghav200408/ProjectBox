package com.boxproject.BoxProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "player_locations")
public class PlayerLocation {
    @Id
    @Column(name = "user_id")
    private Long userId;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false)
    private double latitude;
    @Column(nullable = false)
    private double longitude;
    @Column(name = "accuracy_meters", nullable = false)
    private double accuracyMeters;
    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
