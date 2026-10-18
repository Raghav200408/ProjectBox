package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.exception.VenueNotFoundException;
import com.boxproject.BoxProject.model.VenueRequestDTO;
import com.boxproject.BoxProject.model.VenueResponseDTO;
import com.boxproject.BoxProject.repository.VenueRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@Transactional
public class VenueService {

    private static final GeometryFactory GEOMETRY_FACTORY =
            new GeometryFactory(new PrecisionModel(), 4326);

    private final VenueRepository venueRepository;

    public VenueService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    public VenueResponseDTO createVenue(VenueRequestDTO request) {
        Venue venue = new Venue();
        applyRequest(venue, request);
        return toResponse(venueRepository.save(venue));
    }

    @Transactional(readOnly = true)
    public VenueResponseDTO getVenue(Long venueId) {
        return venueRepository.findByIdAndDeletedAtIsNull(venueId)
                .map(this::toResponse)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    @Transactional(readOnly = true)
    public List<VenueResponseDTO> getVenues(String city, String status) {
        String normalizedCity = trimToNull(city);
        String normalizedStatus = trimToNull(status);
        List<Venue> venues;

        if (normalizedCity != null && normalizedStatus != null) {
            venues = venueRepository
                    .findAllByCityIgnoreCaseAndStatusAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                            normalizedCity,
                            normalizedStatus
                    );
        } else if (normalizedCity != null) {
            venues = venueRepository
                    .findAllByCityIgnoreCaseAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                            normalizedCity
                    );
        } else if (normalizedStatus != null) {
            venues = venueRepository
                    .findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(
                            normalizedStatus
                    );
        } else {
            venues = venueRepository.findAllByDeletedAtIsNullOrderByCreatedAtDescIdDesc();
        }

        return venues.stream().map(this::toResponse).toList();
    }

    public VenueResponseDTO updateVenue(Long venueId, VenueRequestDTO request) {
        Venue venue = requireVenue(venueId);
        applyRequest(venue, request);
        return toResponse(venueRepository.save(venue));
    }

    public void deleteVenue(Long venueId) {
        Venue venue = requireVenue(venueId);
        venue.setStatus("closed");
        venue.setDeletedAt(Instant.now());
        venueRepository.save(venue);
    }

    private Venue requireVenue(Long venueId) {
        return venueRepository.findByIdAndDeletedAtIsNull(venueId)
                .orElseThrow(() -> new VenueNotFoundException(venueId));
    }

    private void applyRequest(Venue venue, VenueRequestDTO request) {
        venue.setName(request.getName().trim());
        venue.setDescription(trimToNull(request.getDescription()));
        venue.setPhone(request.getPhone().trim());
        venue.setEmail(trimToNull(request.getEmail()));
        venue.setAddressLine1(request.getAddressLine1().trim());
        venue.setAddressLine2(trimToNull(request.getAddressLine2()));
        venue.setLocality(request.getLocality().trim());
        venue.setCity(request.getCity().trim());
        venue.setState(request.getState().trim());
        venue.setPostalCode(request.getPostalCode().trim());
        venue.setLocation(toPoint(request.getLatitude(), request.getLongitude()));
        venue.setLatitude(request.getLatitude());
        venue.setLongitude(request.getLongitude());
        venue.setTimezone(defaultValue(request.getTimezone(), "Asia/Kolkata"));
        venue.setStatus(defaultValue(request.getStatus(), "draft"));
    }

    private static Point toPoint(BigDecimal latitude, BigDecimal longitude) {
        Point point = GEOMETRY_FACTORY.createPoint(
                new Coordinate(longitude.doubleValue(), latitude.doubleValue())
        );
        point.setSRID(4326);
        return point;
    }

    private VenueResponseDTO toResponse(Venue venue) {
        Point location = venue.getLocation();
        BigDecimal latitude = location == null
                ? venue.getLatitude()
                : BigDecimal.valueOf(location.getY());
        BigDecimal longitude = location == null
                ? venue.getLongitude()
                : BigDecimal.valueOf(location.getX());

        return new VenueResponseDTO(
                venue.getId(),
                venue.getName(),
                venue.getDescription(),
                venue.getPhone(),
                venue.getEmail(),
                venue.getAddressLine1(),
                venue.getAddressLine2(),
                venue.getLocality(),
                venue.getCity(),
                venue.getState(),
                venue.getPostalCode(),
                latitude,
                longitude,
                venue.getTimezone(),
                venue.getStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultValue(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }
}
