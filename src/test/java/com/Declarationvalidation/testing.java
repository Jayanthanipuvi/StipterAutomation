package com.Declarationvalidation;

import com.qa.util.ExcelReader;
import com.qa.util.PropertiesFileReader;
import com.stipter.util.DatabaseUtil;
import io.cucumber.java.en.Given;


import java.sql.*;
import java.util.*;

public class testing {


    List<Map<String, String>> data;

    @Given("need excute query and check {int}")
    public void need_excute_query_and_check(Integer rowNumber) {

        try {
            System.out.println("✅ Starting need_execute_query_and_compare_row with RowNumber: " + rowNumber);

            // 1️⃣ Load CSV data
            String csvFilePath = PropertiesFileReader.getProperty("Declaration.csv");
            ExcelReader reader = new ExcelReader();
            List<Map<String, String>> csvData = reader.getData(csvFilePath, "testing");

            if (rowNumber < 0 || rowNumber >= csvData.size()) {
                throw new RuntimeException("❌ Invalid rowNumber: " + rowNumber);
            }
            Map<String, String> csvRow = csvData.get(rowNumber);
            System.out.println("\n✅ Selected CSV Row " + rowNumber + ": " + csvRow);

            // 2️⃣ Define expected fields
            List<String> fields = Arrays.asList(
                    "Toewijzingsnr",
                    "StartPeriode",
                    "EindPeriode",
                    "ProductCode",
                    "ProductNaam",
                    "Tarief(excl)",
                    "#Eenheden",
                    "Rejectreason Id",
                    "Rejectreason Name",
                    "Rejectreason ggkretourcode",
                    "Invoiceline rejectreasonremark"
            );

            // 3️⃣ Read SQL query from .properties
            String query = PropertiesFileReader.getProperty("query.for.testing");
            Map<String, String> dbRow = new HashMap<>();

            // 4️⃣ Execute query
            try (Connection conn = DatabaseUtil.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {

                if (!rs.next()) {
                    System.out.println("⚠️ No data returned from DB for this query.");
                } else {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = meta.getColumnLabel(i);
                        String value = rs.getString(i);
                        dbRow.put(columnName, value == null ? "" : value.trim());
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException("❌ Error executing SQL query: " + e.getMessage(), e);
            }

            // 5️⃣ Print comparison table
            System.out.println();
            System.out.println("-----------------------------------------------------------------------------------------------");
            System.out.printf("| %-35s | %-30s | %-30s | %-7s |%n", "Field", "CSV", "DB", "Match?");
            System.out.println("-----------------------------------------------------------------------------------------------");

            for (String field : fields) {
                String csvValue = csvRow.getOrDefault(field, "").trim();
                String dbValue = dbRow.getOrDefault(field, "").trim();
                boolean match = normalize(csvValue).equalsIgnoreCase(normalize(dbValue));

                System.out.printf("| %-35s | %-30s | %-30s | %-7s |%n",
                        field,
                        csvValue,
                        dbValue,
                        match ? "✔" : "❌");
            }

            System.out.println("-----------------------------------------------------------------------------------------------");
            System.out.println("✅ Comparison completed.\n");

        } catch (Exception e) {
            throw new RuntimeException("❌ Error in need_execute_query_and_compare_row: " + e.getMessage(), e);
        }
    }

    /**
     * Utility to normalize values for comparison.
     */
    private String normalize(String value) {
        if (value == null) return "";
        return value.trim().replace(",", "."); // handle decimal separator
    }
}