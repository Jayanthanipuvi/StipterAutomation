package com.Cakvalidation;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.qa.util.ExcelReader;
import com.qa.util.PropertiesFileReader;
import com.stipter.util.DatabaseUtil;
import com.stipter.util.TokenManager;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import io.cucumber.java.Scenario;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.testng.Assert;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


public class Cakvalidationcsv {

    private Scenario scenario;
    private String accessToken;
    private final ExcelReader excel = new ExcelReader();
    private final String excelPath = PropertiesFileReader.getProperty("cak.excel.path");
    private List<Map<String, String>> jobSheet;
    private List<Map<String, String>> validationRows;
    private Map<String, Map<String, String>> jobByIeb;
    private final List<Map<String, String>> executedRows = new ArrayList<>();
    private final String baseUrl = PropertiesFileReader.getProperty("url.test");

    private Response lastResponse;

    private static final String authMode = PropertiesFileReader.getProperty("auth.mode");

    @Given("I have a valid CAK access token")
    public void i_have_a_valid_cak_access_token() {

        System.out.println("--------------------------------------------------");
        System.out.println(" CAK Authentication Mode: " + authMode.toUpperCase());
        System.out.println("--------------------------------------------------");

        if (authMode.equalsIgnoreCase("KEYCLOAK")) {

            System.out.println("🔐 Using Keycloak authentication for CAK");
            accessToken = TokenManager.getAccessToken();

            if (accessToken == null || accessToken.isEmpty()) {
                throw new RuntimeException("❌ Failed to get Keycloak token for CAK");
            }

        } else {
            System.out.println("🍪 Using Cookie + x-user-token authentication for CAK");
            accessToken = null;
        }

        RestAssured.useRelaxedHTTPSValidation();
    }


    @When("I create, trigger, and wait for CAK jobs for all rows in {string} using job metadata from {string}")
    public void i_execute_jobs(String validationSheetName, String jobSheetName) throws Exception {

        loadSheets(validationSheetName, jobSheetName);

        for (Map<String, String> columnValues : validationRows) {

            if (!columnValues.isEmpty() && !columnValues.get("Job").isEmpty()) {

                String jobVersion = columnValues.get("Job");
                String bsn = columnValues.get("BSN");
                String facilitator = columnValues.get("FacilitatorId");

                Map<String, String> meta = jobByIeb.get(jobVersion);

                System.out.printf("🚀 Starting CAK execution for JobVersion=%s, BSN=%s%n", jobVersion, bsn);

                String payload = buildCreateJobPayload(meta, facilitator, bsn);

                RequestSpecification request = RestAssured.given().relaxedHTTPSValidation();

                if (authMode.equalsIgnoreCase("KEYCLOAK")) {
                    request = request.header("Authorization", "Bearer " + accessToken);
                } else {
                    request = request
                            .header("Cookie", PropertiesFileReader.getProperty("auth.cookies"))
                            .header("x-user-token", PropertiesFileReader.getProperty("auth.x_user_token"));
                }

                lastResponse = request
                        .header("Content-Type", "application/json")
                        .body(payload)
                        .post(baseUrl + "/api/v1/jobs/createorupdate");

                Assert.assertEquals(lastResponse.getStatusCode(), 200, "❌ Job creation failed!");

                String url = baseUrl + "/api/v1/jobs/triggerGroup/" +
                        meta.get("triggerGroup") + "/triggerName/" +
                        meta.get("triggerName") + "/trigger";

                request = RestAssured.given().relaxedHTTPSValidation();

                if (authMode.equalsIgnoreCase("KEYCLOAK")) {
                    request = request.header("Authorization", "Bearer " + accessToken);
                } else {
                    request = request
                            .header("Cookie", PropertiesFileReader.getProperty("auth.cookies"))
                            .header("x-user-token", PropertiesFileReader.getProperty("auth.x_user_token"));
                }

                lastResponse = request.get(url);

                Assert.assertEquals(lastResponse.getStatusCode(), 200, "❌ Trigger failed!");

                waitUntilJobStepFinished(jobVersion);

                executedRows.add(columnValues);
            }
        }
    }


