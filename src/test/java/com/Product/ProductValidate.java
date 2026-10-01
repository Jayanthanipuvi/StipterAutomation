package com.Product;

import com.qa.factory.RequestSpecFactory;
import com.qa.util.ExcelReader;
import com.qa.util.PropertiesFileReader;
import com.stipter.util.AuthMode;
import com.stipter.util.TokenManager;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.Assert;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductValidate {

    private static final String STEP_ONE_ENDPOINT =
            "/api/v1/product/validate/wizard/step1?form=true";
    private static final String STEP_TWO_ENDPOINT =
            "/api/v1/product/validate/wizard/step2";
    private static final String STEP_THREE_ENDPOINT =
            "/api/v1/product/validate/wizard/step3?form=true";
    private static final String CREATE_ENDPOINT =
            "/api/v1/product/create";

    private final ProductRequestBuilder requestBuilder = new ProductRequestBuilder();
    private final ProductDBValidation dbValidation = new ProductDBValidation();
    private final List<ProductRunResult> results = new ArrayList<>();

    private Scenario scenario;
    private AuthMode mode;

    @Before
    public void beforeScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    @Given("I have valid Product API authentication")
    public void i_have_valid_product_api_authentication() {
        String configMode = PropertiesFileReader.getProperty("auth.mode");

        if (configMode == null || configMode.trim().isEmpty()) {
            configMode = System.getProperty("auth.mode");
        }

        mode = AuthMode.from(configMode);
        System.out.println("Auth Mode Selected = " + mode);

        if (mode == AuthMode.KEYCLOAK) {
            TokenManager.getAccessToken();
            System.out.println("Generated Keycloak token");
        } else {
            System.out.println("Using Cookie / x-user-token authentication");
        }

        RestAssured.useRelaxedHTTPSValidation();
    }

    @When("I create Products from Excel sheet {string}")
    public void i_create_products_from_excel_sheet(String sheetName) throws Exception {
        results.clear();

        String excelPath = resolveProductExcelPath();
        List<Map<String, String>> rows = new ExcelReader().getData(excelPath, sheetName);
        String runId = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        int rowNumber = 1;
        for (Map<String, String> row : rows) {
            rowNumber++;
            if (isBlankRow(row)) {
                continue;
            }
            createProductForRow(row, rowNumber, runId);
        }

        if (results.isEmpty()) {
            Assert.fail("No Product rows found in sheet '" + sheetName + "' from " + excelPath);
        }
    }

    @Then("Product creation report should be generated")
    public void product_creation_report_should_be_generated() {
        String report = buildReport();
        scenario.attach(report, "text/plain", "Product creation report");
        System.out.println(report);

        long failed = results.stream().filter(result -> !result.passed()).count();
        Assert.assertEquals(failed, 0, "Product rows failed. See Product creation report.");
    }

    private void createProductForRow(Map<String, String> row, int rowNumber, String runId) {
        ProductRunResult result = new ProductRunResult(rowNumber);
        results.add(result);

        try {
            ProductRequestBuilder.ProductPayloads payloads =
                    requestBuilder.buildPayloads(row, rowNumber, runId);
            result.productData = payloads.productData();

            if (!executeStep(result, "step1", STEP_ONE_ENDPOINT, payloads.stepOnePayload())) {
                return;
            }
            if (!executeStep(result, "step2", STEP_TWO_ENDPOINT, payloads.stepTwoPayload())) {
                return;
            }
            if (!executeStep(result, "step3", STEP_THREE_ENDPOINT, payloads.stepThreePayload())) {
                return;
            }

            Response createResponse = postJson(CREATE_ENDPOINT, payloads.stepThreePayload());
            result.addApiStep("create", createResponse);
            attachResponse(rowNumber, "create", createResponse);

            if (!isResponseSuccessful(createResponse)) {
                result.fail("create failed: HTTP " + createResponse.getStatusCode() + " " + safeBody(createResponse));
                return;
            }

            result.productId = extractProductId(createResponse);
            ProductDBValidation.ValidationResult dbResult =
                    dbValidation.validate(result.productId, payloads.productData());
            result.dbMessage = dbResult.message();

            if (!dbResult.passed()) {
                result.fail("DB validation failed: " + dbResult.message());
                return;
            }

            result.pass();
        } catch (Exception e) {
            result.fail(e.getMessage());
        }
    }

    private boolean executeStep(ProductRunResult result, String step, String endpoint, String payload) {
        Response response = postJson(endpoint, payload);
        result.addApiStep(step, response);
        attachResponse(result.rowNumber, step, response);

        if (!isResponseSuccessful(response)) {
            result.fail(step + " failed: HTTP " + response.getStatusCode() + " " + safeBody(response));
            return false;
        }

        return true;
    }

    private Response postJson(String endpoint, String payload) {
        return RestAssured.given()
                .spec(RequestSpecFactory.givenApi())
                .contentType(ContentType.JSON)
                .body(payload)
                .log().all()
                .when()
                .post(endpoint)
                .then()
                .log().all()
                .extract().response();
    }

    private boolean isResponseSuccessful(Response response) {
        String body = safeBody(response);
        int status = response.getStatusCode();
        return status >= 200 && status < 300 && !containsBusinessError(body);
    }

    private boolean containsBusinessError(String body) {
        if (body == null || body.isBlank()) {
            return false;
        }

        String normalized = body.toLowerCase(Locale.ROOT).replace(" ", "");
        return normalized.contains("\"errors\":[{")
                || normalized.contains("\"success\":false");
    }

    private int extractProductId(Response response) {
        String id = response.jsonPath().getString("ID");
        if (id == null || id.isBlank()) {
            id = response.jsonPath().getString("id");
        }
        if (id == null || id.isBlank()) {
            id = response.jsonPath().getString("data.ID");
        }
        if (id == null || id.isBlank()) {
            id = response.jsonPath().getString("data.id");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Create response did not return Product ID. Body: " + safeBody(response));
        }
        return Integer.parseInt(id);
    }

    private void attachResponse(int rowNumber, String step, Response response) {
        scenario.attach(
                safeBody(response),
                "application/json",
                "Product row " + rowNumber + " " + step + " response"
        );
    }

    private String safeBody(Response response) {
        try {
            String body = response.asString();
            return body == null ? "" : body;
        } catch (Exception e) {
            return "";
        }
    }

    private boolean isBlankRow(Map<String, String> row) {
        return row.values().stream().allMatch(value -> value == null || value.isBlank());
    }

    private String resolveProductExcelPath() {
        String configured = PropertiesFileReader.getProperty("Product.csv");
        if (configured == null || configured.isBlank()) {
            configured = "C:/MOPAutomationFiles/MOPAutomationExcel/Newproduct/ProductData.xlsx";
        }

        File file = new File(configured);
        if (file.isDirectory()) {
            return new File(file, "ProductData.xlsx").getAbsolutePath();
        }
        return configured;
    }

    private String buildReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n============================================================\n");
        report.append("                 PRODUCT CREATION REPORT\n");
        report.append("============================================================\n");
        report.append(String.format("%-8s %-12s %-35s %-12s %-10s %-8s %-10s %s%n",
                "Row", "ProductId", "ProductName", "ProductCode", "API", "DB", "Result", "Message"));
        report.append("------------------------------------------------------------------------------------------------------------\n");

        for (ProductRunResult result : results) {
            ProductRequestBuilder.ProductData data = result.productData;
            report.append(String.format(
                    "%-8s %-12s %-35s %-12s %-10s %-8s %-10s %s%n",
                    result.rowNumber,
                    result.productId == 0 ? "" : result.productId,
                    data == null ? "" : data.productName(),
                    data == null ? "" : data.vektisCode(),
                    result.apiSummary(),
                    result.dbMessage == null ? "" : (result.dbMessage.isBlank() ? "" : "CHECKED"),
                    result.passed() ? "PASS" : "FAIL",
                    result.message
            ));
        }

        return report.toString();
    }

    private static class ProductRunResult {
        private final int rowNumber;
        private final List<String> apiSteps = new ArrayList<>();
        private ProductRequestBuilder.ProductData productData;
        private int productId;
        private String dbMessage = "";
        private String message = "";
        private boolean passed;

        private ProductRunResult(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        private void addApiStep(String step, Response response) {
            apiSteps.add(step + ":" + response.getStatusCode());
        }

        private String apiSummary() {
            return String.join(",", apiSteps);
        }

        private void pass() {
            this.passed = true;
            this.message = "Created and DB matched";
        }

        private void fail(String message) {
            this.passed = false;
            this.message = message == null ? "" : message;
        }

        private boolean passed() {
            return passed;
        }
    }
}
