package org.example.repository;

import net.datafaker.Faker;
import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedStreet;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class StreetRepository {
    private final Random random;
    private final Faker faker;

    private static final int MIN_STREETS_PER_CITY = 5;
    private static final int MAX_STREETS_PER_CITY = 30;

    private static final String[] STREET_TYPES = {"вулиця", "проспект", "бульвар", "провулок", "площа", "алея", "набережна", "узвіз"};
    private static final String[] UKRAINIAN_HEROES = {"Шевченка", "Франка", "Грушевського", "Сковороди", "Лесі Українки", "Хмельницького", "Сагайдачного", "Мазепи", "Дорошенка", "Стуса"};
    private static final String[] UKRAINIAN_COLORS = {"Червона", "Зелена", "Синя", "Жовта", "Біла", "Чорна", "Срібна", "Золота"};
    private static final String[] UKRAINIAN_HISTORY = {"Свободи", "Незалежності", "Соборності", "Волі", "Перемоги", "Мирна", "Єдності", "Братства"};

    public StreetRepository(Random random, Faker faker) {
        this.random = random;
        this.faker = faker;
    }

    public List<ParsedStreet> generateStreets(List<Integer> allCityIds) {
        List<ParsedStreet> streets = new ArrayList<>();

        System.out.println("--- ГЕНЕРАЦІЯ ВУЛИЦЬ ДЛЯ ВСІХ " + allCityIds.size() + " МІСТ ІЗ БД ---");

        int processed = 0;
        for (Integer cityId : allCityIds) {
            int streetsCount = MIN_STREETS_PER_CITY + random.nextInt(MAX_STREETS_PER_CITY - MIN_STREETS_PER_CITY + 1);
            Set<String> usedNames = new HashSet<>();

            for (int i = 0; i < streetsCount; i++) {
                ParsedStreet street = new ParsedStreet();
                street.setName(generateUniqueStreetName(usedNames));
                street.setCityId(cityId);
                streets.add(street);
            }

            processed++;
            if (processed % 2000 == 0) {
                System.out.printf("Оброблено %,d міст... (Згенеровано %,d вулиць)\n", processed, streets.size());
            }
        }

        System.out.printf("=== ЗАВЕРШЕНО: Згенеровано вулиці для %,d міст із БД ===\n", processed);
        return streets;
    }

    public void saveStreets(List<ParsedStreet> streets) {
        String sql = "INSERT INTO streets (street_name, city_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setAutoCommit(false);
            int count = 0;
            for (ParsedStreet street : streets) {
                if (street.getCityId() == null) continue;
                ps.setString(1, normalizeStreetName(street.getName()));
                ps.setInt(2, street.getCityId());
                ps.addBatch();
                if (++count % 10000 == 0) {
                    ps.executeBatch();
                    conn.commit();
                    System.out.print(".");
                }
            }
            ps.executeBatch();
            conn.commit();
            System.out.println("\nУспішно збережено вулиць у БД: " + count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateUniqueStreetName(Set<String> usedNames) {
        String streetName;
        int attempts = 0;
        do {
            String type = STREET_TYPES[random.nextInt(STREET_TYPES.length)];
            String name = switch (random.nextInt(6)) {
                case 0 -> UKRAINIAN_HEROES[random.nextInt(UKRAINIAN_HEROES.length)];
                case 1 -> UKRAINIAN_COLORS[random.nextInt(UKRAINIAN_COLORS.length)];
                case 2 -> UKRAINIAN_HISTORY[random.nextInt(UKRAINIAN_HISTORY.length)];
                case 3 -> faker.address().cityName();
                case 4 -> "Сонячна";
                default -> faker.name().lastName();
            };
            streetName = type + " " + name;
            attempts++;
        } while (usedNames.contains(streetName) && attempts < 20);
        usedNames.add(streetName);
        return streetName;
    }

    private String normalizeStreetName(String streetName) {
        if (streetName == null) return "";
        return streetName.trim().replaceAll("\\s+", " ");
    }

    public Map<Integer, List<Integer>> getCityStreetMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT city_id, street_id FROM streets")) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getInt("city_id"), k -> new ArrayList<>()).add(rs.getInt("street_id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }
}