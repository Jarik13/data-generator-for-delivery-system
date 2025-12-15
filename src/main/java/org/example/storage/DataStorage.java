package org.example.storage;

import org.example.model.item.StorageConditionItem;
import org.example.model.item.WorkTimeIntervalItem;

import java.util.List;

public class DataStorage {
    public static final List<String> SHIPMENT_TYPES = List.of(
            "Стандартна",
            "Експрес"
    );

    public static final List<String> SHIPMENT_STATUSES = List.of(
            "Створено",
            "Очікує надходження",
            "Прийнято у відділенні",
            "Сортування термінал",
            "У дорозі",
            "Прибув у відділення",
            "Видано кур'єру",
            "Доставлено",
            "Відмова",
            "Втрачено",
            "Утилізовано"
    );

    public static final List<String> PAYMENT_TYPES = List.of(
            "Готівка",
            "Картка",
            "Онлайн-оплата",
            "Безготівковий розрахунок",
            "Післяплата (накладений платіж)"
    );

    public static final List<String> RETURN_REASONS = List.of(
            "Відмова отримувача",
            "Закінчився термін зберігання",
            "Пошкодження вантажу",
            "Невірна адреса",
            "Отримувач не знайдений",
            "Помилкове відправлення"
    );

    public static final List<StorageConditionItem> STORAGE_CONDITIONS = List.of(
            new StorageConditionItem(
                    "Звичайне зберігання",
                    "Стандартне складське приміщення, температура +15...+25°C, вологість до 70%."
            ),
            new StorageConditionItem(
                    "Температурний режим",
                    "Холодильна камера з контрольованою температурою +2...+8°C (ліки, продукти)."
            ),
            new StorageConditionItem(
                    "Морозильна камера",
                    "Глибока заморозка з температурою нижче -18°C."
            ),
            new StorageConditionItem(
                    "Сухий склад",
                    "Приміщення з пониженою вологістю (не більше 40%) для паперу, електроніки, сипучих речовин."
            ),
            new StorageConditionItem(
                    "Захист від світла",
                    "Темне приміщення для фотоматеріалів та хімічних речовин, чутливих до ультрафіолету."
            ),
            new StorageConditionItem(
                    "ADR (Небезпечні вантажі)",
                    "Спеціально обладнаний ізольований сектор з системами пожежогасіння та вентиляції."
            )
    );

    // --- Класифікатори інфраструктури та роботи ---

    public static final List<String> BRANCH_TYPES = List.of(
            "Вантажне відділення",
            "Поштове відділення",
            "Пункт видачі",
            "Сортувальний центр",
            "Міні-відділення"
    );

    public static final List<String> DAYS_OF_WEEK = List.of(
            "Понеділок",
            "Вівторок",
            "Середа",
            "Четвер",
            "П'ятниця",
            "Субота",
            "Неділя"
    );

    // --- Статуси логістичних процесів ---

    public static final List<String> ROUTE_LIST_STATUSES = List.of(
            "Сформовано",
            "Видано кур'єру",
            "У процесі доставки",
            "Завершено",
            "Скасовано"
    );

    public static final List<String> TRIP_STATUSES = List.of(
            "Заплановано",
            "Завантаження",
            "В дорозі",
            "Розвантаження",
            "Завершено",
            "Аварійна зупинка"
    );

    public static final List<String> VEHICLE_ACTIVITY_STATUSES = List.of(
            "Активний (на лінії)",
            "В гаражі",
            "Технічне обслуговування",
            "Ремонт",
            "Списано",
            "Продано"
    );

    // --- Класифікатори автопарку (Fleet) ---

    public static final List<String> FLEET_BRANDS = List.of(
            "Mercedes-Benz",
            "MAN",
            "Scania",
            "Volvo",
            "Renault",
            "Volkswagen",
            "Ford",
            "DAF",
            "Iveco"
    );

    public static final List<String> FLEET_FUEL_TYPES = List.of(
            "Дизель",
            "Бензин",
            "Газ/Бензин",
            "Електро",
            "Гібрид"
    );

    public static final List<String> FLEET_BODY_TYPES = List.of(
            "Суцільнометалевий фургон",
            "Тентований",
            "Ізотермічний",
            "Рефрижератор",
            "Контейнеровоз",
            "Бортовий"
    );

    public static final List<String> FLEET_TRANSMISSION_TYPES = List.of(
            "Механічна",
            "Автоматична",
            "Роботизована",
            "Варіатор"
    );

    public static final List<String> FLEET_DRIVE_TYPES = List.of(
            "Задній",
            "Передній",
            "Повний (4x4)",
            "Колісна формула 6x4"
    );

    public static final List<WorkTimeIntervalItem> WORK_TIME_INTERVALS = List.of(
            new WorkTimeIntervalItem("09:00:00", "18:00:00"),
            new WorkTimeIntervalItem("08:00:00", "17:00:00"),
            new WorkTimeIntervalItem("08:00:00", "20:00:00"),
            new WorkTimeIntervalItem("09:00:00", "21:00:00"),
            new WorkTimeIntervalItem("10:00:00", "19:00:00"),
            new WorkTimeIntervalItem("08:00:00", "22:00:00"),
            new WorkTimeIntervalItem("00:00:00", "23:59:59")
    );
}