package ru.just.userservice.config;

import org.testcontainers.containers.PostgreSQLContainer;

public class PostgresqlContainer extends PostgreSQLContainer<PostgresqlContainer> {
    private static final String IMAGE_VERSION = "postgres:15";
    private static PostgresqlContainer container;

    private PostgresqlContainer() {
        super(IMAGE_VERSION);
    }

    public static PostgresqlContainer getInstance() {
        if (container == null) {
            container = new PostgresqlContainer()
                    .withDatabaseName("users_service")
                    .withUsername("username")
                    .withPassword("password");
        }
        return container;
    }

    @Override
    public void start() {
        super.start();
        final String containerHost = container.getJdbcUrl()
                .replace("jdbc:postgresql://", "")
                .split("/")[0];
        System.setProperty("DB_CONTAINER", containerHost);
        System.setProperty("DB_USERNAME", container.getUsername());
        System.setProperty("DB_PASSWORD", container.getPassword());
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }
}
