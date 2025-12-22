package org.example.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.manager.helper.ClassifierDataManager;
import org.example.manager.helper.DeliveryPointDataManager;
import org.example.manager.helper.GeoDataManager;
import org.example.manager.helper.PersonDataManager;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DataManager {
    private final GeoDataManager geoDataManager;
    private final DeliveryPointDataManager deliveryPointDataManager;
    private final ClassifierDataManager classifierDataManager;
    private final PersonDataManager personDataManager;

    public void importAllData(NovaPoshtaAPI api) {
        log.info("=== ЗАПУСК ГЛОБАЛЬНОГО МЕНЕДЖЕРА ДАНИХ ===");

        classifierDataManager.importClassifiers(api);

        geoDataManager.importGeography(api);

        Map<String, Integer> cityMap = geoDataManager.getCityMap();
        deliveryPointDataManager.importDeliveryPoints(api, cityMap);

        personDataManager.generateAllPeople();

        log.info("=== ГЛОБАЛЬНИЙ ПРОЦЕС ЗАВЕРШЕНО ===");
    }
}