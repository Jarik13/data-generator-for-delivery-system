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

    private static final int STREETS_KYIV = 2500;
    private static final int STREETS_KHARKIV = 1800;
    private static final int STREETS_METROPOLIS = 1300;
    private static final int STREETS_LARGE_CENTER = 900;
    private static final int STREETS_REGIONAL_CENTER = 600;
    private static final int STREETS_BIG_CITY = 400;

    private static final int CITY_MIN = 150;
    private static final int CITY_MAX = 400;

    private static final int SMT_MIN = 30;
    private static final int SMT_MAX = 80;

    private static final int SETTLEMENT_MIN = 10;
    private static final int SETTLEMENT_MAX = 30;

    private static final int VILLAGE_MIN = 2;
    private static final int VILLAGE_MAX = 10;

    private static final int DEFAULT_MIN = 5;
    private static final int DEFAULT_MAX = 20;

    private static final String[] STREET_TYPES = {"вулиця", "проспект", "бульвар", "провулок", "площа", "алея", "набережна", "узвіз"};
    private static final String[] UKRAINIAN_HEROES = {"Шевченка", "Франка", "Грушевського", "Сковороди", "Лесі Українки", "Хмельницького", "Сагайдачного", "Мазепи", "Дорошенка", "Стуса"};
    private static final String[] UKRAINIAN_COLORS = {"Червона", "Зелена", "Синя", "Жовта", "Біла", "Чорна", "Срібна", "Золота"};
    private static final String[] UKRAINIAN_HISTORY = {"Свободи", "Незалежності", "Соборності", "Волі", "Перемоги", "Мирна", "Єдності", "Братства"};

    public StreetRepository(Random random, Faker faker) {
        this.random = random;
        this.faker = faker;
    }

    public List<ParsedStreet> generateStreets(Map<Integer, String> cityData) {
        List<ParsedStreet> streets = new ArrayList<>();
        System.out.println("--- ГЕНЕРАЦІЯ ВУЛИЦЬ НА ОСНОВІ ТИПУ НАСЕЛЕНОГО ПУНКТУ ---");

        int processed = 0;
        for (Map.Entry<Integer, String> entry : cityData.entrySet()) {
            Integer cityId = entry.getKey();
            String cityName = entry.getValue().toLowerCase();

            int streetsCount = determineStreetCount(cityName);

            Set<String> usedNames = new HashSet<>();
            for (int i = 0; i < streetsCount; i++) {
                ParsedStreet street = new ParsedStreet();
                street.setName(generateUniqueStreetName(usedNames));
                street.setCityId(cityId);
                streets.add(street);
            }

            processed++;
            if (processed % 2000 == 0 || streetsCount > 500) {
                System.out.printf("Оброблено %,d міст... (Останнє: %s, вулиць: %d)\n",
                        processed, entry.getValue(), streetsCount);
            }
        }

        System.out.printf("=== ЗАВЕРШЕНО: Згенеровано вулиці для %,d міст. Всього вулиць: %,d ===\n",
                processed, streets.size());
        return streets;
    }

    private int determineStreetCount(String cityName) {
        String name = cityName.toLowerCase();

        boolean isCity = name.startsWith("місто") || name.startsWith("м.");
        if (isCity) {
            if (name.contains("київ")) return STREETS_KYIV;

            if (name.contains("харків")) return STREETS_KHARKIV;

            if (name.contains("одеса") || name.contains("дніпро") ||
                name.contains("львів") || name.contains("донецьк")) {
                return STREETS_METROPOLIS;
            }

            if (name.contains("запоріжжя") || name.contains("кривий ріг") ||
                name.contains("миколаїв") || name.contains("маріуполь") ||
                name.contains("луганськ")) {
                return STREETS_LARGE_CENTER;
            }

            if (name.contains("полтава") || name.contains("вінниця") ||
                name.contains("житомир") || name.contains("чернігів") ||
                name.contains("черкаси") || name.contains("хмельницький") ||
                name.contains("суми") || name.contains("рівне") ||
                name.contains("івано-франківськ") || name.contains("тернопіль") ||
                name.contains("луцьк") || name.contains("біла церква") ||
                name.contains("кропивницький") || name.contains("херсон") ||
                name.contains("чернівці") || name.contains("ужгород")) {
                return STREETS_REGIONAL_CENTER;
            }

            if (name.contains("кременчук") || name.contains("кам'янське") ||
                name.contains("краматорськ") || name.contains("мелітополь") ||
                name.contains("бердянськ") || name.contains("нікополь") ||
                name.contains("слов'янськ") || name.contains("бровари") ||
                name.contains("павлоград") || name.contains("уман")) {
                return STREETS_BIG_CITY;
            }

            return CITY_MIN + random.nextInt(CITY_MAX - CITY_MIN + 1);
        }

        if (name.startsWith("смт") || name.contains("селище міського типу")) {
            return SMT_MIN + random.nextInt(SMT_MAX - SMT_MIN + 1);
        }

        if (name.startsWith("селище")) {
            return SETTLEMENT_MIN + random.nextInt(SETTLEMENT_MAX - SETTLEMENT_MIN + 1);
        }

        if (name.startsWith("село") || name.startsWith("с.")) {
            return VILLAGE_MIN + random.nextInt(VILLAGE_MAX - VILLAGE_MIN + 1);
        }

        return DEFAULT_MIN + random.nextInt(DEFAULT_MAX - DEFAULT_MIN + 1);
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

            if (attempts > 15) {
                streetName += " " + (random.nextInt(100) + 1);
            }
        } while (usedNames.contains(streetName) && attempts < 30);
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