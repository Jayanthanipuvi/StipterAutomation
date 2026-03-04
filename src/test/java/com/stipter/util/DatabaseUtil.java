package com.stipter.util;



import com.qa.util.PropertiesFileReader;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseUtil {

    private static String jdbcUrl;
    private static String jdbcUsername;
    private static String jdbcPassword;

    static {
        try {
            jdbcUrl = PropertiesFileReader.getProperty("jdbcUrl");
            jdbcUsername = PropertiesFileReader.getProperty("jdbcusername");
            jdbcPassword = PropertiesFileReader.getProperty("jdbcpassword");
        } catch (Exception e) {
            throw new RuntimeException("❌ Error loading DB properties: " + e.getMessage(), e);
        }
    }

    /**
     * Get a new JDBC connection using the properties file.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
    }
}