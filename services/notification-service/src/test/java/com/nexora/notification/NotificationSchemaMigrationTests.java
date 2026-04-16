package com.nexora.notification;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.PostgreSQLContainer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NotificationSchemaMigrationTests {

  private static final String SCHEMA = "notification_service";

  private PostgreSQLContainer<?> postgres;
  private Connection connection;

  @BeforeAll
  void setUp() throws SQLException {
    configureDockerHostForWindows();
    postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    try {
      postgres.start();
    } catch (IllegalStateException exception) {
      Assumptions.assumeTrue(
          false,
          "Skipping notification schema migration validation because Docker/Testcontainers is unavailable: "
              + exception.getMessage());
      return;
    }

    Flyway.configure()
        .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
        .schemas(SCHEMA)
        .defaultSchema(SCHEMA)
        .createSchemas(true)
        .locations("classpath:db/migration", "classpath:db/seed")
        .load()
        .migrate();

    connection = DriverManager.getConnection(
        postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    try (Statement statement = connection.createStatement()) {
      statement.execute("SET search_path TO " + SCHEMA);
    }
  }

  @AfterAll
  void tearDown() throws Exception {
    if (connection != null) {
      connection.close();
    }
    if (postgres != null) {
      postgres.stop();
    }
  }

  @Test
  void createsExpectedTables() throws SQLException {
    assertThat(queryForLong(
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'notification_service'"))
        .isEqualTo(4);
  }

  @Test
  void seedsTemplatesPreferencesNotificationsAndAttempts() throws SQLException {
    assertThat(queryForLong("SELECT COUNT(*) FROM notification_templates"))
        .isEqualTo(2);
    assertThat(queryForString(
        "SELECT status FROM notifications WHERE id = '60000000-0000-0000-0000-000000000031'"))
        .isEqualTo("sent");
    assertThat(queryForLong("SELECT COUNT(*) FROM delivery_attempts"))
        .isEqualTo(1);
  }

  private long queryForLong(String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      assertThat(resultSet.next()).isTrue();
      return resultSet.getLong(1);
    }
  }

  private String queryForString(String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery(sql)) {
      assertThat(resultSet.next()).isTrue();
      return resultSet.getString(1);
    }
  }

  private void configureDockerHostForWindows() {
    String osName = System.getProperty("os.name", "").toLowerCase();
    if (osName.contains("win")
        && System.getProperty("docker.host") == null
        && System.getenv("DOCKER_HOST") == null) {
      System.setProperty("docker.host", "npipe:////./pipe/dockerDesktopLinuxEngine");
    }
  }
}
