package org.example.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.model.parsed.*;
import org.example.repository.*;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class GeoDataService {
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final CityRepository cityRepository;
    private final StreetRepository streetRepository;
    private final AddressHouseRepository addressHouseRepository;
    private final AddressRepository addressRepository;

    public void importGeography(NovaPoshtaAPI api) {
        log.info("=== ПОЧАТОК ІМПОРТУ ГЕОГРАФІЇ ===");
        long start = System.currentTimeMillis();

        try {
            log.info(">>> [1/6] Обробка областей...");
            List<ParsedRegion> regions = api.getAreas();
            regionRepository.saveRegions(regions);
            Map<String, Integer> regionMap = regionRepository.getRegionNameIdMap();

            log.info(">>> [2/6] Завантаження міст з API та формування районів...");
            List<ParsedCity> settlements = api.getSettlements();
            List<ParsedDistrict> districts = extractDistrictsFromCities(settlements);
            districtRepository.saveDistricts(districts, regionMap);
            Map<String, Integer> districtMap = districtRepository.getDistrictNameIdMap(regionMap);

            log.info(">>> [3/6] Збереження міст у БД...");
            cityRepository.saveCities(settlements, districtMap);

            log.info(">>> [4/6] Генерація вулиць на основі даних з БД...");
            Map<Integer, String> cityData = cityRepository.getAllCityDataFromDb();
            List<ParsedStreet> streets = streetRepository.generateStreets(cityData);
            streetRepository.saveStreets(streets);

            log.info(">>> [5/6] Генерація номерів будинків...");
            Map<Integer, List<Integer>> cityStreetMap = streetRepository.getCityStreetMap();
            addressHouseRepository.saveHouses(cityStreetMap, 10, 50);

            log.info(">>> [6/6] Генерація повних адрес (квартир) на основі будинків...");
            int[] houseRange = addressRepository.getHouseIdRange();
            addressRepository.saveAddresses(houseRange[0], houseRange[1]);

            long duration = (System.currentTimeMillis() - start) / 1000;
            log.info("=== ІМПОРТ ГЕОГРАФІЇ ЗАВЕРШЕНО ({} с.) ===", duration);
        } catch (Exception e) {
            log.error("КРИТИЧНА ПОМИЛКА ПРИ ІМПОРТІ ГЕОГРАФІЇ", e);
        }
    }

    public Map<String, Object> getCityMap() {
        return cityRepository.getSmartCityMap();
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