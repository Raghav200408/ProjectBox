package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.Amenity;
import com.boxproject.BoxProject.model.AmenityRequestDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class AmenityRepository {

    private static final String SELECT_COLUMNS =
            "SELECT id, name, is_active FROM amenities";

    private static final RowMapper<Amenity> AMENITY_MAPPER = (rs, rowNum) -> {
        Amenity amenity = new Amenity();
        amenity.setId(rs.getLong("id"));
        amenity.setName(rs.getString("name"));
        amenity.setActive(rs.getBoolean("is_active"));
        return amenity;
    };

    private final NamedParameterJdbcTemplate jdbc;

    public AmenityRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Amenity create(AmenityRequestDTO request) {
        return jdbc.queryForObject("""
                INSERT INTO amenities (name, is_active)
                VALUES (:name, :active)
                RETURNING id, name, is_active
                """, parameters(request), AMENITY_MAPPER);
    }

    public Optional<Amenity> findById(Long amenityId) {
        return jdbc.query(
                SELECT_COLUMNS + " WHERE id = :id",
                new MapSqlParameterSource("id", amenityId),
                AMENITY_MAPPER
        ).stream().findFirst();
    }

    public Optional<Amenity> findActiveById(Long amenityId) {
        return jdbc.query(
                SELECT_COLUMNS + " WHERE id = :id AND is_active = TRUE",
                new MapSqlParameterSource("id", amenityId),
                AMENITY_MAPPER
        ).stream().findFirst();
    }

    public List<Amenity> findAll(boolean activeOnly) {
        String sql = SELECT_COLUMNS
                + (activeOnly ? " WHERE is_active = TRUE" : "")
                + " ORDER BY name, id";
        return jdbc.query(sql, AMENITY_MAPPER);
    }

    public Optional<Amenity> update(Long amenityId, AmenityRequestDTO request) {
        return jdbc.query("""
                UPDATE amenities
                SET name = :name,
                    is_active = :active
                WHERE id = :id
                RETURNING id, name, is_active
                """, parameters(request).addValue("id", amenityId), AMENITY_MAPPER)
                .stream()
                .findFirst();
    }

    public boolean deactivate(Long amenityId) {
        return jdbc.update("""
                UPDATE amenities
                SET is_active = FALSE
                WHERE id = :id
                """, new MapSqlParameterSource("id", amenityId)) == 1;
    }

    private static MapSqlParameterSource parameters(AmenityRequestDTO request) {
        return new MapSqlParameterSource()
                .addValue("name", request.getName().trim())
                .addValue("active", request.getActive() == null || request.getActive());
    }
}
