package com.stipter.util;
import io.restassured.response.Response;
public class TestContext {

    private Response response;
    private String accessToken;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
