package com.SSO;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SsoEnvironmentConfig {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input =
                     SsoEnvironmentConfig.class
                             .getClassLoader()
                             .getResourceAsStream("application.properties")) {

            if (input == null) {
                throw new RuntimeException(
                        "application.properties not found"
                );
            }

            properties.load(input);

        } catch (IOException e) {
            throw new RuntimeException(
                    "Unable to load application.properties", e
            );
        }
    }

    public static String getEnvironment() {

        String environment =
                System.getProperty(
                        "sso.environment",
                        properties.getProperty(
                                "sso.environment",
                                "TEST"
                        )
                );

        return environment.toUpperCase();
    }

    public static String getUrl() {

        String environment = getEnvironment();

        return properties.getProperty(
                "sso." + environment + ".url"
        );
    }
}