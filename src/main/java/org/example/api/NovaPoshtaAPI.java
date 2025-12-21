package org.example.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.DeliveryPoint;
import org.example.model.parsed.*;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class NovaPoshtaAPI {
    private static final String API_URL = "https://api.novaposhta.ua/v2.0/json/";
    private static final String API_KEY = "a6a4aaa5e577bf6fe7661ce523b9c494";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public NovaPoshtaAPI() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public List<ParsedRegion> getAreas() {
        return getDataFromAPI(
                "Address",
                "getAreas",
                "областей",
                1,
                this::parseRegionFromJson
        );
    }

    public List<ParsedCity> getSettlements() {
        return getDataFromAPI(
                "Address",
                "getSettlements",
                "населених пунктів",
                80,
                this::parseCityFromJson
        );
    }

    public List<DeliveryPoint> getDeliveryPoints() {
        return getDataFromAPI(
                "Address",
                "getWarehouses",
                "точок доставки",
                100,
                this::parseDeliveryPointFromJson
        );
    }

    public List<ParsedCargoType> getCargoTypes() {
        return getListNoPagination(
                "Common",
                "getCargoTypes",
                "типів вантажу",
                this::parseCargoTypeFromJson
        );
    }

    public List<ParsedPack> getPackList() {
        return getListNoPagination(
                "Common",
                "getPackList",
                "типів пакування",
                this::parsePackFromJson
        );
    }

    private <T> List<T> getListNoPagination(
            String modelName,
            String methodName,
            String entityName,
            Function<JsonNode, T> parser) {

        List<T> results = new ArrayList<>();
        try {
            String jsonBody = """
                {
                    "apiKey": "%s",
                    "modelName": "%s",
                    "calledMethod": "%s"
                }
                """.formatted(API_KEY, modelName, methodName);

            HttpRequest request = createHttpRequest(jsonBody);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());

            if (root.has("success") && !root.get("success").asBoolean()) {
                System.err.println("API Error (" + entityName + "): " + root.path("errors"));
                return Collections.emptyList();
            }

            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    T parsedItem = parser.apply(item);
                    if (parsedItem != null) {
                        results.add(parsedItem);
                    }
                }
            }
            System.out.printf("=== Всього отримано %d %s (одним запитом) ===%n", results.size(), entityName);

        } catch (Exception e) {
            System.err.println("Exception fetching " + entityName + ": " + e.getMessage());
        }
        return results;
    }

    private <T> List<T> getDataFromAPI(
            String modelName,
            String methodName,
            String entityName,
            int maxPages,
            Function<JsonNode, T> parser) {

        List<T> results = new ArrayList<>();
        int page = 1;
        int emptyPagesCount = 0;
        int limit = 500;

        while (page <= maxPages) {
            try {
                String jsonBody = """
                    {
                        "apiKey": "%s",
                        "modelName": "%s",
                        "calledMethod": "%s",
                        "methodProperties": {
                            "Page": "%d",
                            "Limit": "%d"
                        }
                    }
                    """.formatted(API_KEY, modelName, methodName, page, limit);

                HttpRequest request = createHttpRequest(jsonBody);
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode root = objectMapper.readTree(response.body());

                if (root.has("success") && !root.get("success").asBoolean()) {
                    System.err.println("API Error on page " + page + ": " + root.path("errors"));
                    Thread.sleep(2000);
                    continue;
                }

                JsonNode data = root.path("data");
                if (!data.isArray() || data.isEmpty()) {
                    emptyPagesCount++;
                    if (emptyPagesCount > 1) break;
                } else {
                    emptyPagesCount = 0;
                }

                int itemsAdded = 0;
                for (JsonNode item : data) {
                    T parsedItem = parser.apply(item);
                    if (parsedItem != null) {
                        results.add(parsedItem);
                        itemsAdded++;
                    }
                }

                System.out.println("Сторінка " + page + ": завантажено " + itemsAdded + " " + entityName + ".");

                if (itemsAdded < limit && page > 1) break;

                page++;
                Thread.sleep(200);
            } catch (Exception e) {
                System.err.println("Exception on page " + page + ": " + e.getMessage());
                break;
            }
        }
        System.out.printf("=== Всього отримано %d %s ===%n", results.size(), entityName);
        return results;
    }

    private HttpRequest createHttpRequest(String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    private ParsedRegion parseRegionFromJson(JsonNode node) {
        return new ParsedRegion(node.path("Ref").asText(), node.path("Description").asText());
    }

    private ParsedCity parseCityFromJson(JsonNode node) {
        return new ParsedCity(
                node.path("Description").asText(),
                node.path("AreaDescription").asText(),
                node.path("RegionsDescription").asText(),
                node.path("Ref").asText(),
                node.path("SettlementTypeDescription").asText()
        );
    }

    private DeliveryPoint parseDeliveryPointFromJson(JsonNode node) {
        DeliveryPoint dp = new DeliveryPoint();
        dp.setName(node.path("Description").asText());
        dp.setAddress(node.path("ShortAddress").asText());
        dp.setCity(node.path("CityDescription").asText());
        dp.setRef(node.path("Ref").asText());
        dp.setTypeRef(node.path("TypeOfWarehouse").asText());
        return dp;
    }

    private ParsedCargoType parseCargoTypeFromJson(JsonNode node) {
        ParsedCargoType type = new ParsedCargoType();
        type.setRef(node.path("Ref").asText());
        type.setDescription(node.path("Description").asText());
        return type;
    }

    private ParsedPack parsePackFromJson(JsonNode node) {
        ParsedPack pack = new ParsedPack();
        pack.setRef(node.path("Ref").asText());
        pack.setDescription(node.path("Description").asText());
        pack.setLength(new BigDecimal(node.path("Length").asText("0").isEmpty() ? "0" : node.path("Length").asText("0")));
        pack.setWidth(new BigDecimal(node.path("Width").asText("0").isEmpty() ? "0" : node.path("Width").asText("0")));
        pack.setHeight(new BigDecimal(node.path("Height").asText("0").isEmpty() ? "0" : node.path("Height").asText("0")));
        return pack;
    }
}