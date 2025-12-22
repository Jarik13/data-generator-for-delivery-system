package org.example.repository;

import lombok.RequiredArgsConstructor;
import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedPack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class BoxRepository {
    private final Random random;

    private static final String INSERT_TYPE = "INSERT INTO box_types (box_type_name) VALUES (?)";
    private static final String INSERT_VARIANT = "INSERT INTO box_variants (box_variant_price, box_variant_width, box_variant_length, box_variant_height, box_type_id) VALUES (?, ?, ?, ?, ?)";

    private static final Pattern DIM_PATTERN = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*[*xх]\\s*(\\d+(?:[.,]\\d+)?)(?:\\s*[*xх]\\s*(\\d+(?:[.,]\\d+)?))?");

    public void saveBoxData(List<ParsedPack> packs) {
        System.out.println("--- Збереження пакувань та варіантів у БД ---");

        Map<String, Integer> typeCache = new HashMap<>();

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psType = conn.prepareStatement(INSERT_TYPE, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement psVariant = conn.prepareStatement(INSERT_VARIANT)) {

                int count = 0;

                for (ParsedPack pack : packs) {
                    String rawName = pack.getDescription();

                    enrichDimensions(pack);

                    String cleanName = cleanBoxName(rawName);
                    int typeId = getOrCreateTypeId(conn, psType, typeCache, cleanName);

                    BigDecimal price = BigDecimal.valueOf(5 + (145 * random.nextDouble()))
                            .setScale(2, RoundingMode.HALF_UP);

                    psVariant.setBigDecimal(1, price);
                    psVariant.setBigDecimal(2, pack.getWidth());
                    psVariant.setBigDecimal(3, pack.getLength());
                    psVariant.setBigDecimal(4, pack.getHeight());
                    psVariant.setInt(5, typeId);

                    psVariant.addBatch();
                    count++;
                }

                psVariant.executeBatch();
                conn.commit();
                System.out.println("Збережено варіантів пакування: " + count);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void enrichDimensions(ParsedPack pack) {
        if (pack.getLength().compareTo(BigDecimal.ZERO) > 0 || pack.getWidth().compareTo(BigDecimal.ZERO) > 0) {
            return;
        }

        Matcher matcher = DIM_PATTERN.matcher(pack.getDescription());
        if (matcher.find()) {
            try {
                String d1 = matcher.group(1).replace(",", ".");
                String d2 = matcher.group(2).replace(",", ".");

                pack.setLength(new BigDecimal(d1));
                pack.setWidth(new BigDecimal(d2));

                if (matcher.group(3) != null) {
                    String d3 = matcher.group(3).replace(",", ".");
                    pack.setHeight(new BigDecimal(d3));
                } else {
                    pack.setHeight(BigDecimal.ZERO);
                }
            } catch (Exception _) {
            }
        }
    }

    private int getOrCreateTypeId(Connection conn, PreparedStatement psInsert, Map<String, Integer> cache, String typeName) throws SQLException {
        if (cache.containsKey(typeName)) {
            return cache.get(typeName);
        }

        String checkSql = "SELECT box_type_id FROM box_types WHERE box_type_name = ?";
        try (PreparedStatement psCheck = conn.prepareStatement(checkSql)) {
            psCheck.setString(1, typeName);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    cache.put(typeName, id);
                    return id;
                }
            }
        }

        psInsert.setString(1, typeName);
        psInsert.executeUpdate();
        try (ResultSet rs = psInsert.getGeneratedKeys()) {
            if (rs.next()) {
                int id = rs.getInt(1);
                cache.put(typeName, id);
                return id;
            }
        }

        throw new SQLException("Не вдалося отримати ID для типу коробки: " + typeName);
    }

    private String cleanBoxName(String rawName) {
        String name = rawName;
        name = name.replaceAll("\\(.*?\\)", "");
        name = name.replaceAll("\\d+([.,]\\d+)?\\s*[*xх]\\s*\\d+([.,]\\d+)?", "");
        name = name.replaceAll("(?i)\\b\\d+\\s*кг\\b", "");
        name = name.replaceAll("(?i)\\b\\d+\\s*мм\\b", "");
        name = name.replaceAll("(?i)\\bб/н\\b", "");
        name = name.replaceAll("(?i)\\bниз\\b", "");
        name = name.replaceAll("(?i)\\bверх\\b", "");
        name = name.replaceAll("\\s+", " ").trim();

        if (!name.isEmpty()) {
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        return name;
    }
}