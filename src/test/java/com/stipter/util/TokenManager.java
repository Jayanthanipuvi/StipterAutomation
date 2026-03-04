package com.stipter.util;

import com.qa.factory.GlobalDriverConfig;
import com.qa.util.PropertiesFileReader;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.util.HashMap;
import java.util.Map;

public class TokenManager {

    private static String accessToken;           // for KEYCLOAK
    private static String xUserToken;            // for COOKIE_MODE
    private static String cookieHeader;          // for COOKIE_MODE

    private static Response lastErrorResponse;
    private static Response lastSuccessResponse;

    // ---- PUBLIC: one place to get headers for any auth mode ----
    public static Map<String, String> getAuthHeaders() {
        AuthMode mode = AuthMode.from(PropertiesFileReader.getProperty("auth.mode"));
        Map<String, String> headers = new HashMap<>();

        if (mode == AuthMode.KEYCLOAK) {
            String token = getAccessToken();
            if (token != null && !token.isEmpty()) {
                headers.put("Authorization", "Bearer " + token);
            }
        } else {
            headers.put("x-user-token", getXUserToken());
            headers.put("Cookie", getCookies());
        }
        return headers;
    }

    // ------------ COOKIE_MODE getters -------------
    private static String getXUserToken() {
        if (xUserToken == null) {
            xUserToken = PropertiesFileReader.getProperty("auth.x_user_token");
        }
        return xUserToken;
    }

    private static String getCookies() {
        if (cookieHeader == null) {
            cookieHeader = PropertiesFileReader.getProperty("auth.cookies");
        }
        return cookieHeader;
    }

    // ------------ KEYCLOAK (existing) -------------
    public static String getAccessToken() {
        if (accessToken == null) {
            accessToken = generateAccessToken();
        }
        return accessToken;
    }

    public static String generateAccessToken() {
        String username = PropertiesFileReader.getProperty("swagger.username");
        String password = PropertiesFileReader.getProperty("swagger.password");
        String url = PropertiesFileReader.getProperty("swagger.token.url");
        return generateAccessToken(username, password, url);
    }

    public static String generateAccessToken(String username, String password, String url) {
        try {
            GlobalDriverConfig.logger.info("Authenticating to URL: {}", url);
            Map<String, String> loginPayload = new HashMap<>();
            loginPayload.put("username", username);
            loginPayload.put("password", password);
            loginPayload.put("grant_type", PropertiesFileReader.getProperty("swagger.grant_type"));
            loginPayload.put("client_id", PropertiesFileReader.getProperty("swagger.client_id"));
            loginPayload.put("client_secret", PropertiesFileReader.getProperty("swagger.client_secret"));

            RestAssured.baseURI = url;
            Response response = RestAssured.given()
                    .contentType(ContentType.URLENC)
                    .formParams(loginPayload)
                    .post();

            int statusCode = response.getStatusCode();
            GlobalDriverConfig.logger.info("Auth response status code: {}", statusCode);

            if (statusCode == 200) {
                String token = response.jsonPath().getString("access_token");
                if (token != null && !token.trim().isEmpty()) {
                    accessToken = token;
                    lastSuccessResponse = response;
                    lastErrorResponse = null;
                    return token;
                }
                GlobalDriverConfig.logger.error("Access token missing in 200 response!");
                lastSuccessResponse = null;
                lastErrorResponse = response;
                return null;
            } else {
                GlobalDriverConfig.logger.warn("Authentication failed with status code: {}", statusCode);
                lastSuccessResponse = null;
                lastErrorResponse = response;
                return null;
            }
        } catch (Exception e) {
            GlobalDriverConfig.logger.error("Error during authentication", e);
            lastSuccessResponse = null;
            lastErrorResponse = null;
            return null;
        }
    }

    public static Response getLastErrorResponse() { return lastErrorResponse; }
    public static Response getLastSuccessResponse() { return lastSuccessResponse; }
}
