package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedCity;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CityRepository {
    public static String prepareDistrictName(String rawName) {
        if (rawName == null || rawName.isBlank()) return "";
        String name = rawName.trim();
        if (name.endsWith("а")) name = name.substring(0, name.length() - 1) + "ий";
        else if (name.endsWith("е")) name = name.substring(0, name.length() - 1) + "ий";

        name = name.replaceAll("(?i)\\s*(р-н|район)$", "").trim();
        return name + " район";
    }

    public static String normalize(String text) {
        if (text == null || text.isBlank()) return "";
        String extra = "";
        Matcher m = Pattern.compile("\\((.*?)\\)").matcher(text);
        if (m.find()) {
            extra = m.group(1).toLowerCase()
                    .replaceAll("(сільська|селищна|міська|рада|тг|громада|територіальна)", "")
                    .replaceAll("[^а-яіїєґ0-9]", "").trim();
        }
        String t = text.replaceAll("\\(.*?\\)", "").toLowerCase().trim();
        t = t.replaceAll("^(селище міського типу|міського типу|місто|село|селище|смт|м\\.|с\\.)\\s+", "");
        t = t.replaceAll("\\s+(район|р-н|область)$", "");
        t = t.replace("'", "").replace("’", "").replace("`", "");
        t = t.replace("i", "і").replace("y", "і").replace("и", "і").replace("ї", "і").replace("є", "е");
        t = t.replaceAll("[^а-яіїєґ0-9]", "");
        t = t.replaceAll("(івська|івський|івське|ська|ський|ське)$", "");
        t = t.replace("ж", "з").replace("ц", "с");
        return t.trim() + (extra.isEmpty() ? "" : "|" + extra);
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
        } catch (Exception e) { e.printStackTrace(); }
    }

    public Map<String, Object> getCityMap() {
        Map<String, Map<String, Integer>> smartMap = new HashMap<>();
        String sql = """
            SELECT c.city_id, c.city_name, d.district_name, r.region_name 
            FROM cities c
            LEFT JOIN districts d ON c.district_id = d.district_id
            LEFT JOIN regions r ON d.region_id = r.region_id
        """;
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("city_id");
                String reg = normalize(rs.getString("region_name"));
                String dist = normalize(rs.getString("district_name"));
                String cityKeyFull = normalize(rs.getString("city_name"));
                String lookupKey = reg + "|" + cityKeyFull;
                smartMap.computeIfAbsent(lookupKey, k -> new HashMap<>()).put(dist, id);
            }
        } catch (Exception e) { e.printStackTrace(); }
        Map<String, Object> result = new HashMap<>();
        result.put("smart", smartMap);
        return result;
    }
}