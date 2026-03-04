//package com.Cakvalidation;
//
//import com.stipter.util.DatabaseUtil;
//import com.stipter.util.TokenManager;
//import io.cucumber.java.en.And;
//import io.cucumber.java.en.Given;
//import io.cucumber.java.en.When;
//import io.restassured.response.Response;
//
//
//
//import io.cucumber.java.en.Then;
//
//import io.restassured.RestAssured;
//import org.testng.Assert;
//
//
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//
//import java.sql.Statement;
//import java.util.HashMap;
//import java.util.Map;
//
//import static io.restassured.RestAssured.given;
//
//
//public class CAKvalidations {
//
//    private String accessToken;
//    private Response response;
//
//    @Given("I have a valid CAK access token")
//    public void i_have_a_valid_cak_access_token() {
//        accessToken = TokenManager.getAccessToken();
//
//        if (accessToken == null) {
//            throw new RuntimeException("Failed to get access token");
//        }
//    }
//
//    @When("I trigger the CAK job for version {string} with BSN {string} and facilitator {string}")
//    public void i_trigger_the_cak_job_for_version_with_bsn_and_facilitator(String jobVersion, String bsn, String facilitatorId) {
//        String url;
//        String body;
//
//        if (jobVersion.equals("2.0")) {
//                url = "https://test-moplatform.stipter.nl/api/v1/jobs/triggerGroup/CAK/triggerName/CAKSUBSCREXPORTIE2_0_TRIGGER/trigger";
//            body = "{\n" +
//                    "  \"cakexport.cs_specificBsns\": \"" + bsn + "\",\n" +
//                    "  \"cakexport.cs_facilitatorIds\": \"" + facilitatorId + "\",\n" +
//                    "  \"cakexport.cakType\": \"HV-SUBSCR\",\n" +
//                    "  \"manualTrigger\": true,\n" +
//                    "  \"cakexport.processType\": \"1\",\n" +
//                    "  \"mailto\": \"test@stipter.nl\",\n" +
//                    "  \"skipSettingStatusToExported\": false,\n" +
//                    "  \"cakexport.productCodes\": \"\",\n" +
//                    "  \"cakexport.commaSeperatedPayFrequencies\": \"\"\n" +
//                    "}";
//        } else if (jobVersion.equals("3.0")) {
//            url = "https://test-moplatform.stipter.nl/api/v1/jobs/triggerGroup/INVOICE_CAK_SERVICE/triggerName/CAKSUBSCREXPORTIEB3_0_TRIGGER/trigger";
//            body = "{\n" +
//                    "  \"cakexport.cs_bsnsToSkip\": \"\",\n" +
//                    "  \"cakexport.cs_facilitatorIds\": \"" + facilitatorId + "\",\n" +
//                    "  \"cakexport.commaSeperatedPayFrequencies\": \"\",\n" +
//                    "  \"cakexport.productCodes\": \"\",\n" +
//                    "  \"cakexport.skipSettingStatusToExported\": false,\n" +
//                    "  \"manualTrigger\": true,\n" +
//                    "  \"cakexport.manualTrigger\": true,\n" +
//                    "  \"cakexport.cakType\": \"HV-SUBSCR\",\n" +
//                    "  \"mailto\": \"\",\n" +
//                    "  \"cakexport.cs_specificBsns\": \"" + bsn + "\",\n" +
//                    "  \"cakexport.processType\": \"1\"\n" +
//                    "}";
//        } else {
//            throw new IllegalArgumentException("Unsupported job version: " + jobVersion);
//        }
//
//        RestAssured.useRelaxedHTTPSValidation();
//        response = given()
//                .header("Authorization", "Bearer " + accessToken)
//                .header("Content-Type", "application/json")
//                .body(body)
//                .when()
//                .get(url);
//
//
//    }
//
//    @Then("the API response status should be {int}")
//    public void the_api_response_status_should_be(Integer expectedStatus) {
//        int actualStatus = response.getStatusCode();
//        System.out.println("Response Status Code 👉 " + actualStatus);
//
//        // assertion to verify
//        Assert.assertEquals(actualStatus, (int) expectedStatus, "Status code mismatch!");
//
//    }
//
//    @And("I verify that the job for version {string} has been executed in the table")
//    public void i_verify_that_the_job_has_been_executed_in_the_table(String jobVersion) throws Exception {
//
//        String query;
//
//        if (jobVersion.equals("2.0")) {
//            query = "SELECT count(*) as count FROM table_type_values " +
//                    "WHERE \"column\" = 'doCakSubscriptionIe2_0ExportJobLogic' " +
//                    "AND tablename = 'activeJobBatchIgnoreDoMethod'";
//        } else if (jobVersion.equals("3.0")) {
//            query = "SELECT count(*) as count FROM table_type_values " +
//                    "WHERE \"column\" = 'doCakSubscriptionExportJobLogic' " +
//                    "AND tablename = 'activeJobBatchIgnoreDoMethod'";
//        } else {
//            throw new IllegalArgumentException("❌ Unsupported job version: " + jobVersion);
//        }
//
//        boolean jobCompleted = false;
//        int retryCount = 0;
//        int maxRetries = 10; // configurable (50s max wait)
//        int waitSeconds = 5;
//
//        while (retryCount < maxRetries) {
//            try (Connection conn = DatabaseUtil.getConnection();
//                 Statement stmt = conn.createStatement();
//                 ResultSet rs = stmt.executeQuery(query)) {
//
//                if (rs.next()) {
//                    int count = rs.getInt("count");
//                    System.out.println("🔍 Query count = " + count);
//
//                    if (count == 0) {
//                        jobCompleted = true;
//                        System.out.println("✅ Job " + jobVersion + " executed successfully (count=0).");
//                        break;
//                    }
//                }
//            }
//
//            retryCount++;
//            System.out.println("⏳ Waiting for job to complete... retry " + retryCount);
//            Thread.sleep(waitSeconds * 1000L);
//        }
//
//        if (!jobCompleted) {
//            throw new AssertionError("❌ Job " + jobVersion + " did not complete within timeout.");
//        }
//    }
//
//
//    @When("I trigger the CAK job for version {string} with BSN {string}, facilitator {string}, and contract {string}")
//    public void triggerCakJob(String job, String bsn, String facilitatorId, String contractId) {
//        ScenarioContext.set("BSN", bsn);
//        ScenarioContext.set("FacilitatorId", facilitatorId);
//        ScenarioContext.set("ContractID", contractId);
//
//        // Trigger job logic...
//    }
//
//
//    @When("I validate the {string}, {string}, {string}")
//    public void validateDbValues(String expectedStatusType, String expectedCakStart, String expectedCakEnd) throws Exception {
//        Connection connection = null;
//        PreparedStatement ps = null;
//        ResultSet rs = null;
//
//        try {
//            connection = DatabaseUtil.getConnection();
//            String query =
//                    "SELECT cc.status, " +
//                            "       aa.facilitatorid, " +
//                            "       TO_CHAR(aa.subscription_startdate, 'dd-MM-yyyy') AS cakstart, " +
//                            "       TO_CHAR(aa.subscription_stopdate, 'dd-MM-yyyy') AS cakend " +
//                            "FROM caksubscriptiondelivery aa, caksubscription_contract cc " +
//                            "WHERE aa.bapersonid IN (SELECT b.id FROM baperson b WHERE b.bsn = '999970471') " +
//                            "AND aa.facilitatorid = 24 " +
//                            "AND cc.contractid = 1010574 " +
//                            "AND aa.id = cc.caksubscriptiondeliveryid";
//
//            ps = connection.prepareStatement(query);
//            rs = ps.executeQuery();
//
//            // Header
//            System.out.println("---------------------------------------------------------------");
//            System.out.printf("| %-15s | %-12s | %-12s | %-15s |%n",
//                    "Category", "StatusType", "CAK Start", "CAK End");
//            System.out.println("---------------------------------------------------------------");
//
//            if (rs.next()) {
//                String actualStatus = rs.getString("status");
//                String actualCakStart = rs.getString("cakstart");
//                String actualCakEnd = rs.getString("cakend");
//
//                // Expected row
//                System.out.printf("| %-15s | %-12s | %-12s | %-15s |%n",
//                        "Expected", expectedStatusType, expectedCakStart, expectedCakEnd);
//
//                // Actual row
//                System.out.printf("| %-15s | %-12s | %-12s | %-15s |%n",
//                        "Actual", actualStatus, actualCakStart, actualCakEnd);
//
//                System.out.println("---------------------------------------------------------------");
//
//                // Validation
//                boolean statusMatch = expectedStatusType.equals(actualStatus);
//                boolean startMatch = expectedCakStart.equals(actualCakStart);
//                boolean endMatch = expectedCakEnd.equals(actualCakEnd);
//
//                if (statusMatch && startMatch && endMatch) {
//                    System.out.println("✅ Validation successful: All values match!");
//                } else {
//                    System.out.println("❌ Validation failed: Some values do not match!");
//                }
//            } else {
//                Assert.fail("❌ No records found for the given query.");
//            }
//
//        } finally {
//            if (rs != null) rs.close();
//            if (ps != null) ps.close();
//            if (connection != null) connection.close();
//        }
//    }
//    public class ScenarioContext {
//        private static final Map<String, Object> context = new HashMap<>();
//
//        public static void set(String key, Object value) {
//            context.put(key, value);
//        }
//
//        public static Object get(String key) {
//            return context.get(key);
//        }
//
//        public static void clear() {
//            context.clear();
//        }
//
//
//    }

//
//ert
//}
//sd
