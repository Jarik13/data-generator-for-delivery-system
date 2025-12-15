package org.example.manager.helper;

import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.model.parsed.ParsedCity;
import org.example.model.parsed.ParsedDistrict;
import org.example.model.parsed.ParsedRegion;
import org.example.repository.CityRepository;
import org.example.repository.DistrictRepository;
import org.example.repository.RegionRepository;

import java.util.*;

@Slf4j
public class GeoDataManager {
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final CityRepository cityRepository;

    public GeoDataManager() {
        this.regionRepository = new RegionRepository();
        this.districtRepository = new DistrictRepository();
        this.cityRepository = new CityRepository();
    }

    public void importGeography(NovaPoshtaAPI api) {
        log.info("=== ПОЧАТОК ІМПОРТУ ГЕОГРАФІЇ ===");
        long start = System.currentTimeMillis();

        try {
            log.info(">>> [1/3] Завантаження та збереження областей...");
            List<ParsedRegion> regions = api.getAreas();
            regionRepository.saveRegions(regions);

            Map<String, Integer> regionMap = regionRepository.getRegionNameIdMap();
            log.debug("Отримано мапу областей: {} записів", regionMap.size());

            log.info(">>> [2/3] Завантаження міст з API...");
            List<ParsedCity> cities = api.getSettlements();
            log.info("Завантажено міст з API: {}", cities.size());

            log.info(">>> Вилучення унікальних районів зі списку міст...");
            List<ParsedDistrict> districts = extractDistrictsFromCities(cities);
            log.info("Знайдено унікальних районів: {}", districts.size());

            districtRepository.saveDistricts(districts, regionMap);

            Map<String, Integer> districtMap = districtRepository.getDistrictNameIdMap(regionMap);
            log.info("Завантажено мапу районів з БД: {} записів.", districtMap.size());

            log.info(">>> [3/3] Збереження міст у БД...");
            cityRepository.saveCities(cities, districtMap);

            long duration = (System.currentTimeMillis() - start) / 1000;
            log.info("=== ІМПОРТ ГЕОГРАФІЇ ЗАВЕРШЕНО ({} с.) ===", duration);
        } catch (Exception e) {
            log.error("КРИТИЧНА ПОМИЛКА ПРИ ІМПОРТІ ГЕОГРАФІЇ", e);
        }
    }

    public Map<String, Integer> getCityMap() {
        return cityRepository.getCityNameIdMap();
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