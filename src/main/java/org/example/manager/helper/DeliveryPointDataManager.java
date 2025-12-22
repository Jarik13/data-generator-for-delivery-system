package org.example.manager.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.api.NovaPoshtaAPI;
import org.example.model.DeliveryPoint;
import org.example.repository.DeliveryPointRepository;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class DeliveryPointDataManager {
    private final DeliveryPointRepository deliveryPointRepository;

    public void importDeliveryPoints(NovaPoshtaAPI api, Map<String, Integer> cityMap) {
        log.info("=== ПОЧАТОК ЗАВАНТАЖЕННЯ ТОЧОК ДОСТАВКИ ===");
        long start = System.currentTimeMillis();

        try {
            log.info(">>> Запит до API Нової Пошти...");
            List<DeliveryPoint> points = api.getDeliveryPoints();

            log.info("Успішно отримано точок доставки з API: {}", points.size());

            deliveryPointRepository.saveDeliveryPoints(points, cityMap);

            long duration = (System.currentTimeMillis() - start) / 1000;
            log.info("=== ЗАВАНТАЖЕННЯ ТОЧОК ДОСТАВКИ ЗАВЕРШЕНО ({} с.) ===", duration);
        } catch (Exception e) {
            log.error("ПОМИЛКА ПРИ ОТРИМАННІ ТОЧОК ДОСТАВКИ", e);
        }
    }
}