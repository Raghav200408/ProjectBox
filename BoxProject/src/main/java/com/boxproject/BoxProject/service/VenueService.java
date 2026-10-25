package com.boxproject.BoxProject.service;

import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.entity.User;
import com.boxproject.BoxProject.entity.SportType;
import com.boxproject.BoxProject.entity.VenueSport;
import com.boxproject.BoxProject.entity.VenueMember;
import com.boxproject.BoxProject.entity.Amenity;
import com.boxproject.BoxProject.entity.VenueAmenity;
import com.boxproject.BoxProject.repository.UserRepository;
import com.boxproject.BoxProject.repository.SportTypeRepository;
import com.boxproject.BoxProject.repository.VenueSportRepository;
import com.boxproject.BoxProject.repository.VenueMemberRepository;
import com.boxproject.BoxProject.repository.AmenityRepository;
import com.boxproject.BoxProject.repository.VenueAmenityRepository;
import java.util.ArrayList;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
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

    private final UserRepository userRepository;
    private final VenueMemberRepository memberRepository;
    private final AmenityRepository amenityRepository;
    private final VenueAmenityRepository venueAmenityRepository;
    private final SportTypeRepository sportTypeRepository;
    private final VenueSportRepository venueSportRepository;

    public VenueService(VenueRepository venueRepository, UserRepository userRepository,
                        VenueMemberRepository memberRepository, AmenityRepository amenityRepository,
                        VenueAmenityRepository venueAmenityRepository, SportTypeRepository sportTypeRepository,
                        VenueSportRepository venueSportRepository) {
        this.venueRepository = venueRepository;
        this.userRepository = userRepository;
        this.memberRepository = memberRepository;
        this.amenityRepository = amenityRepository;
        this.venueAmenityRepository = venueAmenityRepository;
        this.sportTypeRepository = sportTypeRepository;
        this.venueSportRepository = venueSportRepository;
    }

    public VenueResponseDTO createVenue(VenueRequestDTO request) {
        User owner = userRepository.findByUserIdAndDeletedAtIsNull(request.getOwnerUserId())
                .filter(user -> "active".equals(user.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Owner must reference an existing active user."));
        List<SportType> sports = sportTypeRepository.findAllById(request.getSportTypeIds());
        if (sports.size() != request.getSportTypeIds().size() || sports.stream().anyMatch(sport -> !sport.isActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select existing active sports without duplicates.");
        }
        List<Long> amenityIds = request.getAmenityIds() == null ? List.of() : request.getAmenityIds();
        List<Amenity> amenities = amenityRepository.findAllById(amenityIds);
        if (amenities.size() != amenityIds.size() || amenities.stream().anyMatch(amenity -> !amenity.isActive())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select existing active amenities without duplicates.");
        }
        Venue venue = new Venue();
        applyRequest(venue, request);
        venue.setStatus("active");
        venueRepository.save(venue);
        List<VenueSport> sportAssignments = new ArrayList<>();
        for (SportType sport : sports) {
            VenueSport assignment = new VenueSport();
            assignment.setVenue(venue);
            assignment.setSportType(sport);
            sportAssignments.add(assignment);
        }
        venueSportRepository.saveAll(sportAssignments);

        VenueMember member = new VenueMember();
        member.setVenue(venue);
        member.setUser(owner);
        member.setMemberType("owner");
        member.setActive(true);
        memberRepository.save(member);

        List<VenueAmenity> assignments = new ArrayList<>();
        for (Amenity amenity : amenities) {
            VenueAmenity assignment = new VenueAmenity();
            assignment.setVenue(venue);
            assignment.setAmenity(amenity);
            assignments.add(assignment);
        }
        venueAmenityRepository.saveAll(assignments);
        return toResponse(venue);
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

    @Transactional(readOnly = true)
    public List<SportType> getSports() {
        return sportTypeRepository.findAllByActiveTrueOrderByNameAscIdAsc();
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
        venue.setLocationUrl(trimToNull(request.getLocationUrl()));
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

        VenueResponseDTO response = new VenueResponseDTO(
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
                venue.getLocationUrl(),
                latitude,
                longitude,
                venue.getTimezone(),
                venue.getStatus(),
                venue.getCreatedAt(),
                venue.getUpdatedAt()
        );
        response.setSportTypeIds(venueSportRepository.findAllByVenueIdOrderBySportTypeIdAsc(venue.getId())
                .stream().map(assignment -> assignment.getSportType().getId()).toList());
        response.setOwnerUserId(memberRepository.findAllByVenueIdAndActiveTrueOrderByCreatedAtAscIdAsc(venue.getId())
                .stream().filter(member -> "owner".equals(member.getMemberType()))
                .map(member -> member.getUser().getUserId()).findFirst().orElse(null));
        response.setAmenityIds(venueAmenityRepository.findAllByVenueIdOrderByAmenityNameAscAmenityIdAsc(venue.getId())
                .stream().map(assignment -> assignment.getAmenity().getId()).toList());
        return response;
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultValue(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }
}
