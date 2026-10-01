package com.Fobo;

import com.qa.util.ExcelReader;
import com.qa.util.PropertiesFileReader;
import com.stipter.util.AuthMode;

import com.stipter.util.TokenManager;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;

import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import io.cucumber.java.Scenario;





public class Fobovalidation {
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(Fobovalidation.class);
    private String accessToken;
    private AuthMode mode;
    private Scenario scenario;
    private List<String> failedTestCases = new ArrayList<>();

    @io.cucumber.java.Before
    public void beforeScenario(Scenario scenario) {
        this.scenario = scenario;
    }



    private List<Map<String, String>> executedRows = new ArrayList<>();

    private List<ExecutionResult> executionResults = new ArrayList<>();

    @Given("I have a valid GGK access token or cookie")
    public void i_have_a_valid_ggk_access_token() {
        String configMode = PropertiesFileReader.getProperty("auth.mode");


        if (configMode == null || configMode.trim().isEmpty()) {
            configMode = System.getProperty("auth.mode");
        }

        mode = AuthMode.from(configMode);
        System.out.println("⚙️ Auth Mode Selected = " + mode);

        if (mode == AuthMode.KEYCLOAK) {
            System.out.println("🔐 Using Keycloak mode");
            accessToken = TokenManager.getAccessToken();
            System.out.println("✅ Generated Token: " + accessToken);

        } else {
            System.out.println("🍪 Using Cookie Authentication mode");
            System.out.println("✅ Skipping Keycloak login — Cookie / x-user-token will be used");
            accessToken = null;
        }

        RestAssured.useRelaxedHTTPSValidation();


    }

