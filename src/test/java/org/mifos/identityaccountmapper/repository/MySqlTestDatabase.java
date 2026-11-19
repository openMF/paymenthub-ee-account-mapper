package org.mifos.identityaccountmapper.repository;

import org.testcontainers.containers.MySQLContainer;

/**
 * One MySQL container for every test class that needs the real schema. Started on first use and stopped by
 * Testcontainers when the JVM exits. Test classes using it carry {@code @Testcontainers(disabledWithoutDocker = true)}.
 */
final class MySqlTestDatabase {

    static final String ROOT_PASSWORD = "mysql";

    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0").withUsername("root").withPassword(ROOT_PASSWORD);

    private MySqlTestDatabase() {}

    static synchronized MySQLContainer<?> get() {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
        return MYSQL;
    }

    /** JDBC URL of another, empty database in the same container. */
    static String urlFor(String database) {
        MySQLContainer<?> mysql = get();
        return "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(MySQLContainer.MYSQL_PORT) + "/" + database
                + "?createDatabaseIfNotExist=true";
    }
}
