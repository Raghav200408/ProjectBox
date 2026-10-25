package com.boxproject.BoxProject.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.jpa.repository.Query;
import java.sql.DriverManager;
import java.util.ArrayList;
import static org.assertj.core.api.Assertions.assertThat;

// Optional real-PostGIS test. Only temporary tables are used; no application records are changed.
@EnabledIfEnvironmentVariable(named = "POSTGIS_TEST_URL", matches = ".+")
class NearbyVenuePostgisTests {
    @Test void radiusIncludesBoundaryAndExcludesOutsideInactiveAndDeletedVenues() throws Exception {
        try (var connection = DriverManager.getConnection(System.getenv("POSTGIS_TEST_URL"))) {
            connection.setAutoCommit(false);
            try (var sql = connection.createStatement()) {
                sql.execute("CREATE TEMP TABLE venues (id BIGINT PRIMARY KEY, status TEXT, deleted_at TIMESTAMPTZ, location GEOGRAPHY(POINT,4326)) ON COMMIT DROP");
                try (var insert = connection.prepareStatement("INSERT INTO venues VALUES (?, ?, ?, ST_Project('SRID=4326;POINT(0 0)'::geography, ?, 0))")) {
                    for (int id = 1; id <= 5; id++) {
                        insert.setLong(1, id); insert.setString(2, id == 4 ? "suspended" : "active");
                        insert.setTimestamp(3, id == 5 ? java.sql.Timestamp.from(java.time.Instant.now()) : null);
                        insert.setDouble(4, id == 1 ? 14999 : id == 2 ? 15000 : id == 3 ? 15001 : 100);
                        insert.executeUpdate();
                    }
                }
                // Execute the repository's actual native query, not a separate distance approximation.
                String query = VenueRepository.class.getMethod("findNearbyActive", double.class, double.class)
                        .getAnnotation(Query.class).value().replace(":longitude", "0").replace(":latitude", "0");
                var ids = new ArrayList<Long>();
                try (var result = sql.executeQuery(query)) { while (result.next()) ids.add(result.getLong("id")); }
                assertThat(ids).containsExactly(1L, 2L);
            } finally { connection.rollback(); }
        }
    }
}
