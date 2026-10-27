package com.BRP;

import com.stipter.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NpsLk01DatabaseValidator {

    private static final String SELECT_BY_BSN = """
            SELECT
                id,
                bsn,
                controlledby_userid,
                controltype,
                modificationdate,
                expectedcontroltype,
                org_zipcode,
                new_zipcode,
                org_city,
                new_city,
                org_number,
                new_number,
                org_street,
                new_street
            FROM public.ba_gba_modified
            WHERE bsn = ?
            ORDER BY id DESC
            """;

    public void validate(Map<String, String> excelData, NpsLk01ValidationResult result) {
        String bsn = required(excelData, "sleutelOntvangend");
        String postcode = required(excelData, "postcode");
        String city = required(excelData, "woonplaatsNaam");
        String houseNumber = required(excelData, "huisnummer");
        // ${straatnaam} is used in the dynamic <gor.straatnaam> XML element.
        String street = required(excelData, "straatnaam");

        try {
            List<DatabaseRecord> records = findRecords(bsn);
            result.addValidation(
                    "Database record exists",
                    "Record for BSN " + bsn,
                    records.isEmpty() ? "No record found" : records.size() + " record(s) found",
                    !records.isEmpty(),
                    records.isEmpty() ? "No ba_gba_modified record found for the submitted BSN." : ""
            );

            if (records.isEmpty()) {
                return;
            }

            List<DatabaseRecord> exactMatches = records.stream()
                    .filter(record -> matches(record, bsn, postcode, city, houseNumber, street))
                    .toList();

            DatabaseRecord selected = selectRecord(records, exactMatches, result);
            if (selected == null) {
                return;
            }

            result.setDatabaseRecordId(selected.id());
            compare(result, "bsn", bsn, selected.bsn(), false);
            compare(result, "new_zipcode", postcode, selected.newZipcode(), true);
            compare(result, "new_city", city, selected.newCity(), false);
            compare(result, "new_number", houseNumber, selected.newNumber(), false);
            compare(result, "new_street", street, selected.newStreet(), false);
            result.addValidation(
                    "expectedcontroltype",
                    "Captured only - no Excel expected value",
                    nullToEmpty(selected.expectedControlType()),
                    true,
                    ""
            );
        } catch (Exception exception) {
            result.addFailure("Database validation exception", usefulMessage(exception));
        }
    }

    private List<DatabaseRecord> findRecords(String bsn) throws Exception {
        List<DatabaseRecord> records = new ArrayList<>();

        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_BY_BSN)) {

            statement.setLong(1, parseBsn(bsn));

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(new DatabaseRecord(
                            resultSet.getLong("id"),
                            resultSet.getString("bsn"),
                            resultSet.getString("expectedcontroltype"),
                            resultSet.getString("new_zipcode"),
                            resultSet.getString("new_city"),
                            resultSet.getString("new_number"),
                            resultSet.getString("new_street")
                    ));
                }
            }
        }

        return records;
    }

    private DatabaseRecord selectRecord(List<DatabaseRecord> records,
                                        List<DatabaseRecord> exactMatches,
                                        NpsLk01ValidationResult result) {
        if (exactMatches.size() == 1) {
            DatabaseRecord selected = exactMatches.get(0);
            result.addValidation(
                    "Database record selection",
                    "Exactly one record matching submitted address",
                    "ID " + selected.id(),
                    true,
                    ""
            );
            return selected;
        }

        if (records.size() == 1) {
            DatabaseRecord selected = records.get(0);
            result.addValidation(
                    "Database record selection",
                    "Single record for submitted BSN",
                    "ID " + selected.id(),
                    true,
                    ""
            );
            return selected;
        }

        if (!exactMatches.isEmpty()) {
            DatabaseRecord selected = exactMatches.stream()
                    .max(java.util.Comparator.comparingLong(DatabaseRecord::id))
                    .orElseThrow();
            result.addValidation(
                    "Database record selection",
                    "Newest matching record after XML submission",
                    "ID " + selected.id() + " from candidate IDs: " + candidateIds(exactMatches),
                    true,
                    "Selected by highest database ID because modificationdate is not populated."
            );
            return selected;
        }

        result.addValidation(
                "Database record selection",
                "One unique matching record",
                "Candidate IDs: " + candidateIds(records),
                false,
                "No database record matches all submitted address values."
        );
        return null;
    }

    private String candidateIds(List<DatabaseRecord> records) {
        return records.stream()
                .map(record -> String.valueOf(record.id()))
                .collect(Collectors.joining(", "));
    }

    private boolean matches(DatabaseRecord record, String bsn, String postcode,
                            String city, String houseNumber, String street) {
        return equal(record.bsn(), bsn, false)
                && equal(record.newZipcode(), postcode, true)
                && equal(record.newCity(), city, false)
                && equal(record.newNumber(), houseNumber, false)
                && equal(record.newStreet(), street, false);
    }

    private void compare(NpsLk01ValidationResult result, String field,
                         String expected, String actual, boolean postcode) {
        boolean passed = equal(actual, expected, postcode);
        result.addValidation(
                field,
                nullToEmpty(expected),
                nullToEmpty(actual),
                passed,
                passed ? "" : field + " mismatch."
        );
    }

    private boolean equal(String actual, String expected, boolean postcode) {
        String normalizedActual = normalize(actual, postcode);
        String normalizedExpected = normalize(expected, postcode);
        return normalizedActual.equalsIgnoreCase(normalizedExpected);
    }

    private String normalize(String value, boolean postcode) {
        String normalized = nullToEmpty(value).trim();
        return postcode ? normalized.replaceAll("\\s+", "") : normalized;
    }

    private String required(Map<String, String> data, String column) {
        String value = data.get(column);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing Excel value for column: " + column);
        }
        return value.trim();
    }

    private long parseBsn(String bsn) {
        try {
            return Long.parseLong(bsn);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Excel sleutelOntvangend must be numeric: " + bsn, exception);
        }
    }

    private String usefulMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record DatabaseRecord(
            long id,
            String bsn,
            String expectedControlType,
            String newZipcode,
            String newCity,
            String newNumber,
            String newStreet
    ) {
    }
}
