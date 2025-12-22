package org.example.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.config.DatabaseConfig;
import org.example.model.DeliveryPoint;

import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class DeliveryPointRepository {
    private final Random random;
    private int defaultDistrictId = -1;

    private static final String REF_TYPE_CARGO = "6f8c0829-0676-11e4-80cf-005056801329";
    private static final String REF_TYPE_POST_OFFICE = "9a674f35-4e5c-11e4-81e0-005056801329";
    private static final String REF_TYPE_POSTOMAT = "f9316480-5f2d-425d-bc2c-ac7cd29decf0";
    private static final String REF_TYPE_MINI = "ef757832-4091-11e4-81e0-005056801329";
    private static final String REF_TYPE_MOBILE = "95dc2124-da4b-11e5-837d-005056801329";

    private static final String INSERT_PARENT = "INSERT INTO delivery_points (delivery_point_name, delivery_point_address, city_id) VALUES (?, ?, ?)";
    private static final String INSERT_BRANCH = "INSERT INTO branches (delivery_point_id, branch_type_id) VALUES (?, ?)";
    private static final String INSERT_POSTOMAT = "INSERT INTO postomats (delivery_point_id, postomat_code, postomat_cells_count, is_active) VALUES (?, ?, ?, ?)";

    public void saveDeliveryPoints(List<DeliveryPoint> points, Map<String, Integer> cityMap) {
        log.info("--- Початок збереження точок доставки у БД. Кількість: {} ---", points.size());

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement psParent = conn.prepareStatement(INSERT_PARENT, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement psBranch = conn.prepareStatement(INSERT_BRANCH);
             PreparedStatement psPostomat = conn.prepareStatement(INSERT_POSTOMAT)) {

            conn.setAutoCommit(false);
            int count = 0;
            int savedBranches = 0;
            int savedPostomats = 0;

            for (DeliveryPoint point : points) {
                Integer cityId = findCityId(point.getCity(), cityMap);

                if (cityId == null) {
                    try {
                        cityId = createCity(conn, point.getCity());
                        cityMap.put(CityRepository.normalize(point.getCity()), cityId);
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
                                savedPostomats++;
                            } else {
                                saveAsBranch(psBranch, deliveryPointId, point);
                                savedBranches++;
                            }
                        }
                    }
                } catch (SQLException e) {
                    log.error("Помилка при збереженні '{}': {}", point.getName(), e.getMessage());
                }

                if (++count % 1000 == 0) {
                    conn.commit();
                    log.info("... оброблено {} точок (Відділень: {}, Поштоматів: {}) ...", count, savedBranches, savedPostomats);
                }
            }

            conn.commit();
            log.info("Імпорт завершено. Всього збережено відділень: {}, поштоматів: {}", savedBranches, savedPostomats);
        } catch (SQLException e) {
            log.error("Критична помилка БД", e);
        }
    }

    private void saveAsPostomat(PreparedStatement ps, int parentId, DeliveryPoint point) throws SQLException {
        ps.setInt(1, parentId);
        ps.setString(2, point.getRef());
        ps.setInt(3, 20 + random.nextInt(60));

        int isActive = (random.nextDouble() < 0.9) ? 1 : 0;
        ps.setInt(4, isActive);

        ps.executeUpdate();
    }

    private void saveAsBranch(PreparedStatement ps, int parentId, DeliveryPoint point) throws SQLException {
        ps.setInt(1, parentId);
        int typeId;
        String typeRef = point.getTypeRef();
        String nameLower = point.getName().toLowerCase();

        if (REF_TYPE_CARGO.equals(typeRef)) typeId = 1;
        else if (REF_TYPE_POST_OFFICE.equals(typeRef)) typeId = 2;
        else if (REF_TYPE_MOBILE.equals(typeRef) || nameLower.contains("мобільне")) typeId = 4;
        else if (REF_TYPE_MINI.equals(typeRef) || nameLower.contains("mini") || nameLower.contains("point")) typeId = 5;
        else if (nameLower.contains("приймання-видачі")) typeId = 3;
        else if (nameLower.contains("пункт")) typeId = 6;
        else typeId = 2;

        ps.setInt(2, typeId);
        ps.executeUpdate();
    }

    private boolean isPostomat(DeliveryPoint point) {
        return point.getTypeRef().equalsIgnoreCase(REF_TYPE_POSTOMAT)
               || point.getName().toLowerCase().contains("поштомат");
    }

    private String cleanName(String name) {
        if (name == null) return "";
        return name.contains(":") ? name.split(":")[0].trim() : name.trim();
    }

    private Integer findCityId(String rawCityName, Map<String, Integer> cityMap) {
        if (rawCityName == null || rawCityName.isBlank()) return null;
        String normName = CityRepository.normalize(rawCityName);
        if (cityMap.containsKey(normName)) return cityMap.get(normName);
        String cleanName = rawCityName.contains("(") ? rawCityName.substring(0, rawCityName.indexOf("(")).trim() : rawCityName.trim();
        String normClean = CityRepository.normalize(cleanName);
        if (cityMap.containsKey(normClean)) return cityMap.get(normClean);
        for (Map.Entry<String, Integer> entry : cityMap.entrySet()) {
            if (entry.getKey().contains(normClean)) return entry.getValue();
        }
        return null;
    }

    private int createCity(Connection conn, String rawName) throws SQLException {
        String cityName = rawName.contains("(") ? rawName.substring(0, rawName.indexOf("(")).trim() : rawName.trim();
        int distId = findCorrectDistrictId(conn, rawName);
        if (distId == -1) distId = getDefaultDistrictId(conn);
        String sql = "INSERT INTO cities (city_name, district_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cityName);
            ps.setInt(2, distId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        throw new SQLException("Failed to create city");
    }

    private int findCorrectDistrictId(Connection conn, String rawCityName) {
        String distName = "";
        if (rawCityName.contains("(") && rawCityName.contains("р-н")) {
            distName = rawCityName.substring(rawCityName.indexOf("(") + 1, rawCityName.indexOf("р-н")).trim();
        } else if (rawCityName.contains("(") && rawCityName.contains("район")) {
            distName = rawCityName.substring(rawCityName.indexOf("(") + 1, rawCityName.indexOf("район")).trim();
        }
        if (!distName.isEmpty()) {
            String formattedDist = DistrictRepository.formatDistrictName(distName);
            return queryDistrictId(conn, formattedDist);
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
            log.warn("District search error: {}", e.getMessage());
        }
        return -1;
    }

    private int getDefaultDistrictId(Connection conn) throws SQLException {
        if (defaultDistrictId != -1) return defaultDistrictId;
        String sql = "SELECT TOP 1 district_id FROM districts";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                defaultDistrictId = rs.getInt(1);
                return defaultDistrictId;
            }
        }
        throw new SQLException("Districts table is empty!");
    }
}