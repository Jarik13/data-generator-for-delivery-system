package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedCity;
import java.sql.*;
import java.util.*;

public class CityRepository {
    public static String prepareDistrictName(String rawName) {
        if (rawName == null || rawName.isBlank()) return "";
        String name = rawName.trim();
        if (name.endsWith("а")) name = name.substring(0, name.length() - 1) + "ий";
        else if (name.endsWith("е")) name = name.substring(0, name.length() - 1) + "ий";

        name = name.replaceAll("(?i)\\s*(р-н|район)$", "").trim();
        return name + " район";
    }

    public Map<Integer, String> getAllCityDataFromDb() {
        Map<Integer, String> cityData = new HashMap<>();
        String sql = "SELECT city_id, city_name FROM cities";
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cityData.put(rs.getInt("city_id"), rs.getString("city_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return cityData;
    }

    public void saveCities(List<ParsedCity> cities, Map<String, Integer> districtMap) {
        System.out.println("--- Збереження населених пунктів у БД ---");
        String sql = "INSERT INTO cities (city_name, district_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int count = 0;

            for (ParsedCity city : cities) {
                String regionName = city.getRegion();
                String rawDistrict = city.getArea();
                String districtName = (rawDistrict == null || rawDistrict.isBlank()) ? city.getDescription() : rawDistrict;
                String normalizedDistrict = prepareDistrictName(districtName);

                String districtKey = regionName + "_" + normalizedDistrict;
                Integer districtId = districtMap.get(districtKey);

                if (districtId != null) {
                    String fullName = city.getType() + " " + city.getDescription();
                    ps.setString(1, fullName);
                    ps.setInt(2, districtId);
                    ps.addBatch();
                    count++;
                }

                if (count > 0 && count % 5000 == 0) {
                    ps.executeBatch();
                    conn.commit();
                }
            }
            ps.executeBatch();
            conn.commit();
            System.out.println("Збережено міст: " + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}