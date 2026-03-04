package com.SepajobValidation;

import com.qa.util.PropertiesFileReader;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
//import com.qa.util.PropertiesFileReader;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.http.ContentType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.file.Path;
import java.sql.*;
import java.util.*;


import static com.SepajobValidation.LoginDatabase001.ids;
import static io.restassured.RestAssured.given;

import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;




public class ValidateJobAndDocumentAPI {
    public static Response documentResponse;
     // This should be set in your DB ID fetch method

    public static String  latestRecordId ;
    private final StringBuilder comparisonLog = new StringBuilder(); // global or class-level
    private Scenario scenario;

    @io.cucumber.java.Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
    }

    //    public static String messageid;
    private String messageid;
    public String accessToken;
    private String documentBase64Content;

    public static final Logger logger = LogManager.getLogger(ValidateJobAndDocumentAPI.class);
    Connection conn;

    private static Response jobTriggerResponse;
    // ✅ for token storage
    private static Integer expectedStatus;   // ✅ to store status
    private static Integer retrievedId;      // ✅ to store latest id
    private static String apiUrlTemplate;    // ✅ to build final document URL

    @Given("the user logs in and generates a TOKEN using auth mode and stores it")
    public void the_user_logs_in_and_generates_a_token_using_auth_mode_and_stores_it() {

        System.out.println("📁 Config Loaded From: " + System.getProperty("envConfigPath"));
        // Read mode safely
        String mode = PropertiesFileReader.getProperty("auth.mode");

        if (mode == null || mode.trim().isEmpty()) {
            System.out.println("⚠️ auth.mode not set — defaulting to COOKIE_MODE");
            mode = "COOKIE_MODE";
        }

        System.out.println("⚙️ Auth Mode Selected = " + mode);

        if (mode.equalsIgnoreCase("KEYCLOAK")) {
            System.out.println("🔐 Using Keycloak Authentication, fetching token...");
            com.stipter.util.TokenManager.getAccessToken();

        } else {
            System.out.println("🍪 Using Cookie Authentication mode");
            System.out.println("✅ Skipping Keycloak login — x-user-token & Cookie will be used");
        }
    }


    @When("the user triggers the job API  using the stored TOKEN")
    public void the_user_triggers_the_job_api_using_the_stored_token() {

        try {
            int retryCount = 0;
            int maxRetries = 2; // Retry for up to ~2 minutes
            int statusCode = 0;
            String triggerjobapi = PropertiesFileReader.getProperty("triger.sepa.job");
            while (retryCount < maxRetries) {
                jobTriggerResponse = given()
                        .spec(com.qa.factory.RequestSpecFactory.givenApi())
                        .when()
                        .get(triggerjobapi);

                statusCode = jobTriggerResponse.getStatusCode();
                System.out.println("Attempt " + (retryCount + 1) + " - Status: " + statusCode);

                if (statusCode == 200) {
                    System.out.println("✅ Job triggered successfully with status 200.");
                    return; // Exit the method and go to the next step
                }

                Thread.sleep(2000); // Wait before retrying
                retryCount++;
            }

            // If 200 was never received
            throw new RuntimeException("❌ Did not receive status 200 after retries. Last status: " + statusCode);

        } catch (Exception e) {
            throw new RuntimeException("Error while triggering Job API", e);
        }
    }





    @Then("the user downloads the document from API using the retrieved ID and verifies the response")
    public void the_user_downloads_the_document_from_api_using_the_retrieved_id_and_verifies_the_response() throws InterruptedException {

        try {

            Thread.sleep(2000);
            System.out.println("Original ID got: " + ids);

            if (ids == null || ids.isEmpty()) {
                throw new RuntimeException("No valid ID was retrieved for document download");
            }
            String downloadapijob = PropertiesFileReader.getProperty("download.document.api");

            String finalUrl = downloadapijob.replace("{ids}", ids);
            System.out.println("📤 Download document API: " + finalUrl);

            Response documentResponse = given()
                    .spec(com.qa.factory.RequestSpecFactory.givenApi())
                    .when()
                    .get(finalUrl);
            int statusCode = documentResponse.getStatusCode();
            System.out.println("📄 Document Download API Status Code: " + statusCode);

            if (statusCode == 200) {
                System.out.println("✅ Document download successful.");

                // Get byte array of the HTML file
                byte[] htmlBytes = documentResponse.asByteArray();

                // Convert to Base64 string
                String base64Encoded = Base64.getEncoder().encodeToString(htmlBytes);

                // ✅ Print the FULL Base64 encoded output
                System.out.println("📄 Full Base64 Encoded Output:");
                System.out.println(base64Encoded);

                // Optional: store it if needed for next steps
                this.documentBase64Content = base64Encoded;

            } else {
                throw new RuntimeException("❌ Failed to download document. Status: " + statusCode);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error while downloading or encoding the document", e);
        }
    }
    @Then("the user encodes the downloaded document to Base64 and zips it")
    public void the_user_encodes_and_zips() {
        try {
            // 1. Base64 string must be present
            if (documentBase64Content == null || documentBase64Content.isEmpty()) {
                throw new RuntimeException("Base64 content is empty or not available.");
            }

            // 2. Decode Base64 to binary
            byte[] zipBytes = Base64.getDecoder().decode(documentBase64Content);

            // 3. Target path (make sure it's set in config.properties)
            String outputPath = PropertiesFileReader.getProperty("downloadedZipOutput"); // e.g. C:\\Users\\IP0029\\Downloads
            String outputFileName = "converted_document.zip";

            File outputFile = new File(outputPath, outputFileName);
            Files.write(outputFile.toPath(), zipBytes); // Save the decoded ZIP bytes

            System.out.println("✅ ZIP file successfully created and saved at: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            throw new RuntimeException("❌ Failed to convert and save Base64 ZIP file", e);
        }
    }
    @Then("moves the downloaded file to the designated to before class")
    public void moves_the_downloaded_file_to_the_designated_folder() {
        try {
            // Read paths from properties
            String downloadFolderPath = PropertiesFileReader.getProperty("downloadedZipOutput");
            String targetFolderPath = PropertiesFileReader.getProperty("beforeReleaseTargetFolder");

            File downloadFolder = new File(downloadFolderPath);
            File targetFolder = new File(targetFolderPath);

            // Clean target folder
            if (targetFolder.exists()) {
                for (File file : targetFolder.listFiles()) {
                    file.delete();
                }
            } else {
                targetFolder.mkdirs();
            }

            // Find latest ZIP file
            File latestZip = getLatestZIPFile(downloadFolder);
            if (latestZip == null) {
                throw new RuntimeException("⚠ No ZIP file found in download folder.");
            }

            Path destination = targetFolder.toPath().resolve(latestZip.getName());
            Files.move(latestZip.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ Moved ZIP to folder: " + destination);

            // Unzip it
            unzip(destination.toFile(), targetFolder);

        } catch (Exception e) {
            throw new RuntimeException("❌ Error while moving or extracting ZIP", e);
        }
    }

    private File getLatestZIPFile(File folder) {
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));
        if (files == null || files.length == 0) return null;
        return Arrays.stream(files).max(Comparator.comparingLong(File::lastModified)).orElse(null);
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zis.getNextEntry();
            while (entry != null) {
                File newFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                entry = zis.getNextEntry();
            }
            zis.closeEntry();
        }


    }

    @Then("Moves the downloaded file to  After release designated folder")
    public void moves_the_downloaded_file_to_the_Afterreleasedesignated_folder() {
        try {
            File downloadFolder = new File(PropertiesFileReader.getProperty("downloadedZipOutput"));
            File afterReleaseTargetFolder = new File(PropertiesFileReader.getProperty("afterReleaseTargetFolder"));

            if (afterReleaseTargetFolder.exists()) {
                for (File file : afterReleaseTargetFolder.listFiles()) file.delete();
            } else {
                afterReleaseTargetFolder.mkdirs();
            }

            File latestZipFile = getLatestZIPFile(downloadFolder);
            if (latestZipFile == null) throw new RuntimeException("⚠ No ZIP file found in the download folder.");

            Path destination = afterReleaseTargetFolder.toPath().resolve(latestZipFile.getName());
            Files.move(latestZipFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ ZIP file moved to after release folder: " + destination);
            unzip(destination.toFile(), afterReleaseTargetFolder);

        } catch (Exception e) {
            throw new RuntimeException("❌ Error while moving the after release ZIP file.", e);
        }


    }
    @Then("compares the XML files from before and after the release")
    public void compares_the_xml_files_from_before_and_after_the_release() throws Exception {
        Thread.sleep(3000); // wait before execution

        // Fetching folders from properties file
        File beforeFolder = new File(PropertiesFileReader.getProperty("beforeReleaseTargetFolder"));
        File afterFolder = new File(PropertiesFileReader.getProperty("afterReleaseTargetFolder"));

        File file1 = findXmlInFolder(beforeFolder);
        File file2 = findXmlInFolder(afterFolder);

        if (file1 == null || file2 == null) {
            throw new RuntimeException("⚠ Could not find XML files in before/after release folders.");
        }

        System.out.println("📁 Comparing XML files:");
        System.out.println("Before: " + file1.getName());
        System.out.println("After : " + file2.getName());
        System.out.println("===================================");

        // Compare document structure
        compareXmlDocuments(file1, file2);
    }


    private File findXmlInFolder(File folder) {
        File[] xmlFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".xml"));
        if (xmlFiles == null || xmlFiles.length == 0) return null;
        return Arrays.stream(xmlFiles)
                .max(Comparator.comparingLong(File::lastModified))
                .orElse(null);
    }

    private void compareXmlDocuments(File file1, File file2) throws Exception {
        Document doc1 = parseXml(file1);
        Document doc2 = parseXml(file2);

        // Compare transactions under CdtTrfTxInf
        compareTransactions(doc1, doc2);
    }

    private void compareTransactions(Document doc1, Document doc2) throws InterruptedException {
        NodeList txList1 = doc1.getElementsByTagName("CdtTrfTxInf");
        NodeList txList2 = doc2.getElementsByTagName("CdtTrfTxInf");

        int count1 = txList1.getLength();
        int count2 = txList2.getLength();

//        logMessage("\n=== TRANSACTION COMPARISON: CdtTrfTxInf ===", scenario);

        if (count1 != count2) {
            logMessage("❌ Transaction count mismatch: Before=" + count1 + ", After=" + count2, scenario);
            return;
        }

        for (int i = 0; i < count1; i++) {
            logMessage("\n Invoice Line " + (i + 1) + "", scenario);
            Element tx1 = (Element) txList1.item(i);
            Element tx2 = (Element) txList2.item(i);
            compareTransactionElements(tx1, tx2);
            comparisonLog.append("-------------------------------------------------------------\n");
        }
    }

    private void logMessage(String message, Scenario scenario) {
        System.out.println(message);
        comparisonLog.append(message).append(System.lineSeparator()); // append to report
        if (scenario != null) scenario.log(message); // optional in-line report log
    }


    private void compareTransactionElements(Element tx1, Element tx2) throws InterruptedException {
        compareChildElement("InstrId", "PmtId", tx1, tx2);
        compareChildElement("EndToEndId", "PmtId", tx1, tx2);
        compareChildElement("IBAN", "CdtrAcct/Id", tx1, tx2);
        compareChildElement("Transaction Amountt", "Amt", tx1, tx2);
        compareChildElement("Nm", "Cdtr", tx1, tx2);

        compareChildElement("Ustrd", "RmtInf", tx1, tx2);


//        compareChildElement("InstrId", "PmtId", tx1, tx2);
//        compareChildElement("EndToEndId", "PmtId", tx1, tx2);
//        compareChildElement("InstdAmt", "Amt", tx1, tx2);
//        compareChildElement("Nm", "Cdtr", tx1, tx2);
//        compareChildElement("IBAN", "CdtrAcct/Id", tx1, tx2);
//        compareChildElement("Ustrd", "RmtInf", tx1, tx2);
    }

    private void compareChildElement(String elementName, String parentPath, Element parent1, Element parent2) throws InterruptedException {
        String val1 = getNestedElementValue(parent1, parentPath + "/" + elementName);
        String val2 = getNestedElementValue(parent2, parentPath + "/" + elementName);
        logComparison(elementName, val1, val2);
    }

    private void logComparison(String fieldName, String val1, String val2) throws InterruptedException {
        String status = val1.equals(val2) ? "✅ MATCH" : "❌ MISMATCH";

        String output = String.format(
                "%-15s | Before Release: %-20s | After Release: %-20s | Status: %s%n",
                fieldName, val1, val2, status
        );

        System.out.println(output);


        comparisonLog.append(output);


    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private String getNestedElementValue(Element parent, String path) {
        String[] parts = path.split("/");
        NodeList nodes = parent.getElementsByTagName(parts[0]);
        if (nodes.getLength() == 0) return "N/A";

        if (parts.length == 1) {
            return nodes.item(0).getTextContent().trim();
        } else {
            return getNestedElementValue((Element) nodes.item(0), path.substring(path.indexOf('/') + 1));
        }


    }
    @Before
    public void beforeScenario(Scenario scenario) {
        this.scenario = scenario;
    }


    @Then("attach the comparison result to reports")
    public void attachComparisonToReport()  {


        if (scenario != null && comparisonLog.length() > 0) {
            scenario.attach(comparisonLog.toString(), "text/plain", "XML Comparison Summary");
        }
    }

}

