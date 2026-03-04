package com.qa.util;

import java.io.InputStream;
import java.util.Properties;

public class PropertiesFileReader {

    private static Properties properties;
    static {
        try (InputStream input = PropertiesFileReader.class.getClassLoader().getResourceAsStream("application.properties")) {
            properties = new Properties();
            properties.load(input);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

}

