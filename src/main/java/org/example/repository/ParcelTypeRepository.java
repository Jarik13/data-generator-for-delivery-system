package org.example.repository;

import lombok.RequiredArgsConstructor;
import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedCargoType;

import java.sql.*;
import java.util.List;

@RequiredArgsConstructor
public class ParcelTypeRepository {
    private static final String CHECK_EXISTS_SQL = "SELECT parcel_type_id FROM parcel_types WHERE parcel_type_name = ?";
    private static final String INSERT_SQL = "INSERT INTO parcel_types (parcel_type_name) VALUES (?)";

    public void saveParcelTypes(List<ParsedCargoType> types) {
        System.out.println("--- Збереження типів вмісту посилки (parcel_types) ---");

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psCheck = conn.prepareStatement(CHECK_EXISTS_SQL);
                 PreparedStatement psInsert = conn.prepareStatement(INSERT_SQL)) {

                int count = 0;

                for (ParsedCargoType type : types) {
                    String name = type.getDescription();

                    if (!exists(psCheck, name)) {
                        psInsert.setString(1, name);
                        psInsert.addBatch();
                        count++;
                    }
                }

                psInsert.executeBatch();
                conn.commit();
                System.out.println("Додано записів у parcel_types: " + count);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean exists(PreparedStatement psCheck, String name) throws SQLException {
        psCheck.setString(1, name);
        try (ResultSet rs = psCheck.executeQuery()) {
            return rs.next();
        }
    }
}