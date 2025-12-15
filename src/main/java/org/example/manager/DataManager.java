package org.example.manager;

import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.manager.helper.ClassifierDataManager;
import org.example.manager.helper.DeliveryPointDataManager;
import org.example.manager.helper.GeoDataManager;

import java.util.Random;

@Slf4j
public class DataManager {
    private final Random random = new Random();

    private final GeoDataManager geoDataManager;
    private final DeliveryPointDataManager deliveryPointDataManager;
    private final ClassifierDataManager classifierDataManager;

    public DataManager() {
        this.geoDataManager = new GeoDataManager();
        this.deliveryPointDataManager = new DeliveryPointDataManager(random);
        this.classifierDataManager = new ClassifierDataManager(random);
    }

    public void importAllData(NovaPoshtaAPI api) {
        log.info("=== ЗАПУСК ГЛОБАЛЬНОГО МЕНЕДЖЕРА ДАНИХ ===");

        classifierDataManager.importClassifiers(api);

        // 2. Імпорт географії (розкоментуй, коли потрібно)
        // geoDataManager.importGeography(api);
        // Map<String, Integer> cityMap = geoDataManager.getCityMap();

        // deliveryPointDataManager.importDeliveryPoints(api, cityMap);

        log.info("=== ГЛОБАЛЬНИЙ ПРОЦЕС ЗАВЕРШЕНО ===");
    }
}