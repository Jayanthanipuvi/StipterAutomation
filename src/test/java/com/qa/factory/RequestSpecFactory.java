package com.qa.factory;

import com.qa.util.PropertiesFileReader;
import com.stipter.util.TokenManager;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;


public class RequestSpecFactory {

    private static RequestSpecification spec;

    public static RequestSpecification givenApi() {
        if (spec == null) {
            String baseUrl = PropertiesFileReader.getProperty("api.base.url");

            RequestSpecBuilder builder = new RequestSpecBuilder()
                    .setBaseUri(baseUrl)
                    .setAccept(ContentType.JSON);  // ✅ only Accept set

            // ✅ Add auth headers
            TokenManager.getAuthHeaders().forEach(builder::addHeader);

            spec = builder.build();
        }
        return spec;
    }

    public static void reset() {
        spec = null;
    }
}
