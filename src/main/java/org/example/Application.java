package org.example;

import net.datafaker.Faker;
import org.example.api.NovaPoshtaAPI;
import org.example.config.DatabaseConfig;
import org.example.service.DataService;
import org.example.service.helper.*;
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

        ClassifierDataService classifierDataService = new ClassifierDataService(boxRepository,
                parcelTypeRepository, staticDataRepository);
        GeoDataService geoDataService = new GeoDataService(regionRepository, districtRepository,
                cityRepository, streetRepository, addressHouseRepository, addressRepository);
        DeliveryPointDataService deliveryPointDataService = new DeliveryPointDataService(deliveryPointRepository);
        PersonDataService personDataService = new PersonDataService(random, personRepository);


        NovaPoshtaAPI api = new NovaPoshtaAPI();
        DataService dataService = new DataService(geoDataService, deliveryPointDataService, classifierDataService, personDataService);

        dataService.importAllData(api);

        DatabaseConfig.close();
    }
}