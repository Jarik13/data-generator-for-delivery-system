package org.example.repository;

import lombok.extern.slf4j.Slf4j;
import org.example.config.DatabaseConfig;
import org.example.model.DeliveryPoint;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
public class DeliveryPointRepository {
    private final Random random;
    private int defaultDistrictId = -1;

    private static final String REF_TYPE_CARGO = "6f8c0829-0676-11e4-80cf-005056801329";
    private static final String REF_TYPE_POST_OFFICE = "9a674f35-4e5c-11e4-81e0-005056801329";
    private static final String REF_TYPE_POSTOMAT = "f9316480-5f2d-425d-bc2c-ac7cd29decf0";

    private static final String INSERT_PARENT = "INSERT INTO delivery_points (delivery_point_name, delivery_point_address, city_id) VALUES (?, ?, ?)";
    private static final String INSERT_BRANCH = "INSERT INTO branches (delivery_point_id, branch_type_id) VALUES (?, ?)";
    private static final String INSERT_POSTOMAT = "INSERT INTO postomats (delivery_point_id, postomat_code, postomat_cells_count, is_active) VALUES (?, ?, ?, ?)";

    public DeliveryPointRepository(Random random) {
        this.random = random;
    }

    public void saveDeliveryPoints(List<DeliveryPoint> points, Map<String, Object> smartCityMap) {
        log.info("--- Початок збереження точок доставки. Кількість: {} ---", points.size());

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> smartMap = (Map<String, Map<String, Integer>>) smartCityMap.get("smart");

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psParent = conn.prepareStatement(INSERT_PARENT, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psBranch = conn.prepareStatement(INSERT_BRANCH);
             PreparedStatement psPostomat = conn.prepareStatement(INSERT_POSTOMAT)) {

            conn.setAutoCommit(false);
            int count = 0;
            int saved = 0;

            for (DeliveryPoint point : points) {
                Integer cityId = findCityIdSmart(point.getCity(), smartMap);

                if (cityId == null) {
                    try {
                        cityId = createCity(conn, point.getCity());
                        String norm = CityRepository.normalize(point.getCity());
                        String[] parts = norm.split("\\|");
                        String name = parts[0];
                        String dist = (parts.length > 1) ? parts[1] : "";
                        smartMap.computeIfAbsent("unknown|" + name, k -> new java.util.HashMap<>()).put(dist, cityId);
                    } catch (SQLException e) {
                        continue;
                    }
                }

                try {
                    String cleanPointName = cleanName(point.getName());
                    psParent.setString(1, cleanPointName);
                    psParent.setString(2, point.getAddress());
                    psParent.setInt(3, cityId);
                    psParent.executeUpdate();

                    try (ResultSet generatedKeys = psParent.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int deliveryPointId = generatedKeys.getInt(1);
                            if (isPostomat(point)) {
                                saveAsPostomat(psPostomat, deliveryPointId, point);
                            } else {
                                saveAsBranch(psBranch, deliveryPointId, point);
                            }
                            saved++;
                        }
                    }
                } catch (SQLException e) {
                    log.error("Помилка запису точки: {}", e.getMessage());
                }

                if (++count % 1000 == 0) {
                    conn.commit();
                }
            }
            conn.commit();
            log.info("Імпорт завершено. Успішно збережено: {}", saved);
        } catch (SQLException e) {
            log.error("Критична помилка БД", e);
        }
    }

    private Integer findCityIdSmart(String rawCityName, Map<String, Map<String, Integer>> smartMap) {
        if (rawCityName == null || rawCityName.isBlank()) return null;

        String normalized = CityRepository.normalize(rawCityName);
        String[] parts = normalized.split("\\|");
        String namePart = parts[0];
        String districtHint = (parts.length > 1) ? parts[1] : "";

        for (Map.Entry<String, Map<String, Integer>> entry : smartMap.entrySet()) {
            String fullKey = entry.getKey();

            if (fullKey.endsWith("|" + namePart)) {
                Map<String, Integer> districtCandidates = entry.getValue();

                if (districtCandidates.size() == 1 && districtHint.isEmpty()) {
                    return districtCandidates.values().iterator().next();
                }

                if (!districtHint.isEmpty()) {
                    for (Map.Entry<String, Integer> distEntry : districtCandidates.entrySet()) {
                        String dbDist = distEntry.getKey();
                        if (dbDist.contains(districtHint) || districtHint.contains(dbDist)) {
                            return distEntry.getValue();
                        }
                    }
                }
            }
        }
        return null;
    }

    private void saveAsPostomat(PreparedStatement ps, int parentId, DeliveryPoint point) throws SQLException {
        ps.setInt(1, parentId);
        ps.setString(2, point.getRef());
        ps.setInt(3, 20 + random.nextInt(60));
        ps.setInt(4, (random.nextDouble() < 0.9) ? 1 : 0);
        ps.executeUpdate();
    }

    private void saveAsBranch(PreparedStatement ps, int parentId, DeliveryPoint point) throws SQLException {
        ps.setInt(1, parentId);
        int typeId;
        String typeRef = point.getTypeRef();
        String nameLower = point.getName().toLowerCase();

        if (REF_TYPE_CARGO.equals(typeRef)) typeId = 1;
        else if (REF_TYPE_POST_OFFICE.equals(typeRef)) typeId = 2;
        else if (nameLower.contains("приймання-видачі")) typeId = 3;
        else if (nameLower.contains("пункт")) typeId = 6;
        else typeId = 2;

        ps.setInt(2, typeId);
        ps.executeUpdate();
    }

    private boolean isPostomat(DeliveryPoint point) {
        String typeRef = point.getTypeRef();
        return (typeRef != null && typeRef.equalsIgnoreCase(REF_TYPE_POSTOMAT))
               || point.getName().toLowerCase().contains("поштомат");
    }

    private String cleanName(String name) {
        if (name == null) return "";
        return name.contains(":") ? name.split(":")[0].trim() : name.trim();
    }

    private int createCity(Connection conn, String rawName) throws SQLException {
        String cityName = rawName.contains("(") ? rawName.substring(0, rawName.indexOf("(")).trim() : rawName.trim();
        int distId = findCorrectDistrictId(conn, rawName);
        if (distId == -1) distId = getDefaultDistrictId(conn);

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO cities (city_name, district_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cityName);
            ps.setInt(2, distId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private int findCorrectDistrictId(Connection conn, String rawCityName) {
        if (rawCityName.contains("(") && (rawCityName.contains("р-н") || rawCityName.contains("район"))) {
            String distName = rawCityName.substring(rawCityName.indexOf("(") + 1);
            distName = distName.replaceAll("(?i)\\s*(р-н|район|\\))", "").trim();
            String formatted = DistrictRepository.formatDistrictName(distName);

            try (PreparedStatement ps = conn.prepareStatement("SELECT district_id FROM districts WHERE district_name = ?")) {
                ps.setString(1, formatted);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException ignored) {}
        }
        return -1;
    }

    private int getDefaultDistrictId(Connection conn) throws SQLException {
        if (defaultDistrictId != -1) return defaultDistrictId;
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT TOP 1 district_id FROM districts")) {
            if (rs.next()) defaultDistrictId = rs.getInt(1);
        }
        return defaultDistrictId;
    }
}