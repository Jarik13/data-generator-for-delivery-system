package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.model.item.StorageConditionItem;
import org.example.model.item.WorkTimeIntervalItem;
import org.example.storage.DataStorage;

import java.sql.*;
import java.util.List;

public class StaticDataRepository {
    public void saveAll() {
        System.out.println("--- ПОЧАТОК ЗАВАНТАЖЕННЯ СТАТИЧНИХ ДОВІДНИКІВ ---");

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try {
                saveDictionary(conn, "shipment_types", "shipment_type_name", DataStorage.SHIPMENT_TYPES);
                saveDictionary(conn, "shipment_statuses", "shipment_status_name", DataStorage.SHIPMENT_STATUSES);
                saveDictionary(conn, "payment_types", "payment_type_name", DataStorage.PAYMENT_TYPES);
                saveDictionary(conn, "return_reasons", "return_reason_name", DataStorage.RETURN_REASONS);

                saveStorageConditions(conn);
                saveWorkTimeIntervals(conn);

                saveDictionary(conn, "branch_types", "branch_type_name", DataStorage.BRANCH_TYPES);
                saveDictionary(conn, "days_of_week", "days_of_week_name", DataStorage.DAYS_OF_WEEK);

                saveDictionary(conn, "route_list_statuses", "route_list_status_name", DataStorage.ROUTE_LIST_STATUSES);
                saveDictionary(conn, "trip_statuses", "trip_status_name", DataStorage.TRIP_STATUSES);
                saveDictionary(conn, "vehicle_activity_statuses", "vehicle_activity_status_name", DataStorage.VEHICLE_ACTIVITY_STATUSES);

                saveDictionary(conn, "fleet_brands", "fleet_brand_name", DataStorage.FLEET_BRANDS);
                saveDictionary(conn, "fleet_fuel_types", "fleet_fuel_type_name", DataStorage.FLEET_FUEL_TYPES);
                saveDictionary(conn, "fleet_body_types", "fleet_body_type_name", DataStorage.FLEET_BODY_TYPES);
                saveDictionary(conn, "fleet_transmission_types", "fleet_transmission_type_name", DataStorage.FLEET_TRANSMISSION_TYPES);
                saveDictionary(conn, "fleet_drive_types", "fleet_drive_type_name", DataStorage.FLEET_DRIVE_TYPES);

                conn.commit();
                System.out.println("--- ВСІ ДОВІДНИКИ УСПІШНО ЗБЕРЕЖЕНО ---");
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveDictionary(Connection conn, String tableName, String columnName, List<String> values) throws SQLException {
        String checkSql = "SELECT 1 FROM " + tableName + " WHERE " + columnName + " = ?";
        String insertSql = "INSERT INTO " + tableName + " (" + columnName + ") VALUES (?)";

        try (PreparedStatement psCheck = conn.prepareStatement(checkSql);
             PreparedStatement psInsert = conn.prepareStatement(insertSql)) {

            int count = 0;
            for (String value : values) {
                psCheck.setString(1, value);
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        psInsert.setString(1, value);
                        psInsert.addBatch();
                        count++;
                    }
                }
            }
            psInsert.executeBatch();
            System.out.printf("Таблиця %s: додано %d записів.%n", tableName, count);
        }
    }

    private void saveStorageConditions(Connection conn) throws SQLException {
        String tableName = "storage_conditions";
        String checkSql = "SELECT 1 FROM " + tableName + " WHERE storage_condition_name = ?";
        String insertSql = "INSERT INTO " + tableName + " (storage_condition_name, storage_condition_description) VALUES (?, ?)";

        try (PreparedStatement psCheck = conn.prepareStatement(checkSql);
             PreparedStatement psInsert = conn.prepareStatement(insertSql)) {

            int count = 0;
            for (StorageConditionItem item : DataStorage.STORAGE_CONDITIONS) {
                psCheck.setString(1, item.name());
                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        psInsert.setString(1, item.name());
                        psInsert.setString(2, item.description());
                        psInsert.addBatch();
                        count++;
                    }
                }
            }
            psInsert.executeBatch();
            System.out.printf("Таблиця %s: додано %d записів.%n", tableName, count);
        }
    }

    private void saveWorkTimeIntervals(Connection conn) throws SQLException {
        String tableName = "work_time_intervals";
        String checkSql = "SELECT 1 FROM " + tableName + " WHERE work_time_interval_start_time = ? AND work_time_interval_end_time = ?";
        String insertSql = "INSERT INTO " + tableName + " (work_time_interval_start_time, work_time_interval_end_time) VALUES (?, ?)";

        try (PreparedStatement psCheck = conn.prepareStatement(checkSql);
             PreparedStatement psInsert = conn.prepareStatement(insertSql)) {

            int count = 0;
            for (WorkTimeIntervalItem item : DataStorage.WORK_TIME_INTERVALS) {
                psCheck.setString(1, item.start());
                psCheck.setString(2, item.end());

                try (ResultSet rs = psCheck.executeQuery()) {
                    if (!rs.next()) {
                        psInsert.setString(1, item.start());
                        psInsert.setString(2, item.end());
                        psInsert.addBatch();
                        count++;
                    }
                }
            }
            psInsert.executeBatch();
            System.out.printf("Таблиця %s: додано %d записів.%n", tableName, count);
        }
    }
}