package com.qa.util;

import com.opencsv.CSVReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvReader {

    public List<Map<String, String>> getData(String csvFilePath) throws IOException {
        File csvFile = new File(csvFilePath);
        if (!csvFile.exists()) {
            throw new IOException("CSV file not found at: " + csvFilePath);
        }

        List<Map<String, String>> csvRows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            String[] headers = null;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                // Split CSV by comma, preserving empty strings
                String[] values = line.split(",", -1);

                if (lineNumber == 0) {
                    headers = values;
                } else {
                    LinkedHashMap<String, String> rowMap = new LinkedHashMap<>();
                    for (int i = 0; i < headers.length; i++) {
                        String header = headers[i].trim();
                        String value = (i < values.length) ? values[i].trim() : "";
                        rowMap.put(header, value);
                    }
                    csvRows.add(rowMap);
                }
                lineNumber++;
            }
        }

        return csvRows;
    }
}
//    public static void main(String[] args) {
//        CSVReader reader = new CSVReader();
//        String path = "C:/path/to/your/file.csv";
//
//        try {
//            List<Map<String, String>> data = reader.getData(path);
//            for (Map<String, String> row : data) {
//                System.out.println(row);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
