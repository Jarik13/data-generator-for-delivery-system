package org.example.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.model.parsed.ParsedCargoType;
import org.example.model.parsed.ParsedPack;
import org.example.repository.BoxRepository;
import org.example.repository.ParcelTypeRepository;
import org.example.repository.StaticDataRepository;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ClassifierDataService {
    private final BoxRepository boxRepository;
    private final ParcelTypeRepository parcelTypeRepository;
    private final StaticDataRepository staticDataRepository;

    public void importClassifiers(NovaPoshtaAPI api) {
        log.info("=== ПОЧАТОК ІМПОРТУ КЛАСИФІКАТОРІВ ===");

        try {
            log.info(">>> [1/3] Завантаження статичних довідників...");
            staticDataRepository.saveAll();

            log.info(">>> [2/3] Завантаження типів вантажу (Parcel Types)...");
            List<ParsedCargoType> cargoTypes = api.getCargoTypes();
            parcelTypeRepository.saveParcelTypes(cargoTypes);

            log.info(">>> [3/3] Завантаження типів пакування...");
            List<ParsedPack> packs = api.getPackList();
            boxRepository.saveBoxData(packs);
        } catch (Exception e) {
            log.error("ПОМИЛКА ПРИ ІМПОРТІ КЛАСИФІКАТОРІВ", e);
        }

        log.info("=== ІМПОРТ КЛАСИФІКАТОРІВ ЗАВЕРШЕНО ===");
    }
}