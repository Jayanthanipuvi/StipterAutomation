package com.BRP;

import com.qa.util.ExcelReader;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.response.Response;
import org.testng.Assert;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class NpsLk01Steps {

    private static final int LOG_PREVIEW_LIMIT = 2000;
    private static final String EXCEL_PATH =
            "C:\\MOPAutomationFiles\\MOPAutomationExcel\\BRP\\NPSLK01.xlsx";
    private static final String EXCEL_SHEET = "NPSLK01";
    private static final String TEMPLATE_PATH = "src/test/resources/brp/NPSLK01.xml";
    private static final String ENDPOINT =
            "https://test-moplatform.stipter.nl/smop-ws/services/"
                    + "OntvangAsynchroon0301.OntvangAsynchroon0301HttpSoap11Endpoint/";

    private static final RestAssuredConfig REST_ASSURED_CONFIG = RestAssuredConfig.config()
            .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 30000)
                    .setParam("http.socket.timeout", 60000)
                    .setParam("http.connection-manager.timeout", 30000));

    private final NpsLk01DatabaseValidator databaseValidator = new NpsLk01DatabaseValidator();
    private final NpsLk01ExcelReportWriter excelReportWriter = new NpsLk01ExcelReportWriter();
    private final List<NpsLk01ValidationResult> validationResults = new ArrayList<>();

    private Scenario scenario;

    @Before
    public void beforeScenario(Scenario scenario) {
        this.scenario = scenario;
        validationResults.clear();
    }

    @Given("I send the NPSLK01 message to the TEST BRP endpoint")
    public void sendNpsLk01Message() throws Exception {
        String template = Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);
        List<Map<String, String>> testData = new ExcelReader().getData(EXCEL_PATH, EXCEL_SHEET);

        if (testData == null || testData.isEmpty()) {
            throw new IllegalStateException("No test data found in Excel sheet: " + EXCEL_SHEET);
        }

        int validTestCaseCount = 0;
        for (Map<String, String> data : testData) {
            String testCase = data.get("TestCase");
            if (testCase != null && !testCase.isBlank()) {
                validTestCaseCount++;
                executeTestCase(template, data, testCase.trim());
            }
        }

        if (validTestCaseCount == 0) {
            throw new IllegalStateException("No valid TestCase values found in Excel.");
        }
    }

    @Then("I should receive a successful NPSLK01 response")
    public void verifySuccessfulNpsLk01Response() throws Exception {
        if (validationResults.isEmpty()) {
            Assert.fail("No NPSLK01 validation results were collected.");
        }

        Path excelReport = excelReportWriter.write(validationResults);
        String report = buildReport(excelReport);
        scenario.attach(report, "text/plain", "NPSLK01 API and database validation report");
        System.out.println(report);

        boolean allPassed = validationResults.stream().allMatch(NpsLk01ValidationResult::passed);
        Assert.assertTrue(allPassed, "NPSLK01 validation failed. See the Cucumber attachment and " + excelReport);
    }

    private void executeTestCase(String template, Map<String, String> data, String testCase) {
        String bsn = value(data, "sleutelOntvangend");
        NpsLk01ValidationResult result = new NpsLk01ValidationResult(testCase, bsn);
        validationResults.add(result);

        try {
            String xmlRequest = createXmlRequest(template, data);
            System.out.println("Executing NPSLK01 Test Case: " + testCase);
            System.out.println("Generated NPSLK01 XML:\n" + preview(xmlRequest));

            Response response = given()
                    .config(REST_ASSURED_CONFIG)
                    .header("Content-Type", "application/json")
                    .header("Connection", "close")
                    .body(xmlRequest.getBytes(StandardCharsets.UTF_8))
                    .when()
                    .post(ENDPOINT);

            result.setApiStatus(response.statusCode());
            String responseBody = response.asPrettyString();
            scenario.attach(preview(responseBody), "application/xml", testCase + " API response");
            result.addValidation(
                    "XML HTTP status",
                    "200",
                    String.valueOf(response.statusCode()),
                    response.statusCode() == 200,
                    response.statusCode() == 200 ? "" : preview(responseBody)
            );

            if (response.statusCode() == 200) {
                databaseValidator.validate(data, result);
            } else {
                result.addValidation(
                        "Database validation",
                        "Not run when XML submission fails",
                        "Skipped",
                        false,
                        "Database validation skipped because XML submission returned HTTP " + response.statusCode()
                );
            }
        } catch (Exception exception) {
            result.addFailure("XML submission exception", usefulMessage(exception));
            result.addValidation(
                    "Database validation",
                    "Not run when XML submission fails",
                    "Skipped",
                    false,
                    "Database validation skipped because XML submission raised an exception."
            );
        }
    }

    private String createXmlRequest(String template, Map<String, String> data) {
        String xmlRequest = template;
        String[] placeholders = {
                "organisatie", "sleutelOntvangend", "gemeente1", "gemeente2",
                "woonplaatsNaam", "openbareRuimteNaam", "straatnaam", "postcode", "huisnummer"
        };

        for (String placeholder : placeholders) {
            xmlRequest = replace(xmlRequest, placeholder, value(data, placeholder));
        }
        return xmlRequest;
    }

    private String buildReport(Path excelReport) {
        StringBuilder report = new StringBuilder();
        report.append("\n============================================================\n");
        report.append("          NPSLK01 API AND DATABASE VALIDATION REPORT\n");
        report.append("============================================================\n");

        for (NpsLk01ValidationResult result : validationResults) {
            report.append("TestCase: ").append(result.testCase())
                    .append(" | BSN: ").append(result.bsn())
                    .append(" | API: ").append(result.apiStatus())
                    .append(" | DB ID: ").append(result.databaseRecordId() == null ? "" : result.databaseRecordId())
                    .append(" | Overall: ").append(result.overallStatus()).append('\n');
            for (NpsLk01ValidationResult.FieldValidation validation : result.validations()) {
                report.append("  ").append(validation.passed() ? "PASS" : "FAIL")
                        .append(" | ").append(validation.field())
                        .append(" | Expected: [").append(validation.expected())
                        .append("] Actual: [").append(validation.actual()).append(']');
                if (!validation.failureDetails().isBlank()) {
                    report.append(" | ").append(validation.failureDetails());
                }
                report.append('\n');
            }
        }

        report.append("Excel report: ").append(excelReport.toAbsolutePath()).append('\n');
        return report.toString();
    }

    private String replace(String template, String key, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing Excel value for column: " + key);
        }
        return template.replace("${" + key + "}", value);
    }

    private String value(Map<String, String> data, String key) {
        String value = data.get(key);
        return value == null ? "" : value.trim();
    }

    private String preview(String value) {
        if (value == null || value.length() <= LOG_PREVIEW_LIMIT) {
            return value == null ? "" : value;
        }
        return value.substring(0, LOG_PREVIEW_LIMIT)
                + "\n... output truncated for console. Total length: " + value.length();
    }

    private String usefulMessage(Exception exception) {
        return exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
    }
}