    @When("I send and process all {int} import messages from Excel sheet {string}")
    public void i_send_and_process_all_import_messages_from_excel_sheet(
            Integer count, String sheet) throws Exception {

        ExcelReader reader = new ExcelReader();
        List<Map<String, String>> data =
                reader.getData(PropertiesFileReader.getProperty("Fobo.csv"), sheet);

        List<String> failedTestCases = new ArrayList<>();

        for (int i = 0; i < data.size() && i < count; i++) {

            Map<String, String> row = data.get(i);

            if (row.get("testCaseId") == null || row.get("testCaseId").trim().isEmpty()) {
                System.out.println("⛔ Empty row found at index " + i + " — stopping execution.");
                break;
            }

            String testCaseId = row.get("testCaseId");
            int expectedStatus = Integer.parseInt(row.get("expectedHttpStatus"));
            String expectedToewijzing = row.get("toewijzingNummer");

            String identificationToUse = row.get("incomingIdentification");

            Response response = null;
            String responseBody = "";
            int actualStatus = 0;

            int retryCount = 0;
            int maxRetry = 20;

            // 🔁 Duplicate retry loop (console debug only)
            while (true) {

                response = sendRequest(row, identificationToUse);

                responseBody = response.asString();
                actualStatus = response.getStatusCode();

                // Keep retry debug in console only (NOT in report)
                System.out.println("Trying Identification: " + identificationToUse);
                System.out.println("Status: " + actualStatus);

                if (!responseBody.contains("\"code\":\"3000\"")) {
                    break;
                }

                long newId = Long.parseLong(identificationToUse) + 1;
                identificationToUse = String.valueOf(newId);

                retryCount++;

                if (retryCount >= maxRetry) {
                    System.out.println("❌ Max retry reached for " + testCaseId);
                    break;
                }
            }

            // Attach final response JSON once per row
            scenario.attach(responseBody, "application/json", testCaseId + "_response");

            // ---------------- VALIDATION SECTION ----------------

            boolean rowPassed = true;
            StringBuilder failureReason = new StringBuilder();

            // 1️⃣ HTTP validation
            if (actualStatus != expectedStatus) {
                rowPassed = false;
                failureReason.append("Status mismatch.\n");
            }

            // 2️⃣ Business error validation (detailed)
            List<Map<String, Object>> errors =
                    response.jsonPath().getList("errors");

            List<String> errorCodes = new ArrayList<>();

            if (errors != null) {
                for (Map<String, Object> err : errors) {
                    errorCodes.add(String.valueOf(err.get("code")));
                }
            }


            if (errors != null && !errors.isEmpty()) {

                rowPassed = false;
                failureReason.append("Errors:\n");

                for (Map<String, Object> error : errors) {
                    failureReason.append("Code: ")
                            .append(error.get("code"))
                            .append(" | Message: ")
                            .append(error.get("message"))
                            .append("\n");
                }
            }

            // 3️⃣ acceptedContracts validation
            List<?> acceptedContracts =
                    response.jsonPath().getList("acceptedContracts");

            if (acceptedContracts == null || acceptedContracts.isEmpty()) {

                rowPassed = false;
                failureReason.append("acceptedContracts empty.\n");

            } else {

                String actualAccepted =
                        String.valueOf(acceptedContracts.get(0));

                if (!actualAccepted.equals(expectedToewijzing)) {

                    rowPassed = false;
                    failureReason.append("acceptedContracts mismatch.\n")
                            .append("Expected: ").append(expectedToewijzing)
                            .append(" | Actual: ").append(actualAccepted)
                            .append("\n");
                }
            }

            // 4️⃣ Final row result (only one clean log per row)
            executionResults.add(
                    new ExecutionResult(row, actualStatus, errorCodes, responseBody)
            );
            if (rowPassed) {



                scenario.log(
                        "✅ PASS: " + testCaseId +
                                "\nHTTP Status: " + actualStatus +
                                "\nacceptedContracts: " + expectedToewijzing
                );

            } else {

                scenario.log(
                        "❌ FAIL: " + testCaseId +
                                "\nHTTP Status: " + actualStatus +
                                "\n" + failureReason
                );

                failedTestCases.add(testCaseId);
            }
        }

        // ---------------- FINAL RESULT ----------------

        if (!failedTestCases.isEmpty()) {

            this.failedTestCases = failedTestCases;

            scenario.log("⚠️ Some test cases failed in API execution. Count: " + failedTestCases.size());
            scenario.log("Failed TestCases: " + failedTestCases);

        } else {

            scenario.log("🎉 All test cases executed successfully.");
        }

    }
    @Then("I validate message and contract data in database for all processed rows")
    public void i_validate_message_and_contract_data_in_database_for_all_processed_rows() throws Exception {


        StringBuilder reportTable = new StringBuilder();

        reportTable.append(
                String.format("%-12s %-6s %-12s %-10s %-12s %-10s %-10s %-15s %-20s %-6s\n",
                        "Testcase", "Type", "BSN", "Product", "Toewijz",
                        "ExpStat", "ActStat", "ExpErr", "ActErr", "Result")
        );

        reportTable.append("------------------------------------------------------------------------------------------------------------\n");
        System.out.println("🔍 Starting Validation...");

        boolean overallFail = false;

        for (ExecutionResult result : executionResults) {



            Map<String, String> row = result.row;

            String testCaseId = row.get("testCaseId");
            String type = row.get("301/301_MUT");
            String toewijzing = row.get("toewijzingNummer");
            String productCode = row.get("productCode");
            String bsn = row.get("bsn");

            int expectedStatus = Integer.parseInt(row.get("expectedHttpStatus"));
            String expectedErrorCode = row.getOrDefault("ExpectedFoboerrorcode", "").trim();

            int actualStatus = result.actualStatus;
            List<String> actualErrorCodes = result.actualErrorCodes;

            boolean pass = true;
            StringBuilder reason = new StringBuilder();

            if (testCaseId == null || testCaseId.trim().isEmpty()) {
                pass = false;
                reason.append("Empty row in Excel\n");
            }







            // ✅ Status validation
            if (actualStatus != expectedStatus) {
                pass = false;
                reason.append("Status mismatch. Expected: ")
                        .append(expectedStatus)
                        .append(" Actual: ")
                        .append(actualStatus)
                        .append("\n");
            }

            // ✅ Error code validation
            if (expectedErrorCode.isEmpty()) {
                if (actualErrorCodes != null && !actualErrorCodes.isEmpty()) {
                    pass = false;
                    reason.append("Unexpected error codes. Expected: [] Actual: ")
                            .append(actualErrorCodes)
                            .append("\n");
                }
            } else if (!actualErrorCodes.contains(expectedErrorCode)) {
                pass = false;
                reason.append("ErrorCode mismatch. Expected: ")
                        .append(expectedErrorCode)
                        .append(" Actual: ")
                        .append(actualErrorCodes)
                        .append("\n");
            }

            // ✅ Empty row validation
            if (testCaseId == null || testCaseId.trim().isEmpty()) {
                pass = false;
                reason.append("Empty row in Excel\n");
            }
            String resultStatus = pass ? "PASS" : "FAIL";

            reportTable.append(
                    String.format("%-12s %-6s %-12s %-10s %-12s %-10s %-10s %-15s %-20s %-6s\n",
                            testCaseId,
                            type,
                            bsn,
                            productCode,
                            toewijzing,
                            expectedStatus,
                            actualStatus,
                            expectedErrorCode,
                            actualErrorCodes.toString(),
                            resultStatus
                    )
            );

            // ✅ REPORT PRINT
            if (!pass) {
                overallFail = true;
            }

        }
        writeToExcel(executionResults);
        scenario.attach(reportTable.toString(), "text/plain", "Validation Summary Table");

        if (overallFail) {
            Assert.fail("❌ Validation failed. Check report.");

        }

        System.out.println("✅ Validation Completed");
    }

