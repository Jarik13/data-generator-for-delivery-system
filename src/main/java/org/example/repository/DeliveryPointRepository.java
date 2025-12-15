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

    public DeliveryPointRepository(Random random) {
        this.random = random;
    }

    private static final String INSERT_PARENT = "INSERT INTO delivery_points (delivery_point_name, delivery_point_address, city_id) VALUES (?, ?, ?)";
    private static final String INSERT_BRANCH = "INSERT INTO branches (delivery_point_id, branch_type_id) VALUES (?, ?)";
    private static final String INSERT_POSTOMAT = "INSERT INTO postomats (delivery_point_id, postomat_code, postomat_cells_count, is_active) VALUES (?, ?, ?, 1)";

    private static final String REF_POSTOMAT = "f9316480-5f2d-425d-bc2c-ac7cd29decf0";

    public void saveDeliveryPoints(List<DeliveryPoint> points, Map<String, Integer> cityMap) {
        log.info("--- Початок збереження точок доставки у БД. Кількість: {} ---", points.size());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psParent = conn.prepareStatement(INSERT_PARENT, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psBranch = conn.prepareStatement(INSERT_BRANCH);
             PreparedStatement psPostomat = conn.prepareStatement(INSERT_POSTOMAT)) {

            conn.setAutoCommit(false);
            int count = 0;
            int createdCities = 0;
            int saved = 0;

            for (DeliveryPoint point : points) {
                if (count % 5000 == 0 && count > 0) {
                    log.info("... оброблено {} точок ...", count);
                }

                Integer cityId = findCityId(point.getCity(), cityMap);

                if (cityId == null) {
                    try {
                        cityId = createCity(conn, point.getCity());
                        cityMap.put(point.getCity(), cityId);
                        createdCities++;
                    } catch (SQLException e) {
                        log.error("Не вдалося створити місто '{}': {}", point.getCity(), e.getMessage());
                        continue;
                    }
                }

                try {
                    psParent.setString(1, point.getName());
                    psParent.setString(2, point.getAddress());
                    psParent.setInt(3, cityId);

                    int affectedRows = psParent.executeUpdate();

                    if (affectedRows > 0) {
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
                    }
                    count++;
                } catch (SQLException e) {
                    log.error("Помилка при збереженні точки '{}': {}", point.getName(), e.getMessage());
                }
            }

            conn.commit();
            log.info("Транзакцію завершено. Отримано: {}, Збережено: {}, Створено нових міст: {}",
                    points.size(), saved, createdCities);
        } catch (SQLException e) {
            log.error("Критична помилка при збереженні точок доставки", e);
        }
    }

    private int createCity(Connection conn, String rawName) throws SQLException {
        String cityName = rawName;
        if (cityName.contains("(")) {
            cityName = cityName.substring(0, cityName.indexOf("(")).trim();
        }

        int distId = findCorrectDistrictId(conn, rawName);

        if (distId == -1) {
            log.warn("Район не знайдено для '{}'. Використовую дефолтний.", rawName);
            distId = getDefaultDistrictId(conn);
        }

        String sql = "INSERT INTO cities (city_name, district_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cityName);
            ps.setInt(2, distId);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Не вдалося створити місто: " + cityName);
    }

    private int findCorrectDistrictId(Connection conn, String rawCityName) {
        String searchPattern = "";

        if (rawCityName.contains("(") && rawCityName.contains("р-н")) {
            int start = rawCityName.indexOf("(") + 1;
            int end = rawCityName.indexOf("р-н");
            if (end > start) {
                String distName = rawCityName.substring(start, end).trim();
                int id = queryDistrictId(conn, distName + " район");
                if (id != -1) return id;

                searchPattern = distName + "%";
            }
        }

        if (searchPattern.isEmpty()) {
            String cleanName = rawCityName;
            if (cleanName.contains("(")) cleanName = cleanName.substring(0, cleanName.indexOf("("));
            cleanName = cleanName.trim();

            int id = queryDistrictId(conn, cleanName + " район");
            if (id != -1) return id;

            if (cleanName.length() >= 4) {
                searchPattern = cleanName.substring(0, Math.min(cleanName.length(), 5)) + "%";
            }
        }

        if (!searchPattern.isEmpty()) {
            String sql = "SELECT TOP 1 district_id FROM districts WHERE district_name LIKE ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, searchPattern);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException e) {
                log.warn("Помилка пошуку району LIKE: {}", e.getMessage());
            }
        }

        return -1;
    }

    private int queryDistrictId(Connection conn, String exactName) {
        String sql = "SELECT district_id FROM districts WHERE district_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, exactName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            log.warn("Помилка пошуку району exact: {}", e.getMessage());
        }
        return -1;
    }

    private int getDefaultDistrictId(Connection conn) throws SQLException {
        if (defaultDistrictId != -1) return defaultDistrictId;
        String sql = "SELECT TOP 1 district_id FROM districts";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                defaultDistrictId = rs.getInt(1);
                return defaultDistrictId;
            }
        }
        throw new SQLException("Таблиця districts порожня! Неможливо створити нове місто.");
    }

    private Integer findCityId(String rawCityName, Map<String, Integer> cityMap) {
        if (rawCityName == null || rawCityName.isBlank()) return null;
        String name = rawCityName.trim();

        if (name.contains("(")) {
            name = name.substring(0, name.indexOf("(")).trim();
        }

        if (cityMap.containsKey(name)) return cityMap.get(name);
        if (cityMap.containsKey("місто " + name)) return cityMap.get("місто " + name);
        if (cityMap.containsKey("м. " + name)) return cityMap.get("м. " + name);
        if (cityMap.containsKey("село " + name)) return cityMap.get("село " + name);
        if (cityMap.containsKey("с. " + name)) return cityMap.get("с. " + name);
        if (cityMap.containsKey("смт " + name)) return cityMap.get("смт " + name);
        if (cityMap.containsKey("селище міського типу " + name)) return cityMap.get("селище міського типу " + name);

        String suffixSpace = " " + name.toLowerCase();
        for (Map.Entry<String, Integer> entry : cityMap.entrySet()) {
            String keyLower = entry.getKey().toLowerCase();
            if (keyLower.equals(name.toLowerCase()) || keyLower.endsWith(suffixSpace)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isPostomat(DeliveryPoint point) {
        if (point.getTypeRef() != null && point.getTypeRef().equals(REF_POSTOMAT)) {
            return true;
        }
        return point.getName().toLowerCase().contains("поштомат");
    }

    private void saveAsPostomat(PreparedStatement ps, int parentId, DeliveryPoint point) throws SQLException {
        ps.setInt(1, parentId);
        String code = String.valueOf(parentId);
        ps.setString(2, code);
        ps.setInt(3, 20 + random.nextInt(60));
        ps.executeUpdate();
    }

    private void saveAsBranch(PreparedStatement ps, int parentId, DeliveryPoint point) throws SQLException {
        ps.setInt(1, parentId);
        int typeId = 2;
        String nameLower = point.getName().toLowerCase();
        if (nameLower.contains("вантажне") || nameLower.contains("1000 кг") || nameLower.contains("1100 кг")) {
            typeId = 1;
        } else if (nameLower.contains("mini") || nameLower.contains("point") || nameLower.contains("shop")) {
            typeId = 3;
        }
        ps.setInt(2, typeId);
        ps.executeUpdate();
    }
}