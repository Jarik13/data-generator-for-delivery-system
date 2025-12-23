package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.service.helper.*;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DataService {
    private final GeoDataService geoDataService;
    private final DeliveryPointDataService deliveryPointDataService;
    private final ClassifierDataService classifierDataService;
    private final PersonDataService personDataService;

    public void importAllData(NovaPoshtaAPI api) {
        log.info("=== ЗАПУСК ГЛОБАЛЬНОГО МЕНЕДЖЕРА ДАНИХ ===");

        classifierDataService.importClassifiers(api);

        geoDataService.importGeography(api);

        Map<String, Object> smartCityMap = geoDataService.getCityMap();
        deliveryPointDataService.importDeliveryPoints(api, smartCityMap);

        personDataService.generateAllPeople();

        log.info("=== ГЛОБАЛЬНИЙ ПРОЦЕС ЗАВЕРШЕНО ===");
    }
}