    private Response sendRequest(Map<String, String> row, String identification) {

        String xmlPayload = buildXmlFromRow(row, identification);

        System.out.println("========== REQUEST XML ==========");
        System.out.println(xmlPayload);
        System.out.println("=================================");




        RequestSpecification request = RestAssured.given()
                .relaxedHTTPSValidation()
                .baseUri(PropertiesFileReader.getProperty("Fobo.test"))
                .header("x-user-token", PropertiesFileReader.getProperty("auth.x_user_token"))
                .header("Cookie", PropertiesFileReader.getProperty("auth.cookies"))
                .contentType("application/xml")
                .accept("*/*")
                .redirects().follow(false);

        return request
                .body(xmlPayload)
                .post(row.get("apiEndpoint"));
    }

    // ---------------------------------------------------
    // 🔹 INLINE XML BUILDER METHOD
    // ---------------------------------------------------
    private String buildXmlFromRow(Map<String, String> r, String identification) {


        String messageType = r.get("301/301_MUT");
        boolean isMutation = "301_MUT".equalsIgnoreCase(messageType);

        String redenWijzigingValue = r.get("RedenWijziging");

        boolean isWmo = r.get("flowType").equalsIgnoreCase("WMO");


        String redenWijzigingTag = "";

        if (isMutation && redenWijzigingValue != null && !redenWijzigingValue.isEmpty()) {
            redenWijzigingTag = "<RedenWijziging>" + redenWijzigingValue + "</RedenWijziging>";
        }


        String mainNs = isWmo
                ? "http://www.istandaarden.nl/iwmo/3_2/wmo301/schema"
                : "http://www.istandaarden.nl/ijw/3_2/jw301/schema";

        String baseNs = isWmo
                ? "http://www.istandaarden.nl/iwmo/3_2/basisschema/schema"
                : "http://www.istandaarden.nl/ijw/3_2/basisschema/schema";

        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Bericht xmlns="%s" xmlns:ns2="%s">
                  <Header>
                    <BerichtCode>%s</BerichtCode>
                    <BerichtVersie>%s</BerichtVersie>
                    <BerichtSubversie>%s</BerichtSubversie>
                    <Afzender>%s</Afzender>
                    <Ontvanger>%s</Ontvanger>
                    <BerichtIdentificatie>
                      <ns2:Identificatie>%s</ns2:Identificatie>
                      <ns2:Dagtekening>%s</ns2:Dagtekening>
                    </BerichtIdentificatie>
                    <XsdVersie>
                      <ns2:BasisschemaXsdVersie>%s</ns2:BasisschemaXsdVersie>
                      <ns2:BerichtXsdVersie>%s</ns2:BerichtXsdVersie>
                    </XsdVersie>
                  </Header>
                
                  <Client>
                    <Bsn>%s</Bsn>
                    <Geboortedatum>
                      <ns2:Datum>%s</ns2:Datum>
                    </Geboortedatum>
                    <Geslacht>%s</Geslacht>
                
                    <Naam>
                      <ns2:Geslachtsnaam>
                        <ns2:Achternaam>%s</ns2:Achternaam>
                      </ns2:Geslachtsnaam>
                      <ns2:Voorletters>%s</ns2:Voorletters>
                      <ns2:NaamGebruik>%s</ns2:NaamGebruik>
                    </Naam>
                
                    <Contactgegevens>
                      <Contact>
                        <Soort>01</Soort>
                        <Adres>
                          <ns2:Huis>
                            <ns2:Huisnummer>12</ns2:Huisnummer>
                          </ns2:Huis>
                          <ns2:Postcode>1353AA</ns2:Postcode>
                          <ns2:Straatnaam>Bivak</ns2:Straatnaam>
                          <ns2:Plaatsnaam>Almere</ns2:Plaatsnaam>
                          <ns2:LandCode>NL</ns2:LandCode>
                        </Adres>
                        <Telefoon>
                          <ns2:Telefoon01>
                            <ns2:Telefoonnummer>0612345650</ns2:Telefoonnummer>
                            <ns2:Landnummer>0031</ns2:Landnummer>
                          </ns2:Telefoon01>
                        </Telefoon>
                      </Contact>
                    </Contactgegevens>
                
                    <ToegewezenProducten>
                      <ToegewezenProduct>
                        <ToewijzingNummer>%s</ToewijzingNummer>
                
                        <Product>
                          <ns2:Categorie>%s</ns2:Categorie>
                          <ns2:Code>%s</ns2:Code>
                        </Product>
                
                        <Toewijzingsdatum>%s</Toewijzingsdatum>
                        <Toewijzingstijd>10:00:00</Toewijzingstijd>
                
                        <Ingangsdatum>%s</Ingangsdatum>
                        <Einddatum>%s</Einddatum>
                         %s
                        <Omvang>
                          <ns2:Volume>%s</ns2:Volume>
                          <ns2:Eenheid>%s</ns2:Eenheid>
                          <ns2:Frequentie>%s</ns2:Frequentie>
                        </Omvang>
                      </ToegewezenProduct>
                    </ToegewezenProducten>
                
                  </Client>
                </Bericht>
                """.formatted(
                mainNs, baseNs,
                r.get("berichtCode"),
                r.get("berichtVersie"),
                r.get("berichtSubversie"),
                r.get("afzender"),
                r.get("ontvanger"),
                identification,  // ✅ THIS IS THE ONLY FIX
                r.get("dagtekening"),
                r.get("basisschemaXsdVersie"),
                r.get("berichtXsdVersie"),
                r.get("bsn"),
                r.get("geboortedatum"),
                r.get("geslacht"),
                r.get("achternaam"),
                r.get("voorletters"),
                r.get("naamGebruik"),
                r.get("toewijzingNummer"),
                r.get("productCategorie"),
                r.get("productCode"),
                r.get("dagtekening"),
                r.get("ingangsdatum"),
                r.get("einddatum"),
                redenWijzigingTag,
                r.get("volume"),
                r.get("eenheid"),
                r.get("frequentie")
        );
    }
    // ✅ Excel method (OUTSIDE ExecutionResult)
    private void writeToExcel(List<ExecutionResult> results) throws Exception {

        org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                new org.apache.poi.xssf.usermodel.XSSFWorkbook();
        org.apache.poi.xssf.usermodel.XSSFSheet sheet =
                workbook.createSheet("Results");

        int rowNum = 0;

        // Header
        String[] headers = {"Testcase","Type","BSN","Product","Toewijzing",
                "ExpectedStatus","ActualStatus","ExpectedError","ActualError","Result"};

        org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(rowNum++);

        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Data
        for (ExecutionResult result : results) {

            Map<String, String> row = result.row;

            String testCaseId = row.get("testCaseId");
            String type = row.get("301/301_MUT");
            String bsn = row.get("bsn");
            String product = row.get("productCode");
            String toewijzing = row.get("toewijzingNummer");

            int expectedStatus = Integer.parseInt(row.get("expectedHttpStatus"));
            String expectedError = row.getOrDefault("ExpectedFoboerrorcode", "").trim();

            int actualStatus = result.actualStatus;
            String actualError = result.actualErrorCodes.toString();

            boolean pass = true;

            if (actualStatus != expectedStatus) pass = false;
            if (expectedError.isEmpty()) {
                if (result.actualErrorCodes != null && !result.actualErrorCodes.isEmpty()) {
                    pass = false;
                }
            } else if (!result.actualErrorCodes.contains(expectedError)) {
                pass = false;
            }

            String finalResult = pass ? "PASS" : "FAIL";

            org.apache.poi.ss.usermodel.Row excelRow = sheet.createRow(rowNum++);

            excelRow.createCell(0).setCellValue(testCaseId);
            excelRow.createCell(1).setCellValue(type);
            excelRow.createCell(2).setCellValue(bsn);
            excelRow.createCell(3).setCellValue(product);
            excelRow.createCell(4).setCellValue(toewijzing);
            excelRow.createCell(5).setCellValue(expectedStatus);
            excelRow.createCell(6).setCellValue(actualStatus);
            excelRow.createCell(7).setCellValue(expectedError);
            excelRow.createCell(8).setCellValue(actualError);
            excelRow.createCell(9).setCellValue(finalResult);
        }

        java.io.FileOutputStream fileOut =
                new java.io.FileOutputStream("target/Validation_Report.xlsx");

        workbook.write(fileOut);
        fileOut.close();
        workbook.close();

        System.out.println("📄 Excel report generated: target/Validation_Report.xlsx");
    }


    // ✅ ExecutionResult class (AFTER method)
    class ExecutionResult {

        Map<String, String> row;
        int actualStatus;
        List<String> actualErrorCodes;
        String responseBody;

        public ExecutionResult(Map<String, String> row, int actualStatus,
                               List<String> actualErrorCodes, String responseBody) {
            this.row = row;
            this.actualStatus = actualStatus;
            this.actualErrorCodes = actualErrorCodes;
            this.responseBody = responseBody;
        }
    }
}

