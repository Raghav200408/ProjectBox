package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.model.VenueRequestDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class VenueRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, name, description, phone, email, address_line_1, address_line_2,
                   locality, city, state, postal_code,
                   ST_Y(location::geometry) AS latitude,
                   ST_X(location::geometry) AS longitude,
                   timezone, status, created_at, updated_at
            FROM venues
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public VenueRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Venue create(VenueRequestDTO request) {
        String sql = """
                INSERT INTO venues (
                    name, description, phone, email, address_line_1, address_line_2,
                    locality, city, state, postal_code, location, timezone, status
                ) VALUES (
                    :name, :description, :phone, :email, :addressLine1, :addressLine2,
                    :locality, :city, :state, :postalCode,
                    ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    :timezone, :status
                )
                RETURNING id, name, description, phone, email, address_line_1, address_line_2,
                          locality, city, state, postal_code,
                          ST_Y(location::geometry) AS latitude,
                          ST_X(location::geometry) AS longitude,
                          timezone, status, created_at, updated_at
                """;
        return jdbc.queryForObject(sql, parameters(request), VENUE_MAPPER);
    }

    public Optional<Venue> findActiveById(Long venueId) {
        return jdbc.query(
                SELECT_COLUMNS + " WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", venueId),
                VENUE_MAPPER
        ).stream().findFirst();
    }

    public boolean existsActiveById(Long venueId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM venues WHERE id = :id AND deleted_at IS NULL",
                new MapSqlParameterSource("id", venueId),
                Long.class
        );
        return count != null && count > 0;
    }

    public List<Venue> findAll(
            String city,
            String status
    ) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS).append(" WHERE deleted_at IS NULL");
        MapSqlParameterSource parameters = filterParameters(city, status);
        appendFilters(sql, city, status);
        sql.append(" ORDER BY created_at DESC, id DESC");
        return jdbc.query(sql.toString(), parameters, VENUE_MAPPER);
    }

    public Optional<Venue> update(Long venueId, VenueRequestDTO request) {
        String sql = """
                UPDATE venues
                SET name = :name,
                    description = :description,
                    phone = :phone,
                    email = :email,
                    address_line_1 = :addressLine1,
                    address_line_2 = :addressLine2,
                    locality = :locality,
                    city = :city,
                    state = :state,
                    postal_code = :postalCode,
                    location = ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography,
                    timezone = :timezone,
                    status = :status,
                    updated_at = NOW()
                WHERE id = :id AND deleted_at IS NULL
                RETURNING id, name, description, phone, email, address_line_1, address_line_2,
                          locality, city, state, postal_code,
                          ST_Y(location::geometry) AS latitude,
                          ST_X(location::geometry) AS longitude,
                          timezone, status, created_at, updated_at
                """;
        return jdbc.query(
                sql,
                parameters(request).addValue("id", venueId),
                VENUE_MAPPER
        ).stream().findFirst();
    }

    public boolean softDelete(Long venueId) {
        return jdbc.update("""
                UPDATE venues
                SET status = 'closed', deleted_at = NOW(), updated_at = NOW()
                WHERE id = :id AND deleted_at IS NULL
                """, new MapSqlParameterSource("id", venueId)) == 1;
    }

    private static MapSqlParameterSource parameters(VenueRequestDTO request) {
        return new MapSqlParameterSource()
                .addValue("name", request.getName().trim())
                .addValue("description", trimToNull(request.getDescription()))
                .addValue("phone", request.getPhone().trim())
                .addValue("email", trimToNull(request.getEmail()))
                .addValue("addressLine1", request.getAddressLine1().trim())
                .addValue("addressLine2", trimToNull(request.getAddressLine2()))
                .addValue("locality", request.getLocality().trim())
                .addValue("city", request.getCity().trim())
                .addValue("state", request.getState().trim())
                .addValue("postalCode", request.getPostalCode().trim())
                .addValue("latitude", request.getLatitude())
                .addValue("longitude", request.getLongitude())
                .addValue("timezone", defaultValue(request.getTimezone(), "Asia/Kolkata"))
                .addValue("status", defaultValue(request.getStatus(), "draft"));
    }

    private static MapSqlParameterSource filterParameters(String city, String status) {
        return new MapSqlParameterSource()
                .addValue("city", trimToNull(city))
                .addValue("status", trimToNull(status));
    }

    private static void appendFilters(StringBuilder sql, String city, String status) {
        if (city != null && !city.isBlank()) {
            sql.append(" AND LOWER(city) = LOWER(:city)");
        }
        if (status != null && !status.isBlank()) {
            sql.append(" AND status = :status");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String defaultValue(String value, String defaultValue) {
        String normalized = trimToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static final RowMapper<Venue> VENUE_MAPPER = new RowMapper<>() {
        @Override
        public Venue mapRow(ResultSet rs, int rowNum) throws SQLException {
            Venue venue = new Venue();
            venue.setId(rs.getLong("id"));
            venue.setName(rs.getString("name"));
            venue.setDescription(rs.getString("description"));
            venue.setPhone(rs.getString("phone"));
            venue.setEmail(rs.getString("email"));
            venue.setAddressLine1(rs.getString("address_line_1"));
            venue.setAddressLine2(rs.getString("address_line_2"));
            venue.setLocality(rs.getString("locality"));
            venue.setCity(rs.getString("city"));
            venue.setState(rs.getString("state"));
            venue.setPostalCode(rs.getString("postal_code"));
            venue.setLatitude(rs.getBigDecimal("latitude"));
            venue.setLongitude(rs.getBigDecimal("longitude"));
            venue.setTimezone(rs.getString("timezone"));
            venue.setStatus(rs.getString("status"));
            venue.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            venue.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return venue;
        }
    };
}
