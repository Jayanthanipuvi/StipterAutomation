package com.Product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class ProductRequestBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ProductPayloads buildPayloads(Map<String, String> row, int rowNumber, String runId) {
        try {
            ObjectNode stepOne = readTemplate("product/product-step1.json");
            ObjectNode stepTwo = readTemplate("product/product-step2.json");
            ObjectNode stepThree = readTemplate("product/product-step3.json");

            String baseName = required(row, "productname", rowNumber);
            String productName = baseName + "_" + runId + "_" + rowNumber;

            applyRow(stepOne, row, productName);
            applyRow(stepTwo, row, productName);
            applyRow(stepThree, row, productName);

            ProductData data = getProductData(stepThree.toString(), rowNumber);
            return new ProductPayloads(
                    MAPPER.writeValueAsString(stepOne),
                    MAPPER.writeValueAsString(stepTwo),
                    MAPPER.writeValueAsString(stepThree),
                    data
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to build Product payload for Excel row " + rowNumber, e);
        }
    }

    public ProductData getProductData(String fullPayload, int rowNumber) {
        try {
            JsonNode payload = MAPPER.readTree(fullPayload);
            return new ProductData(
                    rowNumber,
                    payload.path("productName").asText(),
                    payload.path("facilitatorId").asInt(),
                    payload.path("serviceId").asInt(),
                    payload.path("vektisCode").asText(),
                    payload.path("fromDate").asText(),
                    payload.path("tillDate").asText()
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to read Product payload metadata", e);
        }
    }

    private void applyRow(ObjectNode payload, Map<String, String> row, String productName) {
        putText(payload, "productName", productName);
        putText(payload, "description", value(row, "description"));
        putText(payload, "vektisCode", value(row, "productcode"));
        putText(payload, "fromDate", value(row, "Productdurationfrom"));
        putText(payload, "tillDate", value(row, "Productdurationto"));

        putInt(payload, "facilityTypeId", facilityTypeId(value(row, "Werksoort")));
        putInt(payload, "executionVariant", executionVariant(value(row, "Implementation")));
        putInt(payload, "calculateType", calculationMethod(value(row, "Calculationmethod")));
        putInt(payload, "payMethodId", paymentMethod(value(row, "Paymentmethod")));
        putInt(payload, "payFrequencyId", paymentFrequency(value(row, "Paymentfrequency")));
        putInt(payload, "careDateStartRequired", optionalFlag(value(row, "Notification")));
        putInt(payload, "careDateEndRequired", optionalFlag(value(row, "EndofCare")));

        applyProductCost(payload, row);
        applyContractCareFrequency(payload, value(row, "Zorgfrequentie"));
        applyDeliveryUnit(payload, value(row, "Levereenheid"));
    }

    private void applyProductCost(ObjectNode payload, Map<String, String> row) {
        JsonNode costsNode = payload.path("productCosts");
        if (!costsNode.isArray() || costsNode.isEmpty() || !costsNode.get(0).isObject()) {
            return;
        }

        ObjectNode cost = (ObjectNode) costsNode.get(0);
        putInt(cost, "price", amountInCents(value(row, "price")));
        putText(cost, "fromDate", value(row, "durationfrom"));
        putText(cost, "tillDate", value(row, "durationto"));
        putInt(cost, "vatPercentage", parseInteger(value(row, "BTW%"), 0));
        cost.put("active", true);
    }

    private void applyContractCareFrequency(ObjectNode payload, String frequencyName) {
        int activeId = careFrequencyId(frequencyName);
        JsonNode frequencies = payload.path("contractCareFrequencyIds");
        if (!frequencies.isArray()) {
            return;
        }

        for (JsonNode frequency : frequencies) {
            if (frequency.isObject()) {
                ObjectNode item = (ObjectNode) frequency;
                item.put("active", item.path("id").asInt() == activeId);
            }
        }
    }

    private void applyDeliveryUnit(ObjectNode payload, String deliveryName) {
        String activeId = deliveryUnitId(deliveryName);
        JsonNode units = payload.path("serviceDeliveryUnitOverrules");
        if (!units.isArray()) {
            return;
        }

        for (JsonNode unit : units) {
            if (unit.isObject()) {
                ObjectNode item = (ObjectNode) unit;
                boolean active = activeId.equals(item.path("deliveryUnitId").asText());
                item.put("active", active);
                JsonNode supported = item.path("supportedContractCareFrequencies");
                if (supported.isArray()) {
                    markNestedFrequencies((ArrayNode) supported, active);
                }
            }
        }
    }

    private void markNestedFrequencies(ArrayNode frequencies, boolean parentActive) {
        for (JsonNode frequency : frequencies) {
            if (frequency.isObject()) {
                ObjectNode item = (ObjectNode) frequency;
                if (!parentActive) {
                    item.put("active", false);
                } else if (item.path("id").asInt() == 4) {
                    item.put("active", true);
                }
            }
        }
    }

    private ObjectNode readTemplate(String resourcePath) throws Exception {
        return (ObjectNode) MAPPER.readTree(readResource(resourcePath));
    }

    private String readResource(String resourcePath) throws Exception {
        try (InputStream input = ProductRequestBuilder.class.getClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing Product resource: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void putText(ObjectNode node, String field, String value) {
        if (value != null && !value.isBlank()) {
            node.put(field, value.trim());
        }
    }

    private void putInt(ObjectNode node, String field, int value) {
        node.put(field, value);
    }

    private String required(Map<String, String> row, String key, int rowNumber) {
        String value = value(row, key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required column '" + key + "' for row " + rowNumber);
        }
        return value.trim();
    }

    private String value(Map<String, String> row, String key) {
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(key))) {
                return entry.getValue() == null ? "" : entry.getValue().trim();
            }
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[\\s_]", "").toLowerCase(Locale.ROOT);
    }

    private int facilityTypeId(String value) {
        String normalized = normalize(value);
        if (normalized.contains("jeugd")) {
            return 1;
        }
        return 2;
    }

    private int executionVariant(String value) {
        return normalize(value).contains("output") ? 2 : 3;
    }

    private int calculationMethod(String value) {
        return normalize(value).contains("dag") ? 2 : 1;
    }

    private int paymentMethod(String value) {
        return normalize(value).contains("pgb") ? 2 : 1;
    }

    private int paymentFrequency(String value) {
        String normalized = normalize(value);
        if (normalized.contains("week")) {
            return 2;
        }
        if (normalized.contains("jaar")) {
            return 5;
        }
        return 3;
    }

    private int careFrequencyId(String value) {
        String normalized = normalize(value);
        if (normalized.contains("dag")) {
            return 1;
        }
        if (normalized.contains("week") && !normalized.contains("4")) {
            return 2;
        }
        if (normalized.contains("4")) {
            return 3;
        }
        if (normalized.contains("jaar")) {
            return 5;
        }
        if (normalized.contains("totaal")) {
            return 6;
        }
        return 4;
    }

    private String deliveryUnitId(String value) {
        String normalized = normalize(value);
        if (normalized.contains("stuk")) {
            return "11";
        }
        if (normalized.contains("min")) {
            return "20";
        }
        if (normalized.contains("euro")) {
            return "30";
        }
        if (normalized.contains("dagdeel")) {
            return "40";
        }
        if (normalized.contains("etmaal")) {
            return "42";
        }
        return "21";
    }

    private int optionalFlag(String value) {
        String normalized = normalize(value);
        return normalized.contains("verplicht") || normalized.equals("ja") || normalized.equals("yes") ? 1 : 0;
    }

    private int amountInCents(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        String normalized = value.replace(",", ".").trim();
        return new BigDecimal(normalized)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    private int parseInteger(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return new BigDecimal(value.replace(",", ".").trim()).intValue();
    }

    public record ProductPayloads(
            String stepOnePayload,
            String stepTwoPayload,
            String stepThreePayload,
            ProductData productData
    ) {
    }

    public record ProductData(
            int rowNumber,
            String productName,
            int facilitatorId,
            int serviceId,
            String vektisCode,
            String fromDate,
            String tillDate
    ) {
    }
}
