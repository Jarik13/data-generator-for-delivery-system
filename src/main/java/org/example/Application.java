package org.example;

import net.datafaker.Faker;
import org.example.api.NovaPoshtaAPI;
import org.example.config.DatabaseConfig;
import org.example.manager.DataManager;
import org.example.manager.helper.ClassifierDataManager;
import org.example.manager.helper.DeliveryPointDataManager;
import org.example.manager.helper.GeoDataManager;
import org.example.manager.helper.PersonDataManager;
import org.example.repository.*;

import java.util.Locale;
import java.util.Random;

public class Application {
    public static void main(String[] args) {
        final Random random = new Random();
        final Faker faker = new Faker(new Locale("uk"));

        RegionRepository regionRepository = new RegionRepository();
        DistrictRepository districtRepository = new DistrictRepository();
        CityRepository cityRepository = new CityRepository();
        StreetRepository streetRepository = new StreetRepository(random, faker);
        AddressHouseRepository addressHouseRepository = new AddressHouseRepository(random);
        AddressRepository addressRepository = new AddressRepository(random);
        DeliveryPointRepository deliveryPointRepository = new DeliveryPointRepository(random);
        ParcelTypeRepository parcelTypeRepository = new ParcelTypeRepository();
        BoxRepository boxRepository = new BoxRepository(random);
        StaticDataRepository staticDataRepository = new StaticDataRepository();
        PersonRepository personRepository = new PersonRepository();

        ClassifierDataManager classifierDataManager = new ClassifierDataManager(boxRepository,
                parcelTypeRepository, staticDataRepository);
        GeoDataManager geoDataManager = new GeoDataManager(regionRepository, districtRepository,
                cityRepository, streetRepository, addressHouseRepository, addressRepository);
        DeliveryPointDataManager deliveryPointDataManager = new DeliveryPointDataManager(deliveryPointRepository);
        PersonDataManager personDataManager = new PersonDataManager(random, personRepository);


        NovaPoshtaAPI api = new NovaPoshtaAPI();
        DataManager dataManager = new DataManager(geoDataManager, deliveryPointDataManager, classifierDataManager, personDataManager);

        dataManager.importAllData(api);

        DatabaseConfig.close();
    }
}