package org.example.repository;

import lombok.extern.slf4j.Slf4j;
import org.example.config.DatabaseConfig;
import org.example.manager.helper.FakeGenerator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class PersonRepository {
    private static final int BATCH_SIZE = 5000;

    private static final List<String> CLIENT_CODES = List.of("50", "66", "95", "99", "67", "68", "96", "97", "98", "63", "93", "73");
    private static final List<String> EMPLOYEE_CODES = List.of("67", "68", "50", "66");
    private static final List<String> DRIVER_CODES = List.of("63", "93", "73", "91");
    private static final List<String> COURIER_CODES = List.of("99", "95", "98", "93");

    public List<Integer> getAllBranchIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT branch_id FROM branches";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) {
            log.error("Помилка отримання ID відділень", e);
        }
        return ids;
    }

    public void saveClients(int count) {
        log.info("Початок генерації {} клієнтів...", count);
        String sql = "INSERT INTO clients (client_first_name, client_last_name, client_middle_name, client_phone_number, client_email) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int batchCounter = 0;
            int totalSaved = 0;

            for (int i = 0; i < count; i++) {
                FakeGenerator.PersonName person = FakeGenerator.generateName();

                ps.setString(1, person.firstName());
                ps.setString(2, person.lastName());
                ps.setString(3, person.middleName());
                ps.setString(4, FakeGenerator.generateVariedPhone(i, CLIENT_CODES));
                ps.setString(5, FakeGenerator.generateEmail(person.firstName(), person.lastName(), i));

                ps.addBatch();
                batchCounter++;

                if (batchCounter % BATCH_SIZE == 0 || i == count - 1) {
                    int[] batchResults = ps.executeBatch();
                    conn.commit();
                    totalSaved += batchResults.length;
                }
            }

            log.info("Збережено {} клієнтів", totalSaved);
        } catch (SQLException e) {
            log.error("Помилка генерації клієнтів", e);
            e.printStackTrace();
        }
    }

    public void saveEmployees(int count, List<Integer> branchIds, int startIndex) {
        if (branchIds.isEmpty()) {
            log.error("Немає відділень для прив'язки співробітників!");
            return;
        }

        log.info("Початок генерації {} співробітників...", count);
        String sql = "INSERT INTO employees (employee_first_name, employee_last_name, employee_middle_name, employee_phone_number, branch_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int batchCounter = 0;
            int totalSaved = 0;

            for (int i = 0; i < count; i++) {
                FakeGenerator.PersonName person = FakeGenerator.generateName();

                ps.setString(1, person.firstName());
                ps.setString(2, person.lastName());
                ps.setString(3, person.middleName());
                ps.setString(4, FakeGenerator.generateVariedPhone(startIndex + i, EMPLOYEE_CODES));

                int branchId = branchIds.get(i % branchIds.size());
                ps.setInt(5, branchId);

                ps.addBatch();
                batchCounter++;

                if (batchCounter % BATCH_SIZE == 0 || i == count - 1) {
                    int[] batchResults = ps.executeBatch();
                    conn.commit();
                    totalSaved += batchResults.length;
                }
            }

            log.info("Збережено {} працівників", totalSaved);
        } catch (SQLException e) {
            log.error("Помилка генерації співробітників", e);
            e.printStackTrace();
        }
    }

    public void saveDrivers(int count) {
        log.info("Початок генерації {} водіїв...", count);
        String sql = "INSERT INTO drivers (driver_first_name, driver_last_name, driver_middle_name, driver_phone_number, driver_license_number) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int batchCounter = 0;
            int totalSaved = 0;

            for (int i = 0; i < count; i++) {
                FakeGenerator.PersonName person = FakeGenerator.generateName();

                ps.setString(1, person.firstName());
                ps.setString(2, person.lastName());
                ps.setString(3, person.middleName());
                ps.setString(4, FakeGenerator.generateVariedPhone(i, DRIVER_CODES));
                ps.setString(5, FakeGenerator.generateUniqueLicense(i));

                ps.addBatch();
                batchCounter++;

                if (batchCounter % BATCH_SIZE == 0 || i == count - 1) {
                    int[] batchResults = ps.executeBatch();
                    conn.commit();
                    totalSaved += batchResults.length;
                }
            }

            log.info("Збережено {} водіїв", totalSaved);
        } catch (SQLException e) {
            log.error("Помилка генерації водіїв", e);
            e.printStackTrace();
        }
    }

    public void saveCouriers(int count) {
        log.info("Початок генерації {} кур'єрів...", count);
        String sql = "INSERT INTO couriers (courier_first_name, courier_last_name, courier_middle_name, courier_phone_number) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int batchCounter = 0;
            int totalSaved = 0;

            for (int i = 0; i < count; i++) {
                FakeGenerator.PersonName person = FakeGenerator.generateName();

                ps.setString(1, person.firstName());
                ps.setString(2, person.lastName());
                ps.setString(3, person.middleName());
                ps.setString(4, FakeGenerator.generateVariedPhone(i, COURIER_CODES));

                ps.addBatch();
                batchCounter++;

                if (batchCounter % BATCH_SIZE == 0 || i == count - 1) {
                    int[] batchResults = ps.executeBatch();
                    conn.commit();
                    totalSaved += batchResults.length;
                }
            }

            log.info("Збережено {} кур'єрів", totalSaved);
        } catch (SQLException e) {
            log.error("Помилка генерації кур'єрів", e);
            e.printStackTrace();
        }
    }
}