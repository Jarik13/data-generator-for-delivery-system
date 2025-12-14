package org.example.manager;

import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.manager.helper.GeoDataManager;

@Slf4j
public class DataManager {
    private final GeoDataManager geoDataManager;

    public DataManager() {
        this.geoDataManager = new GeoDataManager();
    }

    public void importAllData(NovaPoshtaAPI api) {
        log.info("=== ЗАПУСК ГЛОБАЛЬНОГО МЕНЕДЖЕРА ДАНИХ ===");
        geoDataManager.importGeography(api);
        log.info("=== ГЛОБАЛЬНИЙ ПРОЦЕС ЗАВЕРШЕНО ===");
    }
}