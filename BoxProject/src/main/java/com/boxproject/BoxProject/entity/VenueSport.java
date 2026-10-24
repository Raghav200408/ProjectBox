package com.boxproject.BoxProject.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "venue_sports", uniqueConstraints = @UniqueConstraint(
        name = "venue_sports_unique", columnNames = {"venue_id", "sport_type_id"}))
public class VenueSport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sport_type_id", nullable = false)
    private SportType sportType;
}
