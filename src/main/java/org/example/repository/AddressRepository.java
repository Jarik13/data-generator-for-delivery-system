package org.example.repository;

import org.example.config.DatabaseConfig;
import java.sql.*;
import java.util.Random;

public class AddressRepository {
    private final Random random;
    private static final int BATCH_SIZE = 50000;

    public AddressRepository(Random random) {
        this.random = random;
    }

    public int[] getHouseIdRange() {
        int[] range = {0, 0};
        String sql = "SELECT MIN(address_house_id), MAX(address_house_id) FROM address_houses";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                range[0] = rs.getInt(1);
                range[1] = rs.getInt(2);
            }
        } catch (SQLException e) {
            System.err.println("Помилка при отриманні діапазону ID будинків: " + e.getMessage());
        }
        return range;
    }

    public void saveAddresses(int minHouseId, int maxHouseId) {
        if (minHouseId == 0 && maxHouseId == 0) return;

        System.out.println("--- Генерація повних адрес клієнтів (квартир) ---");
        String sql = "INSERT INTO addresses (address_house_id, apartment_number) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            long totalCount = 0;
            int currentBatch = 0;

            for (int houseId = minHouseId; houseId <= maxHouseId; houseId++) {
                if (random.nextDouble() < 0.7) {
                    int apts = 2 + random.nextInt(4);
                    for (int i = 1; i <= apts; i++) {
                        ps.setInt(1, houseId);
                        ps.setInt(2, i);
                        ps.addBatch();
                        currentBatch++;
                        totalCount++;
                    }
                } else {
                    ps.setInt(1, houseId);
                    ps.setNull(2, Types.INTEGER);
                    ps.addBatch();
                    currentBatch++;
                    totalCount++;
                }

                if (currentBatch >= BATCH_SIZE) {
                    ps.executeBatch();
                    conn.commit();
                    currentBatch = 0;
                    System.out.printf("Збережено адрес: %,d\n", totalCount);
                }
            }

            ps.executeBatch();
            conn.commit();
            System.out.println("Генерацію адрес завершено. Всього: " + totalCount);
        } catch (SQLException e) {
            System.err.println("Критична помилка при збереженні адрес: " + e.getMessage());
        }
    }
}