package com.AppealContract;


import com.qa.util.ExcelReader;
import com.qa.util.PropertiesFileReader;
import com.stipter.util.AuthMode;
import com.stipter.util.TokenManager;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class AppealValidate {
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(AppealValidate.class);
    private String accessToken;
    private AuthMode mode;
    private String baPersonId;



    @Given("I have a valid GGK access token or cookie")
    public void i_have_a_valid_ggk_access_token_or_cookie() {

        String configMode = PropertiesFileReader.getProperty("auth.mode");

        if (configMode == null || configMode.trim().isEmpty()) {
            configMode = System.getProperty("auth.mode");
        }

        mode = AuthMode.from(configMode);
        logger.error("🔥 TEST ERROR LOG - GLOBAL");
        logger.info("⚙️ Auth Mode Selected = {}", mode);

        if (mode == AuthMode.KEYCLOAK) {

            logger.info("🔐 Using Keycloak mode");

            accessToken = TokenManager.getAccessToken();

            if (accessToken != null && !accessToken.isEmpty()) {
                logger.info("✅ Token generated successfully");
                logger.debug("Token: {}", accessToken); // debug only
            } else {
                logger.error("❌ Token generation failed");

                if (TokenManager.getLastErrorResponse() != null) {
                    logger.error("Auth Error Response: {}",
                            TokenManager.getLastErrorResponse().asString());
                }
            }

        } else {

            logger.info("🍪 Using Cookie Authentication mode");
            logger.info("Skipping Keycloak login — using cookie/x-user-token");

            // optional debug
            logger.debug("x-user-token: {}",
                    PropertiesFileReader.getProperty("auth.x_user_token"));

            logger.debug("cookies: {}",
                    PropertiesFileReader.getProperty("auth.cookies"));
        }

        RestAssured.useRelaxedHTTPSValidation();
    }


    @When("I send all appeal requests from Excel sheet {string}")
    public void i_send_all_appeal_requests_from_excel_sheet(String sheetName) {

        try {

            String filePath = "C:/MOPAutomationFiles/MOPAutomationExcel/AppealContract/AppealContractValidations.xlsx";

            ExcelReader reader = new ExcelReader();

            // 👉 Sheet 1
            List<Map<String, String>> data =
                    reader.getData(filePath, "AppealFirstpage");

            // 👉 Sheet 2 (Network)
            List<Map<String, String>> networkSheet =
                    reader.getData(filePath, "Network");

            // 👉 Sheet 3 (Indication)
            List<Map<String, String>> indicationSheet =
                    reader.getData(filePath, "Indication");

            // 👉 Convert Network → Map
            Map<String, Map<String, String>> networkDataMap = new HashMap<>();

            for (Map<String, String> netRow : networkSheet) {
                networkDataMap.put(netRow.get("testCaseId"), netRow);
            }

            // 👉 Convert Indication → Map
            Map<String, Map<String, String>> indicationMap = new HashMap<>();

            for (Map<String, String> indRow : indicationSheet) {
                indicationMap.put(indRow.get("testCaseId"), indRow);
            }

            logger.info("📊 Total Test Cases: {}", data.size());

            for (Map<String, String> row : data) {

                String testCaseId = row.get("testCaseId");

                // 🔥 Skip empty rows
                if (testCaseId == null || testCaseId.trim().isEmpty()) {
                    logger.warn("⛔ Skipping empty row");
                    continue;
                }

                logger.info("🚀 Executing TC: {}", testCaseId);

                // 👉 API1 Request
                String body = AppealRequestBuilder.buildApi1Request(row);

                logger.debug("API1 Request: {}", body);

                Response response = RestAssured.given()
                        .headers(TokenManager.getAuthHeaders())
                        .baseUri("https://test-moplatform.stipter.nl")
                        .contentType("application/json")
                        .body(body)
                        .post("/api/v1/signal/facilitator/24/person/0/unknownbsn/7/createorupdate");

                int status = response.getStatusCode();
                String responseBody = response.asString();

                // 👉 Extract ID
                String baPersonId = response.jsonPath().getString("baPersonId");

                logger.info("📌 Extracted baPersonId: {}", baPersonId);
                logger.info("📥 Status: {}", status);
                logger.debug("Response: {}", responseBody);

                int expectedStatus = Integer.parseInt(row.get("expectedStatus"));

                if (status == expectedStatus) {
                    logger.info("✅ API1 PASS: {}", testCaseId);
                } else {
                    logger.error("❌ API1 FAIL: {}", testCaseId);
                    logger.error("Response: {}", responseBody);
                }

                // =====================================================
                // 🔥 API2 LOGIC STARTS HERE
                // =====================================================

                String networkFlag = row.get("Network");

                if ("YES".equalsIgnoreCase(networkFlag)) {

                    Map<String, String> netRow = networkDataMap.get(testCaseId);

                    if (netRow != null && baPersonId != null) {

                        logger.info("➡️ Calling API2 for TC: {}", testCaseId);

                        Response api2Response = callApi2(baPersonId, netRow);

                        int api2Status = api2Response.getStatusCode();
                        String api2Body = api2Response.asString();

                        logger.info("📥 API2 Status: {}", api2Status);
                        logger.debug("API2 Response: {}", api2Body);

                        if (api2Status == 201) {
                            logger.info("✅ API2 PASS: {}", testCaseId);
                        } else {
                            logger.error("❌ API2 FAIL: {}", testCaseId);
                            logger.error("Response: {}", api2Body);
                        }

                    } else {
                        logger.warn("⚠️ Missing Network data or baPersonId for TC: {}", testCaseId);
                    }

                } else {
                    logger.info("⏭️ Skipping API2 for TC: {}", testCaseId);
                }

// =====================================================
// 🔥 API3 LOGIC STARTS HERE
// =====================================================

                String indicationFlag = row.get("Indication");

                if ("YES".equalsIgnoreCase(indicationFlag)) {

                    Map<String, String> indRow = indicationMap.get(testCaseId);

                    if (indRow != null) {

                        logger.info("➡️ Calling API3 for TC: {}", testCaseId);

                        Response api3Response = callApi3(indRow);

                        int api3Status = api3Response.getStatusCode();
                        String api3Body = api3Response.asString();

                        logger.info("📥 API3 Status: {}", api3Status);
                        logger.debug("API3 Response: {}", api3Body);

                        if (api3Status == 200 || api3Status == 201) {
                            logger.info("✅ API3 PASS: {}", testCaseId);
                        } else {
                            logger.error("❌ API3 FAIL: {}", testCaseId);
                            logger.error("Response: {}", api3Body);
                        }

                    } else {
                        logger.warn("⚠️ No Indication data found for TC: {}", testCaseId);
                    }

                } else {
                    logger.info("⏭️ Skipping API3 for TC: {}", testCaseId);
                }

            }

        } catch (Exception e) {
            logger.error("🔥 Exception occurred", e);
            throw new RuntimeException(e);
        }




    }


    private Response callApi2(String baPersonId, Map<String, String> r) {

        String url = "/api/v1/person/" + baPersonId +
                "/contact/interested/facilitator/24/signals/0?form=true";

        String body = AppealRequestBuilder.buildApi2Request(r);

        logger.debug("API2 Request: {}", body);

        return RestAssured.given()
                .headers(TokenManager.getAuthHeaders())
                .baseUri("https://test-moplatform.stipter.nl")
                .contentType("application/json")
                .body(body)
                .post(url);
    }





    private Response callApi3(Map<String, String> r) {

        String researchId = r.get("researchId");

        String url = "/api/v1/research/" + researchId + "/researchspec?form=true";

        String body = AppealRequestBuilder.buildApi3Request(r);

        logger.debug("API3 Request: {}", body);

        return RestAssured.given()
                .headers(TokenManager.getAuthHeaders())
                .baseUri("https://test-moplatform.stipter.nl")
                .contentType("application/json")
                .body(body)
                .post(url);
    }



    @Then("I validate appeal responses")
    public void i_validate_appeal_responses() {



    }

}
