package org.example.manager.helper;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.example.api.NovaPoshtaAPI;
import org.example.model.parsed.*;
import org.example.repository.*;

import java.util.*;

@Slf4j
public class GeoDataManager {
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final CityRepository cityRepository;
    private final StreetRepository streetRepository;
    private final AddressHouseRepository addressHouseRepository;

    public GeoDataManager(Random random, Faker faker) {
        this.regionRepository = new RegionRepository();
        this.districtRepository = new DistrictRepository();
        this.cityRepository = new CityRepository();
        this.streetRepository = new StreetRepository(random, faker);
        this.addressHouseRepository = new AddressHouseRepository();
    }

    public void importGeography(NovaPoshtaAPI api) {
        log.info("=== ПОЧАТОК ІМПОРТУ ГЕОГРАФІЇ ===");
        long start = System.currentTimeMillis();

        try {
            log.info(">>> [1/5] Завантаження областей...");
            List<ParsedRegion> regions = api.getAreas();
            regionRepository.saveRegions(regions);
            Map<String, Integer> regionMap = regionRepository.getRegionNameIdMap();

            log.info(">>> [2/5] Завантаження міст та формування районів...");
            List<ParsedCity> settlements = api.getSettlements();

            List<ParsedDistrict> districts = extractDistrictsFromCities(settlements);
            districtRepository.saveDistricts(districts, regionMap);
            Map<String, Integer> districtMap = districtRepository.getDistrictNameIdMap(regionMap);

            log.info(">>> [3/5] Збереження міст у БД...");
            cityRepository.saveCities(settlements, districtMap);
            Map<String, Object> smartCityMap = cityRepository.getCityMap();
            Map<String, Map<String, Integer>> smartMap = (Map<String, Map<String, Integer>>) smartCityMap.get("smart");
            log.info("Завантажено розумну мапу міст: {} унікальних назв у областях", smartMap.size());

            log.info(">>> [4/5] Генерація та збереження вулиць...");
            List<ParsedStreet> streets = streetRepository.generateStreets(settlements, smartCityMap);

            log.info("Згенеровано вулиць: {}", streets.size());
            streetRepository.saveStreets(streets);

            log.info(">>> [5/5] Генерація будинків...");
            Map<Integer, List<Integer>> cityStreetMap = streetRepository.getCityStreetMap();
            log.info("Завантажено мапу місто->вулиці для {} міст", cityStreetMap.size());

            addressHouseRepository.saveHouses(cityStreetMap, 10, 50);

            long duration = (System.currentTimeMillis() - start) / 1000;
            log.info("=== ІМПОРТ ГЕОГРАФІЇ ЗАВЕРШЕНО ({} с.) ===", duration);
        } catch (Exception e) {
            log.error("КРИТИЧНА ПОМИЛКА ПРИ ІМПОРТІ ГЕОГРАФІЇ", e);
        }
    }

    private List<ParsedDistrict> extractDistrictsFromCities(List<ParsedCity> cities) {
        Map<String, ParsedDistrict> uniqueDistricts = new HashMap<>();

        for (ParsedCity city : cities) {
            String regionName = city.getRegion();
            String rawDistrict = city.getArea();

            String districtName = (rawDistrict == null || rawDistrict.isBlank()) ? city.getDescription() : rawDistrict;
            String fixedName = CityRepository.prepareDistrictName(districtName);

            String key = regionName + "_" + fixedName;

            if (!uniqueDistricts.containsKey(key)) {
                ParsedDistrict district = new ParsedDistrict();
                district.setName(fixedName);
                district.setRegionName(regionName);
                uniqueDistricts.put(key, district);
            }
        }
        return new ArrayList<>(uniqueDistricts.values());
    }
}