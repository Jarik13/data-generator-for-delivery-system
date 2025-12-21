package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedCity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class CityRepository {
    public static String buildCompositeKey(String region, String district, String cityFullName) {
        String r = normalize(region);
        String d = normalize(district);
        String c = normalize(cityFullName);

        if (d.isBlank()) {
            d = c;
        }

        return r + "|" + d + "|" + c;
    }

    public static String normalize(String text) {
        if (text == null) return "";

        String t = text.toLowerCase()
                .replaceAll("(селище міського типу|міського типу|область|район|р-н|місто|село|селище|смт|м\\.|с\\.)", "")
                .replaceAll("[^а-яіїєґ]", "")
                .trim();

        t = t.replaceAll("(ська|ський|ське|івська|івський|івське|ий|а|е|я|о|ь)$", "");
        t = t.replace("з", "ж");

        return t;
    }

    public void saveCities(List<ParsedCity> cities, Map<String, Integer> districtMap) {
        System.out.println("--- Збереження населених пунктів у БД ---");
        String sql = "INSERT INTO cities (city_name, district_id) VALUES (?, ?)";
        Set<String> processedKeys = new HashSet<>();

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int count = 0;
            for (ParsedCity city : cities) {
                String regionName = city.getRegion();
                String rawDistrict = city.getArea();
                if (rawDistrict == null || rawDistrict.isEmpty()) continue;

                String searchDistrict = rawDistrict.trim();
                if (searchDistrict.endsWith("а")) searchDistrict = searchDistrict.substring(0, searchDistrict.length() - 1) + "ий";
                else if (searchDistrict.endsWith("е")) searchDistrict = searchDistrict.substring(0, searchDistrict.length() - 1) + "ий";
                searchDistrict = searchDistrict.replace(" район", "").replace(" р-н", "").trim();

                String districtKey = regionName + "_" + searchDistrict;
                Integer districtId = districtMap.get(districtKey);

                if (districtId != null) {
                    String fullName = city.getType() + " " + city.getDescription();
                    String dupKey = districtId + "_" + fullName;
                    if (processedKeys.contains(dupKey)) continue;
                    processedKeys.add(dupKey);

                    ps.setString(1, fullName);
                    ps.setInt(2, districtId);
                    ps.addBatch();
                    count++;
                }
                if (count > 0 && count % 5000 == 0) { ps.executeBatch(); conn.commit(); }
            }
            ps.executeBatch();
            conn.commit();
            System.out.println("Збережено міст: " + count);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Map<String, Integer> getCityCompositeMap() {
        Map<String, Integer> map = new HashMap<>();
        String sql = """
            SELECT c.city_id, c.city_name, d.district_name, r.region_name 
            FROM cities c
            JOIN districts d ON c.district_id = d.district_id
            JOIN regions r ON d.region_id = r.region_id
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String key = buildCompositeKey(
                        rs.getString("region_name"),
                        rs.getString("district_name"),
                        rs.getString("city_name")
                );
                map.put(key, rs.getInt("city_id"));
            }
            System.out.println("Завантажено складену мапу міст: " + map.size() + " записів");
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }
}