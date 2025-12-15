package org.example.manager.helper;

import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.model.parsed.ParsedCargoType;
import org.example.model.parsed.ParsedPack;
import org.example.repository.BoxRepository;
import org.example.repository.ParcelTypeRepository;

import java.util.List;
import java.util.Random;

@Slf4j
public class ClassifierDataManager {
    private final BoxRepository boxRepository;
    private final ParcelTypeRepository parcelTypeRepository;

    public ClassifierDataManager(Random random) {
        this.boxRepository = new BoxRepository(random);
        this.parcelTypeRepository = new ParcelTypeRepository();
    }

    public void importClassifiers(NovaPoshtaAPI api) {
        log.info("=== ПОЧАТОК ІМПОРТУ КЛАСИФІКАТОРІВ ===");

        try {
            log.info(">>> [1/2] Завантаження типів вантажу (Parcel Types)...");
            List<ParsedCargoType> cargoTypes = api.getCargoTypes();
            parcelTypeRepository.saveParcelTypes(cargoTypes);

            log.info(">>> [2/2] Завантаження типів пакування...");
            List<ParsedPack> packs = api.getPackList();
            boxRepository.saveBoxData(packs);
        } catch (Exception e) {
            log.error("ПОМИЛКА ПРИ ІМПОРТІ КЛАСИФІКАТОРІВ", e);
        }

        log.info("=== ІМПОРТ КЛАСИФІКАТОРІВ ЗАВЕРШЕНО ===");
    }
}