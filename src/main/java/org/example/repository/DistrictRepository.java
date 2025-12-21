package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedDistrict;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DistrictRepository {

    public void saveDistricts(List<ParsedDistrict> districts, Map<String, Integer> regionMap) {
        System.out.println("--- Збереження районів у БД ---");
        String sql = "INSERT INTO districts (district_name, region_id) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            int count = 0;

            for (ParsedDistrict district : districts) {
                Integer regionId = regionMap.get(district.getRegionName());

                if (regionId != null) {
                    String correctName = formatDistrictName(district.getName());

                    ps.setString(1, correctName);
                    ps.setInt(2, regionId);
                    ps.addBatch();
                    count++;
                }
            }

            ps.executeBatch();
            conn.commit();
            System.out.println("Збережено районів: " + count);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String formatDistrictName(String rawName) {
        if (rawName == null || rawName.isBlank()) return "";

        String name = rawName.trim()
                .replaceAll("(?i)\\s*(р-н|район)$", "");

        if (name.endsWith("а")) {
            name = name.substring(0, name.length() - 1) + "ий";
        } else if (name.endsWith("я") && !name.equals("Ічня")) {
            name = name.substring(0, name.length() - 1) + "ій";
        } else if (name.endsWith("е")) {
            name = name.substring(0, name.length() - 1) + "ий";
        }

        return name + " район";
    }

    public Map<String, Integer> getDistrictNameIdMap(Map<String, Integer> regionMap) {
        Map<String, Integer> resultMap = new HashMap<>();

        Map<Integer, String> regionIdToName = new HashMap<>();
        for (Map.Entry<String, Integer> entry : regionMap.entrySet()) {
            regionIdToName.put(entry.getValue(), entry.getKey());
        }

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT district_id, district_name, region_id FROM districts")) {

            while (rs.next()) {
                int id = rs.getInt("district_id");
                String dbName = rs.getString("district_name");
                int regId = rs.getInt("region_id");

                String regName = regionIdToName.get(regId);
                if (regName != null) {
                    resultMap.put(regName + "_" + dbName, id);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return resultMap;
    }
}