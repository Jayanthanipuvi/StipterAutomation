package com.Product;

import com.stipter.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class ProductDBValidation {

    public ValidationResult validate(
            int productId,
            ProductRequestBuilder.ProductData expected) {
        String sql = """
        SELECT *
        FROM public.product
        WHERE id = ?
        """;

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, productId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return ValidationResult.failed(
                            "No product found for facilitator " + expected.facilitatorId()
                                    + " and name " + expected.productName());
                }

                Map<String, String> row = readRow(resultSet);
                StringBuilder mismatches = new StringBuilder();

                compare(row, "name", expected.productName(), mismatches);
//                compare(row, "facilitatorid", String.valueOf(expected.facilitatorId()), mismatches);
                compare(row, "serviceid", String.valueOf(expected.serviceId()), mismatches);
                compare(row, "vektisproductcode", expected.vektisCode(), mismatches);
                compare(row,
                        "fromdate",
                        convertDate(expected.fromDate()),
                        mismatches);
                compare(row,
                        "tilldate",
                        convertDate(expected.tillDate()),
                        mismatches);



                if (mismatches.length() > 0) {
                    return ValidationResult.failed(mismatches.toString().trim(), row);
                }

                return ValidationResult.passed(row);
            }
        } catch (Exception e) {
            throw new RuntimeException("Product database validation failed", e);
        }


    }

    private Map<String, String> readRow(ResultSet resultSet) throws Exception {
        Map<String, String> row = new LinkedHashMap<>();
        ResultSetMetaData metadata = resultSet.getMetaData();

        for (int index = 1; index <= metadata.getColumnCount(); index++) {
            String column = metadata.getColumnLabel(index).toLowerCase(Locale.ROOT);
            Object value = resultSet.getObject(index);
            row.put(column, value == null ? "" : String.valueOf(value).trim());
        }
        return row;
    }

    private String convertDate(String date) {

        if (date == null || date.isBlank()) {
            return "";
        }

        String[] parts = date.split("-");

        if (parts.length == 3) {
            return parts[2] + "-" + parts[1] + "-" + parts[0];
        }

        return date;
    }

    private void compare(
            Map<String, String> actualRow,
            String column,
            String expected,
            StringBuilder mismatches
    ) {
        if (!actualRow.containsKey(column)) {
            mismatches.append("DB column not found: ").append(column).append('\n');
            return;
        }

        String actual = actualRow.getOrDefault(column, "");
        String normalizedExpected = expected == null ? "" : expected.trim();

        if (!normalizedExpected.equalsIgnoreCase(actual)) {
            mismatches.append(column)
                    .append(" mismatch. Expected=[")
                    .append(normalizedExpected)
                    .append("] Actual=[")
                    .append(actual)
                    .append("]\n");
        }
    }

    public record ValidationResult(boolean passed, String message, Map<String, String> row) {

        static ValidationResult passed(Map<String, String> row) {
            return new ValidationResult(true, "Product row matched expected API payload", row);
        }

        static ValidationResult failed(String message) {
            return new ValidationResult(false, message, Map.of());
        }

        static ValidationResult failed(String message, Map<String, String> row) {
            return new ValidationResult(false, message, row);
        }
    }
}
