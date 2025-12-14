package org.example.manager;

import org.example.api.NovaPoshtaAPI;
import org.example.model.parsed.ParsedCity;
import org.example.model.parsed.ParsedDistrict;
import org.example.model.parsed.ParsedRegion;
import org.example.repository.CityRepository;
import org.example.repository.DistrictRepository;
import org.example.repository.RegionRepository;

import java.util.*;

public class DataManager {
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final CityRepository cityRepository;

    public DataManager() {
        this.regionRepository = new RegionRepository();
        this.districtRepository = new DistrictRepository();
        this.cityRepository = new CityRepository();
    }

    public void importFullGeography(NovaPoshtaAPI api) {
        System.out.println("\n=== ПОЧАТОК ІМПОРТУ ГЕОГРАФІЇ ===");
        long start = System.currentTimeMillis();

        try {
            System.out.println("\n>>> [1/3] Завантаження та збереження областей...");
            List<ParsedRegion> regions = api.getAreas();
            regionRepository.saveRegions(regions);
            Map<String, Integer> regionMap = regionRepository.getRegionNameIdMap();

            System.out.println("\n>>> [2/3] Завантаження міст з API...");
            List<ParsedCity> cities = api.getSettlements();
            System.out.println("Завантажено міст з API: " + cities.size());

            System.out.println(">>> Вилучення унікальних районів зі списку міст...");
            List<ParsedDistrict> districts = extractDistrictsFromCities(cities);
            districtRepository.saveDistricts(districts, regionMap);

            Map<String, Integer> districtMap = districtRepository.getDistrictNameIdMap(regionMap);
            System.out.println("Завантажено мапу районів: " + districtMap.size() + " записів.");

            System.out.println("\n>>> [3/3] Збереження міст у БД...");
            cityRepository.saveCities(cities, districtMap);

            long duration = (System.currentTimeMillis() - start) / 1000;
            System.out.println("\n=== ІМПОРТ ЗАВЕРШЕНО УСПІШНО (" + duration + " с.) ===");
        } catch (Exception e) {
            System.err.println("ПОМИЛКА ПРИ ІМПОРТІ: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<ParsedDistrict> extractDistrictsFromCities(List<ParsedCity> cities) {
        Map<String, ParsedDistrict> uniqueDistricts = new HashMap<>();

        for (ParsedCity city : cities) {
            String rawDistrict = city.getArea();
            String regionName = city.getRegion();

            if (rawDistrict == null || rawDistrict.isBlank()) continue;

            String key = regionName + "_" + rawDistrict;

            if (!uniqueDistricts.containsKey(key)) {
                ParsedDistrict district = new ParsedDistrict();

                String fixedName = rawDistrict;
                if (fixedName.endsWith("а")) {
                    fixedName = fixedName.substring(0, fixedName.length() - 1) + "ий";
                } else if (fixedName.endsWith("е")) {
                    fixedName = fixedName.substring(0, fixedName.length() - 1) + "ий";
                }

                if (!fixedName.toLowerCase().contains("район") && !fixedName.toLowerCase().contains("р-н")) {
                    fixedName += " район";
                }

                district.setName(fixedName);
                district.setRegionName(regionName);
                uniqueDistricts.put(key, district);
            }
        }
        return new ArrayList<>(uniqueDistricts.values());
    }
}