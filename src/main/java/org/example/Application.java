package org.example;

import org.example.api.NovaPoshtaAPI;
import org.example.config.DatabaseConfig;
import org.example.manager.DataManager;

public class Application {
    public static void main(String[] args) {
        NovaPoshtaAPI api = new NovaPoshtaAPI();
        DataManager dataManager = new DataManager();

        dataManager.importAllData(api);

        DatabaseConfig.close();
    }
}