    @Then("I verify the DB values for all executed rows")
    public void i_verify_db_values() throws Exception {

        boolean allPass = true;

        for (Map<String, String> columnValues : validationRows) {
            if (!columnValues.isEmpty() && !columnValues.get("Job").isEmpty()) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PrintStream ps = new PrintStream(baos);
                PrintStream oldOut = System.out;
                System.setOut(ps);

                boolean result = validateDbValues(
                        columnValues.get("BSN"),
                        columnValues.get("FacilitatorId"),
                        columnValues.get("ContractID"),
                        columnValues.get("Statustype"),
                        columnValues.get("CakStart"),
                        columnValues.get("CakEnd")
                );

                System.out.flush();
                System.setOut(oldOut);

                String consoleOutput = baos.toString();
                scenario.log("\n" + consoleOutput + "\n");

                if (!result) allPass = false;
            }
        }

        if (!allPass) {
            throw new AssertionError("❌ Some rows did not match expected DB values.");
        }

        scenario.log("🎉 All rows matched expected DB values. Test PASS ✅");
        System.out.println("🎉 All rows matched expected DB values. Test PASS ✅");
    }


    @Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
    }


    private void loadSheets(String validationSheetName, String jobSheetName)
            throws IOException, InvalidFormatException, org.apache.poi.openxml4j.exceptions.InvalidFormatException {

        System.out.println("Loading sheets: " + validationSheetName + ", " + jobSheetName);

        jobSheet = excel.getData(excelPath, jobSheetName);
        validationRows = excel.getData(excelPath, validationSheetName);

        jobByIeb = jobSheet.stream()
                .filter(r -> r.get("IEB") != null && !r.get("IEB").trim().isEmpty())
                .collect(Collectors.toMap(r -> r.get("IEB"), r -> r));
    }


    private String buildCreateJobPayload(Map<String, String> meta, String facilitatorId, String bsn) {
        return "[{" +
                "\"id\":0," +
                "\"jobClass\":\"" + meta.get("jobClass") + "\"," +
                "\"jobGroup\":\"" + meta.get("jobGroup") + "\"," +
                "\"jobName\":\"" + meta.get("jobName") + "\"," +
                "\"triggerGroup\":\"" + meta.get("triggerGroup") + "\"," +
                "\"triggerName\":\"" + meta.get("triggerName") + "\"," +
                "\"updateIfExists\":true," +
                "\"priority\":5," +
                "\"cronExpression\":\"0 0 9 1 10 ? 2080\"," +
                "\"jobProperties\":[" +
                "{ \"propertyName\": \"cakexport.cs_bsnsToSkip\", \"propertyValue\": \"\" }," +
                "{ \"propertyName\": \"cakexport.cs_facilitatorIds\", \"propertyValue\": \"" + facilitatorId + "\" }," +
                "{ \"propertyName\": \"cakexport.commaSeperatedPayFrequencies\", \"propertyValue\": \"\" }," +
                "{ \"propertyName\": \"cakexport.productCodes\", \"propertyValue\": \"\" }," +
                "{ \"propertyName\": \"cakexport.skipSettingStatusToExported\", \"propertyValue\": \"false\" }," +
                "{ \"propertyName\": \"cakexport.manualTrigger\", \"propertyValue\": \"true\" }," +
                "{ \"propertyName\": \"cakexport.cakType\", \"propertyValue\": \"HV-SUBSCR\" }," +
                "{ \"propertyName\": \"mailto\", \"propertyValue\": \"\" }," +
                "{ \"propertyName\": \"cakexport.cs_specificBsns\", \"propertyValue\": \"" + bsn + "\" }," +
                "{ \"propertyName\": \"cakexport.processType\", \"propertyValue\": \"1\" }" +
                "]" +
                "}]";
    }


    private void waitUntilJobStepFinished(String jobVersion) throws Exception {

        String query;

        if ("2.0".equals(jobVersion)) {
            query = "SELECT count(*) AS cnt FROM table_type_values " +
                    "WHERE \"column\" = 'doCakSubscriptionIe2_0ExportJobLogic' " +
                    "AND tablename = 'activeJobBatchIgnoreDoMethod'";
        } else if ("3.0".equals(jobVersion)) {
            query = "SELECT count(*) AS cnt FROM table_type_values " +
                    "WHERE \"column\" = 'doCakSubscriptionExportJobLogic' " +
                    "AND tablename = 'activeJobBatchIgnoreDoMethod'";
        } else {
            throw new IllegalArgumentException("Unsupported job version: " + jobVersion);
        }

        int retries = 10;

        for (int i = 0; i < retries; i++) {
            try (Connection conn = DatabaseUtil.getConnection();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(query)) {

                if (rs.next() && rs.getInt("cnt") == 0) return;
            }
            Thread.sleep(5000);
        }

        throw new AssertionError("Job " + jobVersion + " did not finish in time");
    }


    private boolean validateDbValues(
            String bsn, String facilitatorId, String contractId,
            String expectedStatusType, String expectedCakStart, String expectedCakEnd) throws Exception {

        String sql = "SELECT cc.status, aa.facilitatorid, " +
                "TO_CHAR(aa.subscription_startdate, 'dd-MM-yyyy') AS cakstart, " +
                "TO_CHAR(aa.subscription_stopdate, 'dd-MM-yyyy') AS cakend " +
                "FROM caksubscriptiondelivery aa, caksubscription_contract cc " +
                "WHERE aa.bapersonid IN (SELECT b.id FROM baperson b WHERE b.bsn = ?::INTEGER) " +
                "AND aa.facilitatorid = ?::INTEGER " +
                "AND cc.contractid = ?::INTEGER " +
                "AND aa.id = cc.caksubscriptiondeliveryid";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, Integer.parseInt(bsn));
            ps.setInt(2, Integer.parseInt(facilitatorId));
            ps.setInt(3, Integer.parseInt(contractId));

            try (ResultSet rs = ps.executeQuery()) {

                if (!rs.next()) {
                    System.out.printf("❌ No record found for BSN=%s, ContractID=%s%n", bsn, contractId);
                    return false;
                }

                String dbStatus = rs.getString("status").trim();
                String dbCakStart = rs.getString("cakstart").trim();
                String dbCakEnd = rs.getString("cakend").trim();

                System.out.println("-------------------------------------------------------------------------------------------");
                System.out.printf("| %-10s | %-12s | %-12s | %-10s | %-10s | %-10s | %-10s |%n",
                        "BSN", "ContractID", "Facilitator", "StatusType", "CakStart", "CakEnd", "ResultType");
                System.out.println("-------------------------------------------------------------------------------------------");

                System.out.printf("| %-10s | %-12s | %-12s | %-10s | %-10s | %-10s | %-10s |%n",
                        bsn, contractId, facilitatorId, expectedStatusType.trim(), expectedCakStart.trim(), expectedCakEnd.trim(), "Expected");

                System.out.printf("| %-10s | %-12s | %-12s | %-10s | %-10s | %-10s | %-10s |%n",
                        bsn, contractId, facilitatorId, dbStatus, dbCakStart, dbCakEnd, "Actual");

                System.out.println("-------------------------------------------------------------------------------------------");

                boolean matches = dbStatus.equals(expectedStatusType.trim())
                        && dbCakStart.equals(expectedCakStart.trim())
                        && dbCakEnd.equals(expectedCakEnd.trim());

                System.out.println(matches ? "✅ PASS" : "❌ FAIL");
                System.out.println();

                return matches;
            }
        }
    }
}
