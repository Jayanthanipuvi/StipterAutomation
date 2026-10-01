package com.Declarationvalidation;


import com.qa.factory.RequestSpecFactory;
import com.qa.util.ExcelReader;
import com.qa.util.PropertiesFileReader;
import com.stipter.util.AuthMode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stipter.util.DatabaseUtil;
import com.stipter.util.TokenManager;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;

import org.apache.logging.log4j.Logger;

import org.testng.Assert;



import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static io.restassured.RestAssured.given;
import static org.testng.Assert.assertEquals;


public class Declarationvalidation_01 {
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(Declarationvalidation_01.class);


    private static Response jobTriggerResponse;
    private final StringBuilder invoiceLineReport = new StringBuilder();


    private String accessToken;
    private AuthMode mode;

    ;
    List<Map<String, String>> data;

    ExcelReader reader = new ExcelReader();
    String excelFilePath = PropertiesFileReader.getProperty("Declaration.csv");

    private Scenario scenario;

    @Given("the user logs in, generates a token from the configured authentication endpoint, and stores the token.")
    public void the_user_logs_in_generates_a_token_from_the_configured_authentication_endpoint_and_stores_the_token() {
        // ✅ Load mode from properties first, fallback to System property
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
    }

    @Given("Given the user posts with token using csv file row  {int}")
    public void given_the_user_posts_with_token_using_csv_file_row(Integer int1) {



        try {
            data = reader.getData(excelFilePath, "Data");
            System.out.println("data.size() ------" + data.size());

            if (int1 >= data.size()) {
                throw new IllegalArgumentException("Row number " + int1 + " is out of bounds for Excel data.");
            }

            Map<String, String> rowData = data.get(int1);
            String facilityId = rowData.get("FACILITY_ID");
            String supplierId = rowData.get("SUPPLIER_ID");

            System.out.println("facilityId: from csv = " + facilityId);
            System.out.println("supplierId: from csv = " + supplierId);

            String urlforcsvupload = PropertiesFileReader.getProperty("url.for.csv.upload");
            String endpoint = String.format(urlforcsvupload, facilityId, supplierId);
            System.out.println("Final URI: " + endpoint);

            File fileToUpload = new File(PropertiesFileReader.getProperty("fileToUpload"));
            if (!fileToUpload.exists()) {
                throw new RuntimeException("File not found: " + fileToUpload.getAbsolutePath());
            }

            var req = given().relaxedHTTPSValidation();

            // ✅ Auth Mode Check
            AuthMode mode = AuthMode.from(System.getProperty("auth.mode"));

            if (mode == AuthMode.KEYCLOAK) {
                System.out.println("🔐 Using Keycloak for upload");

                // ✅ Mandatory headers for declaration API when Keycloak is ON
                req.header("Authorization", "Bearer " + accessToken);
                req.header("Cookie", PropertiesFileReader.getProperty("auth.cookies"));
                req.header("x-user-token", PropertiesFileReader.getProperty("auth.x_user_token"));

            } else {
                System.out.println("🍪 Using Cookie Mode for upload");

                // ✅ RequestSpecFactory already injects cookie + x-user-token
                req.spec(RequestSpecFactory.givenApi());
            }

            // ✅ Clear old content-type
            req.header("Content-Type", "");

            // ✅ Set multipart
            req.header("Content-Type", "multipart/form-data");

            Response response = req
                    .baseUri(PropertiesFileReader.getProperty("baseuri"))
                    .header("FACILITY_ID", facilityId)
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

            System.out.println("endpoint: " + endpoint);
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Response Body: " + response.asPrettyString());

            if (response.getStatusCode() != 200) {
                throw new RuntimeException("Upload failed. Status: " + response.getStatusLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while posting the multipart request. Cause: " + e.getMessage(), e);
        }
    }


        @Given("the user run the query for checking the status")
    public void the_user_run_the_query_for_checking_the_status() {

        String query = "select count(*) from table_type_values where \"column\" ='doHandleIncomingMessageFromFileJobLogic' and tablename ='activeJobLogicUtilDoMethod'";

        System.out.println("Job status query" + query);

        int maxAttempts = 20;
        int waitSeconds = 5;

        try {
            String jdbcUrl = PropertiesFileReader.getProperty("jdbcUrl");
            String jdbcusername = PropertiesFileReader.getProperty("jdbcusername");
            String jdbcpassword = PropertiesFileReader.getProperty("jdbcpassword");

            try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcusername, jdbcpassword);
                 Statement stmt = conn.createStatement()) {

                for (int i = 1; i <= maxAttempts; i++) {
                    ResultSet rs = stmt.executeQuery(query);
                    rs.next();
                    int count = rs.getInt("count");
                    System.out.println("🔍 DB Poll Attempt " + i + ": count = " + count);

                    if (count == 0) {
                        System.out.println("✅ DB shows job logic completed. Proceeding.");
                        return;
                    }

                    Thread.sleep(waitSeconds * 8000);
                }

                throw new RuntimeException("❌ Timeout: DB job logic did not complete within expected time.");
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Error while polling DB for job completion", e);
        }
    }


    // ⏰ Utility method for Netherlands time
    private String getCurrentNetherlandsTime() {
        ZonedDateTime netherlandsTime = ZonedDateTime.now(ZoneId.of("Europe/Amsterdam"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return netherlandsTime.format(formatter);
    }


    @Given("the user validates row {int} using {string} from {string}")
    public void the_user_validates_row_using_from(int rowNumber, String queryKey, String csvPropertyKey) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");


        try {
            System.out.println("\n✅ Starting comparison");
            System.out.println("   CSV Property Key: " + csvPropertyKey);

            System.out.println("   Row Number: " + rowNumber);

            List<Boolean> matchResults = new ArrayList<>();
            // 1️⃣ Load CSV path from properties file
            String csvFilePath = PropertiesFileReader.getProperty(csvPropertyKey);
            if (csvFilePath == null || csvFilePath.trim().isEmpty()) {
                throw new RuntimeException("❌ CSV path not found in properties for key: " + csvPropertyKey);
            }

            // 2️⃣ Load CSV data
            ExcelReader reader = new ExcelReader();
            List<Map<String, String>> csvData = reader.getData(csvFilePath, "Declaration");

            if (rowNumber < 0 || rowNumber >= csvData.size()) {
                throw new RuntimeException("❌ Invalid row number: " + rowNumber + " (CSV has " + csvData.size() + " rows)");
            }


            for (Map<String, String> map : csvData) {
                System.out.println("map Row " + map);
                if (map.get("Toewijzingsnr") == "") break;


                LocalDate date = LocalDate.parse(map.get("StartPeriode"), inputFormatter);
                String startPeriod = date.format(outputFormatter);


                date = LocalDate.parse(map.get("EindPeriode"), inputFormatter);
                String endPeriod = date.format(outputFormatter);
                String currentDate = LocalDate.now().format(outputFormatter);
                String tariefExclForQuery = map.get("Tarief(excl)").replace(",", ".");

                String sql_q = String.format("""
                                 select
                                    distinct i.contractid as "Toewijzingsnr",
                                    TO_CHAR(i.productstartdate, 'DD-MM-YYYY') as "StartPeriode",
                                    TO_CHAR(i.productenddate, 'DD-MM-YYYY') as "EindPeriode",
                                    p.vektisproductcode as "ProductCode",
                                    p.name as "ProductNaam",
                                    REPLACE(TO_CHAR(i.amountexclvat, 'FM999990.00'), '.', ',') as "Tarief(excl)",
                                    CAST(i.deliverednumberofunits AS INTEGER) as "#Eenheden",
                                    ir.id as "Rejectreason Id",
                                    ir.name as "Rejectreason Name",
                                    ir.ggkretourcode as "Rejectreason ggkretourcode",
                                    i.rejectreasonremark as "Invoiceline rejectreasonremark"
                                from
                                    invoiceline i
                                join product p on
                                    p.id = i.productid
                                join invoicestatus s on
                                    s.id = i.status
                                left join invoiceline_rejectreason ir on
                                    ir.id = i.invoiceline_rejectreasonid
                                where
                                    i.invoicelinedate is not null
                                    and i.facilitatorid = 24
                                    and i.supplierid = 1133
                                    and i.contractid = '%s'
                                    and i.status in (8500, 8400)
                                    and i.amountexclvat = %s
                                    and i.deliverednumberofunits = %s
                                    and DATE(i.productstartdate) = '%s'
                                    and DATE(i.productenddate) = '%s'
                                    and DATE(i.invoicelinedate) = '%s'
                                """,
                        map.get("Toewijzingsnr"),
                        tariefExclForQuery,
                        map.get("#Eenheden"),
                        startPeriod,
                        endPeriod,
                        currentDate
                );


                // 3️⃣ Select query based on query key
                String query = sql_q;
                System.out.println(query + "correct query");
                if (query == null) {
                    throw new RuntimeException("❌ No query found for key: " + query);
                }

                // 4️⃣ Run SQL and fetch DB results
                List<Map<String, String>> dbResults = executeQuery(query);
                if (dbResults.isEmpty()) {
                    System.out.println("⚠️ No data returned from DB!");
                    return;
                }

                Map<String, String> dbRow = dbResults.get(0);  // Compare only 1 row

                // 5️⃣ Compare and print result
                compareCsvWithDb(map, dbRow, matchResults);


            }

            generate_the_invoice_line_report(1);
            Assert.assertFalse(matchResults.contains(false), "❌ Some fields did not match between CSV and DB results. Check the report for details.");
        } catch (Exception e) {
            throw new RuntimeException("❌ Error in validateCsvRowWithQuery: " + e.getMessage(), e);
        }
    }


    private List<Map<String, String>> executeQuery(String query) {
        List<Map<String, String>> result = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            while (rs.next()) {
                Map<String, String> row = new HashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    String columnName = meta.getColumnLabel(i);
                    String value = rs.getString(i);
                    row.put(columnName, value == null ? "" : value.trim());
                }
                result.add(row);
            }

        } catch (Exception e) {
            throw new RuntimeException("❌ Query execution failed: " + e.getMessage(), e);
        }

        return result;
    }

