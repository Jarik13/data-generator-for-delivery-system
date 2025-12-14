package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedRegion;

import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionRepository {
    public void saveRegions(List<ParsedRegion> regions) {
        System.out.println("--- Збереження областей у БД ---");
        String sql = "INSERT INTO regions (region_name) VALUES (?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (ParsedRegion region : regions) {
                String name = region.getName();
                if (!name.contains("АРК") && !name.contains("Крим") && !name.contains("область")) {
                    name += " область";
                }
                ps.setString(1, name);
                ps.addBatch();
            }

            ps.executeBatch();
            conn.commit();
            System.out.println("Збережено областей: " + regions.size());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getRegionNameIdMap() {
        Map<String, Integer> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT region_id, region_name FROM regions")) {

            while (rs.next()) {
                String name = rs.getString("region_name");
                String key = name.replace(" область", "").trim();

                map.put(key, rs.getInt("region_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
}