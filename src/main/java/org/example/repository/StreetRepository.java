package org.example.repository;

import net.datafaker.Faker;
import org.example.config.DatabaseConfig;
import org.example.model.parsed.ParsedCity;
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

    private static final String[] STREET_TYPES = {
            "вулиця", "проспект", "бульвар", "провулок",
            "площа", "алея", "набережна", "узвіз"
    };

    private static final String[] UKRAINIAN_HEROES = {
            "Шевченко", "Франко", "Грушевський", "Сковороди",
            "Лесі Українки", "Хмельницького", "Сагайдачного",
            "Мазепи", "Дорошенка", "Стуса"
    };

    private static final String[] UKRAINIAN_COLORS = {
            "Червоний", "Зелений", "Синій", "Жовтий", "Білий",
            "Чорний", "Срібний", "Золотий"
    };

    private static final String[] UKRAINIAN_HISTORY = {
            "Свободи", "Незалежності", "Соборності", "Волі",
            "Перемоги", "Мирна", "Єдності", "Братства"
    };

    public StreetRepository(Random random, Faker faker) {
        this.random = random;
        this.faker = faker;
    }

    public List<ParsedStreet> generateStreets(List<ParsedCity> cities, Map<String, Object> smartCityMap) {
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> smartMap = (Map<String, Map<String, Integer>>) smartCityMap.get("smart");

        List<ParsedStreet> streets = new ArrayList<>();
        int processed = 0;
        int skipped = 0;

        for (ParsedCity apiCity : cities) {
            String regKey = CityRepository.normalize(apiCity.getRegion());
            String cityCleanName = CityRepository.normalize(apiCity.getDescription());
            String apiDist = CityRepository.normalize(apiCity.getArea());

            String lookupKey = regKey + "|" + cityCleanName;

            Map<String, Integer> candidates = smartMap.get(lookupKey);
            if (candidates == null) {
                for (String fullKey : smartMap.keySet()) {
                    if (fullKey.startsWith(lookupKey + "|")) {
                        candidates = smartMap.get(fullKey);
                        break;
                    }
                }
            }

            Integer cityId = null;
            if (candidates != null) {
                if (candidates.size() == 1) {
                    cityId = candidates.values().iterator().next();
                } else {
                    cityId = candidates.get(apiDist);
                    if (cityId == null) {
                        for (Map.Entry<String, Integer> entry : candidates.entrySet()) {
                            if (!entry.getKey().isEmpty() && (entry.getKey().contains(apiDist) || apiDist.contains(entry.getKey()))) {
                                cityId = entry.getValue();
                                break;
                            }
                        }
                    }
                }
            }

            if (cityId == null) {
                skipped++;
                continue;
            }

            int streetsCount = MIN_STREETS_PER_CITY + random.nextInt(MAX_STREETS_PER_CITY - MIN_STREETS_PER_CITY + 1);
            Set<String> usedNames = new HashSet<>();
            for (int i = 0; i < streetsCount; i++) {
                ParsedStreet street = new ParsedStreet();
                street.setName(generateUniqueStreetName(usedNames));
                street.setCityId(cityId);
                streets.add(street);
            }
            processed++;
        }
        System.out.printf("=== РЕЗУЛЬТАТ ГЕНЕРАЦІЇ: Оброблено %,d міст | Пропущено %,d | Всього вулиць: %,d ===\n",
                processed, skipped, streets.size());
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
                if (++count % 5000 == 0) { ps.executeBatch(); conn.commit(); }
            }
            ps.executeBatch();
            conn.commit();
        } catch (Exception e) { e.printStackTrace(); }
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
                case 4 -> generateUkrainianNatureName();
                default -> faker.name().lastName();
            };
            streetName = type + " " + name;
            attempts++;
        } while (usedNames.contains(streetName) && attempts < 50);

        usedNames.add(streetName);
        return streetName;
    }

    private String generateUkrainianNatureName() {
        String[] natureNames = {"Дніпро", "Карпати", "Чорногора", "Лісова", "Польова", "Сонячна"};
        return natureNames[random.nextInt(natureNames.length)];
    }

    private String normalizeStreetName(String streetName) {
        streetName = streetName.trim().replaceAll("\\s+", " ");
        String[] words = streetName.split(" ");
        if (words.length >= 2) {
            words[0] = words[0].toLowerCase();
            words[1] = words[1].substring(0, 1).toUpperCase() + words[1].substring(1).toLowerCase();
        }
        return String.join(" ", words);
    }

    public Map<Integer, List<Integer>> getCityStreetMap() {
        Map<Integer, List<Integer>> map = new HashMap<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT city_id, street_id FROM streets")) {
            while (rs.next()) {
                map.computeIfAbsent(rs.getInt("city_id"), k -> new ArrayList<>()).add(rs.getInt("street_id"));
            }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }
}