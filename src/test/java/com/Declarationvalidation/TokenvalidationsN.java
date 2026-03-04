package com.Declarationvalidation;


import com.qa.util.PropertiesFileReader;
import com.stipter.util.TokenManager;
import io.cucumber.java.en.Given;

import io.restassured.response.Response;



import static org.testng.Assert.*;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;


public class TokenvalidationsN {


    String username;
    String password;
    String url;
    String token;
    Response response;

    @Given("the user has valid credentials")
    public void the_user_has_valid_credentials() {
        username = PropertiesFileReader.getProperty("swagger.username");
        password = PropertiesFileReader.getProperty("swagger.password");
        url = com.qa.util.PropertiesFileReader.getProperty("swagger.token.url");
    }

    @Given("the user has invalid credentials")
    public void the_user_has_invalid_credentials() {
        username = "invalid_user";
        password = "wrong_password";
        url = com.qa.util.PropertiesFileReader.getProperty("swagger.token.url");
    }

    @Given("the token endpoint is incorrect")
    public void the_token_endpoint_is_incorrect() {
        url = com.qa.util.PropertiesFileReader.getProperty("swagger.token.url");
    }

    @Given("the server is unavailable")
    public void the_server_is_unavailable() {
        url = com.qa.util.PropertiesFileReader.getProperty("swagger.token.url");
    }

    @When("the user requests an access token")
    public void the_user_requests_an_access_token() {
        token = TokenManager.generateAccessToken(username, password, url);
        if (token != null) {
            response = TokenManager.getLastSuccessResponse();
        } else {
            response = TokenManager.getLastErrorResponse();
        }
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer expectedStatus) {
        assertNotNull(response, "No response received (possible connection error)");

        int actualStatus = response.getStatusCode();

        assertEquals(
                actualStatus,
                expectedStatus.intValue(),
                "Expected status " + expectedStatus + " but got " + actualStatus
        );
    }
    @Then("the request should fail with connection error")
    public void the_request_should_fail_with_connection_error() {
        assertNull(
                response,
                "Expected connection error (no HTTP response), but got HTTP response with status "
                        + (response != null ? response.getStatusCode() : "null")
        );

    }

    @Then("the access token should not be empty")
    public void the_access_token_should_not_be_empty() {
        assertNotNull(token, "Access token is null!");
        assertFalse(token.trim().isEmpty(), "Access token is empty!");
    }
}