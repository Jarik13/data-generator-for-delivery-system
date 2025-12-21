package org.example.repository;

import org.example.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class AddressHouseRepository {
    public void saveHouses(Map<Integer, List<Integer>> cityStreetMap, int housesPerStreetMin, int housesPerStreetMax) {
        System.out.println("--- Генерація та збереження номерів будинків ---");
        String sql = "INSERT INTO address_houses (address_house_number, street_id) VALUES (?, ?)";

        Random random = new Random();
        int totalGenerated = 0;
        int streetCount = 0;

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (Map.Entry<Integer, List<Integer>> entry : cityStreetMap.entrySet()) {
                Integer cityId = entry.getKey();
                List<Integer> streetIds = entry.getValue();

                System.out.printf("Місто %d: %d вулиць\n", cityId, streetIds.size());

                for (Integer streetId : streetIds) {
                    streetCount++;

                    int houseCount = housesPerStreetMin + random.nextInt(housesPerStreetMax - housesPerStreetMin + 1);

                    Set<String> generatedNumbers = new HashSet<>();

                    for (int i = 0; i < houseCount; i++) {
                        String houseNumber;
                        int attempts = 0;

                        do {
                            int num = 1 + random.nextInt(300);

                            if (random.nextDouble() < 0.3) {
                                char letter = (char) ('А' + random.nextInt(33));
                                houseNumber = num + "" + letter;
                            } else {
                                houseNumber = String.valueOf(num);
                            }

                            attempts++;
                        } while (generatedNumbers.contains(houseNumber) && attempts < 100);

                        generatedNumbers.add(houseNumber);

                        ps.setString(1, houseNumber);
                        ps.setInt(2, streetId);
                        ps.addBatch();
                        totalGenerated++;

                        if (totalGenerated % 10000 == 0) {
                            ps.executeBatch();
                            conn.commit();
                            System.out.printf("Згенеровано %,d будинків...\n", totalGenerated);
                        }
                    }

                    if (streetCount % 1000 == 0) {
                        System.out.printf("Оброблено %,d вулиць\n", streetCount);
                    }
                }
            }

            if (totalGenerated % 10000 != 0) {
                ps.executeBatch();
                conn.commit();
            }

            System.out.printf("Загалом згенеровано будинків: %,d\n", totalGenerated);
            System.out.printf("Оброблено вулиць: %,d\n", streetCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, List<Integer>> getStreetHouseMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT street_id, address_house_id FROM address_houses ORDER BY street_id, address_house_id")) {
            while (rs.next()) {
                int streetId = rs.getInt("street_id");
                int houseId = rs.getInt("address_house_id");

                map.computeIfAbsent(streetId, k -> new ArrayList<>()).add(houseId);
            }
            System.out.printf("Завантажено мапу вулиця->будинки: %,d вулиць\n", map.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}