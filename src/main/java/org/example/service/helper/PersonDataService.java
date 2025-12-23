package org.example.service.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.repository.PersonRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@RequiredArgsConstructor
public class PersonDataService {
    private final Random random;
    private final PersonRepository personRepository;

    private static final int EMPLOYEES_PER_BRANCH_MIN = 5;
    private static final int EMPLOYEES_PER_BRANCH_MAX = 10;

    private static final int CLIENTS_MIN = 800_000;
    private static final int CLIENTS_MAX = 1_200_000;

    private static final int DRIVERS_MIN = 20_000;
    private static final int DRIVERS_MAX = 40_000;

    private static final int COURIERS_MIN = 30_000;
    private static final int COURIERS_MAX = 75_000;

    public void generateAllPeople() {
        System.out.println("=== ПОЧАТОК ГЕНЕРАЦІЇ ЛЮДЕЙ ===");
        log.info("=== ПОЧАТОК ГЕНЕРАЦІЇ ЛЮДЕЙ ===");
        long start = System.currentTimeMillis();

        int clientsCount = generateRandomCount(CLIENTS_MIN, CLIENTS_MAX);
        int driversCount = generateRandomCount(DRIVERS_MIN, DRIVERS_MAX);
        int couriersCount = generateRandomCount(COURIERS_MIN, COURIERS_MAX);

        System.out.println(">>> [1/4] Завантаження відділень...");
        List<Integer> branchIds = personRepository.getAllBranchIds();
        System.out.println("Знайдено відділень: " + branchIds.size());

        if (branchIds.isEmpty()) {
            System.out.println("УВАГА: Таблиця branches порожня! Співробітники не будуть створені.");
        } else {
            int totalEmployees = 0;
            List<BranchEmployees> branchEmployeesList = new ArrayList<>();

            for (Integer branchId : branchIds) {
                int employeesInBranch = generateRandomCount(
                        EMPLOYEES_PER_BRANCH_MIN,
                        EMPLOYEES_PER_BRANCH_MAX
                );
                totalEmployees += employeesInBranch;
                branchEmployeesList.add(new BranchEmployees(branchId, employeesInBranch));
            }

            System.out.printf("  Загальна кількість працівників: %d%n", totalEmployees);
            generateEmployeesForBranches(branchEmployeesList);
        }

        System.out.println("\n>>> [2/4] Генерація водіїв...");
        System.out.println("Кількість водіїв: " + driversCount);
        personRepository.saveDrivers(driversCount);

        System.out.println("\n>>> [3/4] Генерація кур'єрів...");
        System.out.println("Кількість кур'єрів: " + couriersCount);
        personRepository.saveCouriers(couriersCount);

        System.out.println("\n>>> [4/4] Генерація клієнтів (це займе час)...");
        System.out.println("Кількість клієнтів: " + String.format("%,d", clientsCount));
        personRepository.saveClients(clientsCount);

        long duration = (System.currentTimeMillis() - start) / 1000;
        System.out.println("\n=== ГЕНЕРАЦІЮ ЛЮДЕЙ ЗАВЕРШЕНО (" + duration + " с.) ===");
    }

    private void generateEmployeesForBranches(List<BranchEmployees> branchEmployeesList) {
        System.out.println("\n>>> Генерація працівників для відділень...");

        int startIndex = 0;
        for (BranchEmployees be : branchEmployeesList) {
            List<Integer> singleBranchList = new ArrayList<>();
            for (int i = 0; i < be.employeeCount; i++) {
                singleBranchList.add(be.branchId);
            }

            personRepository.saveEmployees(be.employeeCount, singleBranchList, startIndex);
            startIndex += be.employeeCount;

            System.out.printf("  Відділення %d: згенеровано %d працівників (startIndex: %d)%n",
                    be.branchId, be.employeeCount, startIndex - be.employeeCount);
        }
    }

    private int generateRandomCount(int min, int max) {
        return random.nextInt(max - min + 1) + min;
    }

    private record BranchEmployees(int branchId, int employeeCount) { }
}