    private void compareCsvWithDb(Map<String, String> csvRow, Map<String, String> dbRow, List<Boolean> matchResults) {
        List<String> fields = new ArrayList<>(COLUMN_WIDTHS.keySet());

        StringBuilder csvLine = new StringBuilder("Expected Result >> | ");
        StringBuilder dbLine = new StringBuilder("Actual Result  >> | ");
        boolean allMatch = true;

        for (String field : fields) {
            String csvValue = normalize(csvRow.getOrDefault(field, ""));
            String dbValue = normalize(dbRow.getOrDefault(field, ""));
            int width = COLUMN_WIDTHS.getOrDefault(field, 50);

            csvLine.append(pad(csvValue, width)).append(" | ");
            dbLine.append(pad(dbValue, width)).append(" | ");

            if (!csvValue.equalsIgnoreCase(dbValue)) {
                allMatch = false;
                System.out.println("Mismatch field: " + field);
                System.out.println("Expected full value: [" + csvValue + "]");
                System.out.println("Actual full value:   [" + dbValue + "]");
            }
        }

        if (allMatch) {
            dbLine.append("✅");
            //last coloumn comparision
            //need to compare with actual and expected
        } else {
            dbLine.append("❌");
        }

        StringBuilder report = new StringBuilder();
        report.append(csvLine).append("\n");
        report.append(dbLine).append("\n\n");

        matchResults.add(allMatch);
        System.out.println(report.toString());
        invoiceLineReport.append(report.toString());


        System.out.println("Length of report >> " + invoiceLineReport.length());
        // Optional: throw exception if Expected and Actual don't match


    }


