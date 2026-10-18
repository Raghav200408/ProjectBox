package com.boxproject.BoxProject.repository;

import com.boxproject.BoxProject.entity.User;
import com.boxproject.BoxProject.entity.Venue;
import com.boxproject.BoxProject.entity.VenueMember;
import com.boxproject.BoxProject.model.VenueMemberRequestDTO;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class VenueMemberRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, venue_id, user_id, member_type,
                   is_active, created_at, updated_at
            FROM venue_members
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public VenueMemberRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public VenueMember create(Long venueId, VenueMemberRequestDTO request) {
        String sql = """
                INSERT INTO venue_members (
                    venue_id, user_id, member_type, is_active
                ) VALUES (
                    :venueId, :userId, :memberType, :active
                )
                RETURNING id, venue_id, user_id, member_type,
                          is_active, created_at, updated_at
                """;
        return jdbc.queryForObject(sql, parameters(venueId, request), rowMapper());
    }

    public Optional<VenueMember> findById(Long venueId, Long memberId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("venueId", venueId)
                .addValue("memberId", memberId);
        return jdbc.query(
                SELECT_COLUMNS + " WHERE venue_id = :venueId AND id = :memberId",
                parameters,
                rowMapper()
        ).stream().findFirst();
    }

    public List<VenueMember> findAll(Long venueId, boolean activeOnly) {
        String sql = SELECT_COLUMNS + " WHERE venue_id = :venueId"
                + (activeOnly ? " AND is_active = TRUE" : "")
                + " ORDER BY created_at, id";
        return jdbc.query(
                sql,
                new MapSqlParameterSource("venueId", venueId),
                rowMapper()
        );
    }

    public Optional<VenueMember> update(
            Long venueId,
            Long memberId,
            VenueMemberRequestDTO request
    ) {
        String sql = """
                UPDATE venue_members
                SET user_id = :userId,
                    member_type = :memberType,
                    is_active = :active,
                    updated_at = NOW()
                WHERE venue_id = :venueId AND id = :memberId
                RETURNING id, venue_id, user_id, member_type,
                          is_active, created_at, updated_at
                """;
        return jdbc.query(
                sql,
                parameters(venueId, request).addValue("memberId", memberId),
                rowMapper()
        ).stream().findFirst();
    }

    public void deactivate(Long venueId, Long memberId) {
        jdbc.update("""
                UPDATE venue_members
                SET is_active = FALSE, updated_at = NOW()
                WHERE venue_id = :venueId AND id = :memberId
                """, new MapSqlParameterSource()
                .addValue("venueId", venueId)
                .addValue("memberId", memberId));
    }

    private MapSqlParameterSource parameters(
            Long venueId,
            VenueMemberRequestDTO request
    ) {
        return new MapSqlParameterSource()
                .addValue("venueId", venueId)
                .addValue("userId", request.getUserId())
                .addValue("memberType", request.getMemberType())
                .addValue("active", request.getActive() == null || request.getActive());
    }

    private RowMapper<VenueMember> rowMapper() {
        return (rs, rowNum) -> {
            Venue venue = new Venue();
            venue.setId(rs.getLong("venue_id"));

            User user = new User();
            user.setUserId(rs.getLong("user_id"));

            VenueMember member = new VenueMember();
            member.setId(rs.getLong("id"));
            member.setVenue(venue);
            member.setUser(user);
            member.setMemberType(rs.getString("member_type"));
            member.setActive(rs.getBoolean("is_active"));
            member.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            member.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return member;
        };
    }
}
