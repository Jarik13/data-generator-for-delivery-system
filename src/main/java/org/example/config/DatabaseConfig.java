package org.example.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final String DB_URL = "jdbc:sqlserver://DESKTOP-PO7GJL8:1434;databaseName=parcels_db;encrypt=false;trustServerCertificate=true;";
    private static final String USER = "username";
    private static final String PASSWORD = "password";

    private static final HikariDataSource dataSource;

    static {
        try {
            HikariConfig config = new HikariConfig();

            config.setJdbcUrl(DB_URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);
            config.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            dataSource = new HikariDataSource(config);

        } catch (Exception e) {
            throw new RuntimeException("Помилка ініціалізації пулу з'єднань з БД", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}