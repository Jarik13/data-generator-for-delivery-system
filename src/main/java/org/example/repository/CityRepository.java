package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedCity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class CityRepository {
    public void saveCities(List<ParsedCity> cities, Map<String, Integer> districtMap) {
        System.out.println("--- Збереження населених пунктів у БД ---");
        String sql = "INSERT INTO cities (city_name, district_id) VALUES (?, ?)";

        Set<String> processedKeys = new HashSet<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int count = 0;
            int skipped = 0;

            for (ParsedCity city : cities) {
                String regionName = city.getRegion();
                String rawDistrict = city.getArea();

                if (rawDistrict == null || rawDistrict.isEmpty()) {
                    skipped++;
                    continue;
                }

                String searchDistrict = rawDistrict.trim();
                if (searchDistrict.endsWith("а")) {
                    searchDistrict = searchDistrict.substring(0, searchDistrict.length() - 1) + "ий";
                } else if (searchDistrict.endsWith("е")) {
                    searchDistrict = searchDistrict.substring(0, searchDistrict.length() - 1) + "ий";
                }

                searchDistrict = searchDistrict.replace(" район", "").replace(" р-н", "").trim();

                String key = regionName + "_" + searchDistrict;
                Integer districtId = districtMap.get(key);

                if (districtId != null) {
                    String fullName = city.getType() + " " + city.getDescription();

                    String dupKey = districtId + "_" + fullName;
                    if (processedKeys.contains(dupKey)) continue;
                    processedKeys.add(dupKey);

                    ps.setString(1, fullName);
                    ps.setInt(2, districtId);
                    ps.addBatch();
                    count++;
                } else {
                    skipped++;
                }

                if (count > 0 && count % 5000 == 0) {
                    ps.executeBatch();
                    conn.commit();
                    System.out.print(".");
                }
            }

            ps.executeBatch();
            conn.commit();

            System.out.println("\nЗбережено міст: " + count + ". Пропущено (не знайдено район): " + skipped);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getCityNameIdMap() {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT city_id, city_name FROM cities")) {
            while (rs.next()) {
                map.put(rs.getString("city_name"), rs.getInt("city_id"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }
}