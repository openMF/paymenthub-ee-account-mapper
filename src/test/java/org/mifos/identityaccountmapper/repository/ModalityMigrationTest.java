package org.mifos.identityaccountmapper.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * PHEE-474: the V3 migration turns the BIGINT modalities already in the database back into two-character codes.
 */
@Testcontainers(disabledWithoutDocker = true)
class ModalityMigrationTest {

    private static final String ROOT = "root";

    @Test
    void existingRowsGetTheirLeadingZeroBackAndNullsStayNull() throws SQLException {
        String url = MySqlTestDatabase.urlFor("upgrade");
        migrate(url, "2");
        try (Connection connection = DriverManager.getConnection(url, ROOT, MySqlTestDatabase.ROOT_PASSWORD);
                Statement statement = connection.createStatement()) {
            // what V1/V2 stored for "00", "01", "04" and an identity without a modality
            statement.executeUpdate("INSERT INTO payment_modality_details (master_id, modality) VALUES"
                    + " ('m-00', 0), ('m-01', 1), ('m-04', 4), ('m-null', NULL)");
            statement.executeUpdate("INSERT INTO error_tracking (request_id, payee_identity, modality, error_description) VALUES"
                    + " ('r', 'p-00', 0, 'e'), ('r', 'p-99', 99, 'e'), ('r', 'p-null', NULL, 'e')");
        }

        migrate(url, "latest");

        try (Connection connection = DriverManager.getConnection(url, ROOT, MySqlTestDatabase.ROOT_PASSWORD);
                Statement statement = connection.createStatement()) {
            assertThat(modalities(statement, "SELECT master_id, modality FROM payment_modality_details"))
                    .containsExactlyInAnyOrderEntriesOf(Map.of("m-00", "00", "m-01", "01", "m-04", "04", "m-null", "<null>"));
            assertThat(modalities(statement, "SELECT payee_identity, modality FROM error_tracking"))
                    .containsExactlyInAnyOrderEntriesOf(Map.of("p-00", "00", "p-99", "99", "p-null", "<null>"));
        }
    }

    private static void migrate(String url, String target) {
        Flyway.configure().dataSource(url, ROOT, MySqlTestDatabase.ROOT_PASSWORD).target(target).load().migrate();
    }

    private static Map<String, String> modalities(Statement statement, String query) throws SQLException {
        Map<String, String> byKey = new LinkedHashMap<>();
        try (ResultSet rows = statement.executeQuery(query)) {
            while (rows.next()) {
                String modality = rows.getString(2);
                byKey.put(rows.getString(1), modality == null ? "<null>" : modality);
            }
        }
        return byKey;
    }
}
