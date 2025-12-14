package org.example;

import org.example.api.NovaPoshtaAPI;
import org.example.config.DatabaseConfig;
import org.example.manager.DataManager;

public class Application {
    public static void main(String[] args) {
        System.out.println("=== ЗАПУСК ПРОГРАМИ ===");

        NovaPoshtaAPI api = new NovaPoshtaAPI();

        DataManager dataManager = new DataManager();

        dataManager.importFullGeography(api);

        DatabaseConfig.close();
        System.out.println("=== ПРОГРАМА ЗАВЕРШЕНА ===");
    }
}