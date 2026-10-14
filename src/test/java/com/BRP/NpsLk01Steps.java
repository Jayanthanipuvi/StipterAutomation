package com.BRP;

import com.qa.util.ExcelReader;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class NpsLk01Steps {

    private static final int LOG_PREVIEW_LIMIT = 2000;

    // ================================================================
    // BRP TEST ENDPOINT
    // ================================================================

    private static final String ENDPOINT =
            "https://acc-moplatform.stipter.nl/smop-ws/services/" +
                    "OntvangAsynchroon0301.OntvangAsynchroon0301HttpSoap11Endpoint/";

    // ================================================================
    // Response
    // ================================================================

    private Response response;

    // ================================================================
    // HTTP TIMEOUT CONFIGURATION
    // ================================================================

    private static final RestAssuredConfig REST_ASSURED_CONFIG =
            RestAssuredConfig.config()
                    .httpClient(
                            HttpClientConfig.httpClientConfig()
                                    .setParam("http.connection.timeout", 30000)
                                    .setParam("http.socket.timeout", 60000)
                                    .setParam("http.connection-manager.timeout", 30000)
                    );

    // ================================================================
    // GIVEN - Send NPSLK01 message
    // ================================================================

    @Given("I send the NPSLK01 message to the TEST BRP endpoint")
    public void sendNpsLk01Message() throws Exception {

        // ============================================================
        // 1. Excel file
        // ============================================================

        String excelPath =
                "C:\\MOPAutomationFiles\\MOPAutomationExcel\\BRP\\NPSLK01.xlsx";

        // ============================================================
        // 2. XML template
        // ============================================================

        String templatePath =
                "src/test/resources/brp/NPSLK01.xml";

        String template = Files.readString(
                Path.of(templatePath),
                StandardCharsets.UTF_8
        );

        // ============================================================
        // 3. Read Excel
        // ============================================================

        ExcelReader excelReader = new ExcelReader();

        List<Map<String, String>> testData =
                excelReader.getData(excelPath, "NPSLK01");

        System.out.println();
        System.out.println("==============================================");
        System.out.println("Excel data loaded");
        System.out.println("==============================================");

        // ============================================================
        // 4. Validate Excel data
        // ============================================================

        if (testData == null || testData.isEmpty()) {

            throw new IllegalStateException(
                    "No test data found in Excel sheet: NPSLK01"
            );
        }

        // Count only actual test cases
        int validTestCaseCount = 0;

        for (Map<String, String> data : testData) {

            String testCase = data.get("TestCase");

            if (testCase != null && !testCase.trim().isEmpty()) {
                validTestCaseCount++;
            }
        }

        System.out.println(
                "Total valid test cases: " + validTestCaseCount
        );

        if (validTestCaseCount == 0) {

            throw new IllegalStateException(
                    "No valid TestCase values found in Excel."
            );
        }

        // ============================================================
        // 5. Execute each valid Excel test case
        // ============================================================

        for (Map<String, String> data : testData) {

            String testCase = data.get("TestCase");

            // --------------------------------------------------------
            // Skip blank rows
            // --------------------------------------------------------

            if (testCase == null || testCase.trim().isEmpty()) {
                continue;
            }

            testCase = testCase.trim();

            System.out.println();
            System.out.println("==============================================");
            System.out.println("Executing Test Case: " + testCase);
            System.out.println("==============================================");

            // ========================================================
            // 6. Replace Excel values in XML
            // ========================================================

            String xmlRequest = template;

            // --------------------------------------------------------
            // Organisation
            // --------------------------------------------------------

            xmlRequest = replace(
                    xmlRequest,
                    "organisatie",
                    data.get("organisatie")
            );

            // --------------------------------------------------------
            // sleutelOntvangend
            // --------------------------------------------------------

            xmlRequest = replace(
                    xmlRequest,
                    "sleutelOntvangend",
                    data.get("sleutelOntvangend")
            );

            // --------------------------------------------------------
            // First municipality
            // --------------------------------------------------------

            xmlRequest = replace(
                    xmlRequest,
                    "gemeente1",
                    data.get("gemeente1")
            );

            // --------------------------------------------------------
            // Second municipality
            // --------------------------------------------------------

            xmlRequest = replace(
                    xmlRequest,
                    "gemeente2",
                    data.get("gemeente2")
            );

            // --------------------------------------------------------
            // Second object - address details
            // --------------------------------------------------------

            xmlRequest = replace(
                    xmlRequest,
                    "woonplaatsNaam",
                    data.get("woonplaatsNaam")
            );

            xmlRequest = replace(
                    xmlRequest,
                    "openbareRuimteNaam",
                    data.get("openbareRuimteNaam")
            );

            xmlRequest = replace(
                    xmlRequest,
                    "straatnaam",
                    data.get("straatnaam")
            );

            xmlRequest = replace(
                    xmlRequest,
                    "postcode",
                    data.get("postcode")
            );

            xmlRequest = replace(
                    xmlRequest,
                    "huisnummer",
                    data.get("huisnummer")
            );

            // ========================================================
            // 7. Print generated XML
            // ========================================================

            System.out.println();
            System.out.println("Generated NPSLK01 XML:");
            System.out.println("----------------------------------------------");
            System.out.println(preview(xmlRequest));
            System.out.println("----------------------------------------------");

            // ========================================================
            // 8. Prepare request body
            // ========================================================

            byte[] requestBody =
                    xmlRequest.getBytes(StandardCharsets.UTF_8);

            System.out.println();
            System.out.println("Request body length: " + requestBody.length);
            System.out.println("Request Endpoint: " + ENDPOINT);
            System.out.println("Request Method: POST");
            System.out.println("Request Content-Type: application/json");

            // ========================================================
            // 9. Send request
            // ========================================================

            try {

                System.out.println();
                System.out.println(
                        "Sending NPSLK01 request for " + testCase + "..."
                );

                response = given()
                        .config(REST_ASSURED_CONFIG)
                        .header("Content-Type", "application/json")
                        .header("Connection", "close")
                        .body(requestBody)
                        .when()
                        .post(ENDPOINT);

                System.out.println(
                        "Response received for " + testCase
                );

            } catch (Exception e) {

                System.out.println();
                System.out.println("==============================================");
                System.out.println(
                        "REQUEST FAILED: " + testCase
                );
                System.out.println("==============================================");

                System.out.println(
                        "Error: " + e.getMessage()
                );

                throw new AssertionError(
                        "NPSLK01 request failed for Test Case "
                                + testCase,
                        e
                );
            }

            // ========================================================
            // 10. Print HTTP status
            // ========================================================

            System.out.println();
            System.out.println(
                    "HTTP Status for " + testCase + ": "
                            + response.statusCode()
            );

            // ========================================================
            // 11. Print response
            // ========================================================

            System.out.println();
            System.out.println("Response:");
            System.out.println("----------------------------------------------");

            System.out.println(
                    preview(response.asPrettyString())
            );

            System.out.println("----------------------------------------------");

            // ========================================================
            // 12. Validate response
            // ========================================================

            assertNotNull(
                    response,
                    "Response should not be null"
            );

            assertEquals(
                    response.statusCode(),
                    200,
                    "NPSLK01 request failed for Test Case: "
                            + testCase
            );

            // ========================================================
            // 13. Test case successful
            // ========================================================

            System.out.println();
            System.out.println(
                    "Test Case " + testCase
                            + " completed successfully. HTTP Status: 200"
            );
        }
    }

    // ================================================================
    // THEN - Verify successful response
    // ================================================================

    @Then("I should receive a successful NPSLK01 response")
    public void verifySuccessfulNpsLk01Response() {

        assertNotNull(
                response,
                "NPSLK01 response should not be null"
        );

        assertEquals(
                response.statusCode(),
                200,
                "Expected HTTP 200 response"
        );

        System.out.println();
        System.out.println(
                "NPSLK01 request completed successfully. HTTP Status: 200"
        );
    }

    // ================================================================
    // Helper method
    // ================================================================

    private String replace(
            String template,
            String key,
            String value
    ) {

        if (value == null) {

            throw new IllegalArgumentException(
                    "Missing Excel value for column: " + key
            );
        }

        return template.replace(
                "${" + key + "}",
                value
        );
    }

    private String preview(String value) {
        if (value == null) {
            return "";
        }

        if (value.length() <= LOG_PREVIEW_LIMIT) {
            return value;
        }

        return value.substring(0, LOG_PREVIEW_LIMIT)
                + "\n... output truncated for console. Total length: "
                + value.length();
    }
}
