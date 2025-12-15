package org.example.manager;

import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.manager.helper.ClassifierDataManager;
import org.example.manager.helper.DeliveryPointDataManager;
import org.example.manager.helper.GeoDataManager;
import org.example.manager.helper.PersonDataManager;

import java.util.Map;
import java.util.Random;

@Slf4j
public class DataManager {
    private final Random random = new Random();

    private final GeoDataManager geoDataManager;
    private final DeliveryPointDataManager deliveryPointDataManager;
    private final ClassifierDataManager classifierDataManager;
    private final PersonDataManager personDataManager;

    public DataManager() {
        this.geoDataManager = new GeoDataManager();
        this.deliveryPointDataManager = new DeliveryPointDataManager(random);
        this.classifierDataManager = new ClassifierDataManager(random);
        this.personDataManager = new PersonDataManager();
    }

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