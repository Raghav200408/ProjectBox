package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.Amenity;
import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.entity.VenueAmenity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VenueAmenityRepository {

    private static final String SELECT_COLUMNS = """
            SELECT va.id, va.venue_id, va.amenity_id,
                   a.name, a.is_active
            FROM venue_amenities va
            JOIN amenities a ON a.id = va.amenity_id
            """;

    private static final RowMapper<VenueAmenity> VENUE_AMENITY_MAPPER = (rs, rowNum) -> {
        Venue venue = new Venue();
        venue.setId(rs.getLong("venue_id"));

        Amenity amenity = new Amenity();
        amenity.setId(rs.getLong("amenity_id"));
        amenity.setName(rs.getString("name"));
        amenity.setActive(rs.getBoolean("is_active"));

        VenueAmenity venueAmenity = new VenueAmenity();
        venueAmenity.setId(rs.getLong("id"));
        venueAmenity.setVenue(venue);
        venueAmenity.setAmenity(amenity);
        return venueAmenity;
    };

    private final NamedParameterJdbcTemplate jdbc;

    public VenueAmenityRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void create(Long venueId, Long amenityId) {
        jdbc.update("""
                INSERT INTO venue_amenities (venue_id, amenity_id)
                VALUES (:venueId, :amenityId)
                """, parameters(venueId, amenityId));
    }

    public Optional<VenueAmenity> findById(Long venueId, Long amenityId) {
        return jdbc.query(
                SELECT_COLUMNS + " WHERE va.venue_id = :venueId AND va.amenity_id = :amenityId",
                parameters(venueId, amenityId),
                VENUE_AMENITY_MAPPER
        ).stream().findFirst();
    }

    public List<VenueAmenity> findAll(Long venueId, boolean activeOnly) {
        String sql = SELECT_COLUMNS + " WHERE va.venue_id = :venueId"
                + (activeOnly ? " AND a.is_active = TRUE" : "")
                + " ORDER BY a.name, a.id";
        return jdbc.query(
                sql,
                new MapSqlParameterSource("venueId", venueId),
                VENUE_AMENITY_MAPPER
        );
    }

    public boolean delete(Long venueId, Long amenityId) {
        return jdbc.update("""
                DELETE FROM venue_amenities
                WHERE venue_id = :venueId AND amenity_id = :amenityId
                """, parameters(venueId, amenityId)) == 1;
    }

    private static MapSqlParameterSource parameters(Long venueId, Long amenityId) {
        return new MapSqlParameterSource()
                .addValue("venueId", venueId)
                .addValue("amenityId", amenityId);
    }
}