    /**
     * Utility to normalize string values for comparison.
     */
    private String normalize(String input) {
        return input == null ? "" : input.trim().replaceAll("\\s+", " ");
    }

    private String pad(String text, int length) {
        if (text == null) text = "";
        if (text.length() >= length) return text.substring(0, length);
        return String.format("%-" + length + "s", text);
    }


    @Then("generate the InvoiceLine report {int}")
    public void generate_the_invoice_line_report(Integer int1) {
        if (invoiceLineReport == null || invoiceLineReport.length() == 0) {
            System.out.println("\n⚠️ No InvoiceLine comparison results to report.");
            if (scenario != null) {
                scenario.attach("⚠️ No InvoiceLine comparison results to report.", "text/plain", "InvoiceLine Report");
            }
            return;
        }

        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("============================================================\n");
        headerBuilder.append("                FINAL INVOICE LINE COMPARISON REPORT\n");
        headerBuilder.append("============================================================\n");

        // 📌 Build the header using column widths
        for (String field : COLUMN_WIDTHS.keySet()) {
            headerBuilder.append(pad(field, COLUMN_WIDTHS.get(field))).append(" ");
        }
        headerBuilder.append("\n");


        String footer =
                "============================================================\n" +
                        "                     END OF REPORT\n" +
                        "============================================================\n";

        String finalReport = headerBuilder.toString() + invoiceLineReport.toString() + "\n" + footer;

        // ✅ Print to console
        System.out.println(finalReport);

        // ✅ Attach to Cucumber report
        if (scenario != null) {
            scenario.attach(finalReport, "text/plain", "InvoiceLine Report");
        }
    }

