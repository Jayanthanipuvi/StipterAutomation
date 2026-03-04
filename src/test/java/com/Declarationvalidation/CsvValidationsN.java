package com.Declarationvalidation;


import com.qa.util.PropertiesFileReader;
import com.stipter.util.TestContext;
import com.stipter.util.TokenManager;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;


import java.io.File;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.*;

public class CsvValidationsN {

    public String accessToken = TokenManager.getAccessToken();
    private final TestContext context;





    public CsvValidationsN(TestContext context) {
        this.context = context;
    }

    @Given("an access token is generated")
    public void an_access_token_is_generated() {
        System.out.println(accessToken + "My access Token");
    }

    @When("the user posts to {string} with facilitatorId {string} and supplierId {string} using valid file {string}")
    public void post_with_valid_file(String urlTemplate, String facilitatorId, String supplierId, String filePropertyKey) throws InterruptedException {
        Thread.sleep(10000);

        String endpoint;
        endpoint = PropertiesFileReader.getProperty(urlTemplate);
        System.out.println(("testing"+ endpoint));
        if (!facilitatorId.isEmpty()) {
            endpoint = endpoint.replace("{facilitatorId}", facilitatorId);
        } else {
            endpoint = endpoint.replace("/facilitator/{facilitatorId}", "/facilitator");
        }

        if (!supplierId.isEmpty()) {
            endpoint = endpoint.replace("{supplierId}", supplierId);
        } else {
            endpoint = endpoint.replace("/supplier/{supplierId}", "/supplier");
        }
        System.out.println("orginal URI" + endpoint);
        String filePath = com.qa.util.PropertiesFileReader.getProperty(filePropertyKey);
        File fileToUpload = new File(filePath);

        assertTrue(fileToUpload.exists(), "File not found: " + filePath);


        Response response = given()
                .relaxedHTTPSValidation()
                .baseUri("https://test-moplatform.stipter.nl")
                .header("Authorization", "Bearer " + accessToken)
                .header("FACILITY_ID", facilitatorId)
                .header("SUPPLIER_ID", supplierId)
                .multiPart("attachmentTypeId", "200")
                .multiPart("description", "")
                .multiPart("file", fileToUpload, "text/csv")
                .log().all()
                .when()
                .post(endpoint)
                .then()
                .log().all()
                .extract().response();

        context.setResponse(response);
    }

    @When("the user posts to {string} with facilitatorId {string} and supplierId {string} without a file")
    public void post_without_file(String urlTemplate, String facilitatorId, String supplierId) throws InterruptedException {
        Thread.sleep(10000);
        String endpoint2;
        endpoint2 = PropertiesFileReader.getProperty(urlTemplate);

        if (supplierId.isEmpty()) {
            endpoint2 = urlTemplate
                    .replace("/{supplierId}", "//upload");  // force double slash
        } else {
            endpoint2 = urlTemplate
                    .replace("{facilitatorId}", facilitatorId)
                    .replace("{supplierId}", supplierId);
        }
        System.out.println("orginal URI" + endpoint2);
        Response response = given()
                .relaxedHTTPSValidation()
                .baseUri("https://test-moplatform.stipter.nl")
                .header("Authorization", "Bearer " + accessToken)
                .header("FACILITY_ID", facilitatorId)
                .header("SUPPLIER_ID", supplierId)
                .multiPart("attachmentTypeId", "200")
                .multiPart("description", "")
                .multiPart("",  "text/csv")
                // Intentionally skip file part!
                .log().all()
                .when()
                .post(endpoint2)
                .then()
                .log().all()
                .extract().response();

        context.setResponse(response);
    }

    @When("the user posts to {string} with facilitatorId {string} and supplierId {string} using invalid file {string}")
    public void post_with_invalid_file(String urlTemplate, String facilitatorId, String supplierId, String filePropertyKey) throws InterruptedException {
        Thread.sleep(10000);
        int retries = 3;
        int waitMs = 2000;

        String template = PropertiesFileReader.getProperty(urlTemplate);
        String endpoint;
        if (supplierId.isEmpty()) {
            endpoint = template.replace("/supplier/{supplierId}", "/supplier/upload");
        } else {
            endpoint = template
                    .replace("{facilitatorId}", facilitatorId)
                    .replace("{supplierId}", supplierId);
        }

        System.out.println("Resolved Endpoint: " + endpoint);
        String filePath = com.qa.util.PropertiesFileReader.getProperty(filePropertyKey);
        File fileToUpload = new File(filePath);

        assertTrue(fileToUpload.exists(), "File not found: " + filePath);

        for (int attempt = 1; attempt <= retries; attempt++) {
            Response response = given()
                    .relaxedHTTPSValidation()
                    .baseUri("https://test-moplatform.stipter.nl")
                    .header("Authorization", "Bearer " + accessToken)
                    .header("FACILITY_ID", facilitatorId)
                    .header("SUPPLIER_ID", supplierId)
                    .multiPart("attachmentTypeId", "200")
                    .multiPart("description", "")
                    .multiPart("file", fileToUpload, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .log().all()
                    .when()
                    .post(endpoint)
                    .then()
                    .log().all()
                    .extract().response();

            context.setResponse(response);
        }
    }

    @Then("the response status should be like a {int}")
    public void the_response_status_should_be_like_a (Integer expectedStatus) throws InterruptedException {
        Thread.sleep(5000);
        Response response = context.getResponse();

        assertNotNull(response,"No response received (possible connection error)");
        int actualStatus = response.getStatusCode();
        assertEquals(
                actualStatus,
                expectedStatus.intValue(),
                "Expected status " + expectedStatus + " but got " + actualStatus
        );

    }
}

