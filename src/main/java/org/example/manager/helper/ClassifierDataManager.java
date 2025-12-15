package org.example.manager.helper;

import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.model.parsed.ParsedPack;
import org.example.repository.BoxRepository;

import java.util.List;
import java.util.Random;

@Slf4j
public class ClassifierDataManager {
    private final BoxRepository boxRepository;

    public ClassifierDataManager(Random random) {
        this.boxRepository = new BoxRepository(random);
    }

    public void importClassifiers(NovaPoshtaAPI api) {
        log.info("=== ПОЧАТОК ІМПОРТУ КЛАСИФІКАТОРІВ ===");

        try {
            log.info(">>> [1/1] Завантаження типів пакування (Box Types)...");

            List<ParsedPack> packs = api.getPackList();
            log.info("Отримано типів пакування з API: {}", packs.size());

            boxRepository.saveBoxData(packs);

        } catch (Exception e) {
            log.error("ПОМИЛКА ПРИ ІМПОРТІ КЛАСИФІКАТОРІВ", e);
        }

        log.info("=== ІМПОРТ КЛАСИФІКАТОРІВ ЗАВЕРШЕНО ===");
    }
}