    @Before
    public void beforeScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    @When("the user triggers the job API {string} using the stored TOKEN from csv row {int} expecting {int}")
    public void the_user_triggers_the_job_api_with_expected_status(String urlTemplate, Integer rowNumber, Integer expectedStatus) {
        String netherlandsTime = getCurrentNetherlandsTime();
        System.out.println("⏱️ Job Trigger Start Time (Netherlands): " + netherlandsTime);

        try {
            // Make sure Excel was read
            if (data == null || data.isEmpty()) {
                data = reader.getData(excelFilePath, "Data");
            }

            if (rowNumber >= data.size()) {
                throw new IllegalArgumentException("Row number " + rowNumber + " is out of bounds for Excel data.");
            }

            Map<String, String> rowData = data.get(rowNumber);

            String triggerGroup = rowData.get("TRIGGER_GROUP");
            String triggerName = rowData.get("TRIGGER_NAME");
            String facilityId = rowData.get("FACILITY_ID");

            String finalUrl = String.format(urlTemplate, triggerGroup, triggerName);
            System.out.println("🔗 Final Job URL: " + finalUrl);

            // Build JSON payload
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("manualTrigger", true);
            if (facilityId != null && !facilityId.trim().isEmpty()) {
                rootNode.put("invoicehandleuploaded.cs_facilitatorIds", Integer.parseInt(facilityId));
            } else {
                System.out.println("⚠️ FacilityId is empty in Excel row");
            }

            String jsonBody = mapper.writeValueAsString(rootNode);
            System.out.println("📦 JSON Payload: " + jsonBody);

            int retryCount = 0;
            int maxRetries = 6;
            int statusCode = 0;

            while (retryCount < maxRetries) {
                jobTriggerResponse = given()
                        .relaxedHTTPSValidation()
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Content-Type", "application/json")
                        .body(jsonBody)
                        .when()
                        .get(finalUrl);

                statusCode = jobTriggerResponse.getStatusCode();
                System.out.println("Attempt " + (retryCount + 1) + " - Status: " + statusCode);

                // Retry only if not matching expected
                if (statusCode == expectedStatus) {
                    System.out.println("✅ Expected status received: " + statusCode);
                    break;
                }

                Thread.sleep(2000);
                retryCount++;
            }

            // Final assert
            System.out.println("✅ Final Status Code after retries: " + statusCode);

            assertEquals(
                    statusCode,
                    expectedStatus.intValue(),
                    "❌ Unexpected final status code!"
            );

        } catch (Exception e) {
            throw new RuntimeException("❌ Error while triggering Job API", e);
        }
    }


    @Given("the user triggers the job API using the stored TOKEN {int}")
    public void the_user_triggers_the_job_api_using_the_stored_token(Integer rowNumber) {
        try {
            if (data == null || data.isEmpty()) {
                data = reader.getData(excelFilePath, "Data");
            }

            if (rowNumber >= data.size()) {
                throw new IllegalArgumentException("Row number " + rowNumber + " is out of bounds.");
            }

            Map<String, String> rowData = data.get(rowNumber);

            String triggerGroup = rowData.get("TRIGGER_GROUP");
            String triggerName = rowData.get("TRIGGER_NAME");

            String urlTemplate = PropertiesFileReader.getProperty("job.trigger.url");
            String finalUrl = String.format(urlTemplate, triggerGroup, triggerName);

            System.out.println("🔗 Triggering Job URL: " + finalUrl);

            var req = given().relaxedHTTPSValidation();

            // ✅ Auth Mode logic
            AuthMode mode = AuthMode.from(PropertiesFileReader.getProperty("auth.mode"));

            if (mode == AuthMode.KEYCLOAK) {
                System.out.println("🔐 Using Bearer Token for Job Trigger");
                req.header("Authorization", "Bearer " + accessToken);
            } else {
                System.out.println("🍪 Using Cookie Mode for Job Trigger");
                req.spec(RequestSpecFactory.givenApi());
            }

            Response resp = req
                    .header("Content-Type", "application/json")
                    .when()
                    .get(finalUrl)
                    .then()
                    .log().all()
                    .extract().response();

            System.out.println("Job Trigger Status: " + resp.getStatusCode());

            if (resp.getStatusCode() != 200 && resp.getStatusCode() != 204) {
                throw new RuntimeException("❌ Job trigger failed with status: " + resp.getStatusCode());
            }

            System.out.println("✅ Job Trigger Successful");

        } catch (Exception e) {
            throw new RuntimeException("❌ Error triggering job API: " + e.getMessage(), e);
        }
    }

    private static final Map<String, Integer> COLUMN_WIDTHS = new LinkedHashMap<>();

    static {

        COLUMN_WIDTHS.put("Toewijzingsnr", 12);
        COLUMN_WIDTHS.put("StartPeriode", 12);
        COLUMN_WIDTHS.put("EindPeriode", 12);
        COLUMN_WIDTHS.put("ProductCode", 12);
        COLUMN_WIDTHS.put("ProductNaam", 16);
        COLUMN_WIDTHS.put("Tarief(excl)", 16);
        COLUMN_WIDTHS.put("#Eenheden", 12);
        COLUMN_WIDTHS.put("Rejectreason Id", 12);
        COLUMN_WIDTHS.put("Rejectreason Name", 16);
        COLUMN_WIDTHS.put("Rejectreason ggkretourcode", 12);
        COLUMN_WIDTHS.put("Invoiceline rejectreasonremark", 16);

        //need to handle with matched and unmatched
    }

}






