 package com.SepajobValidation;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import com.qa.util.PropertiesFileReader;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;




public class LoginDatabase001 {
    public static String ids;
    Connection conn;
    public static void main(String[] args) {
        LoginDatabase001 lo = new LoginDatabase001();
        lo.the_user_enters_username_and_password();
    }

    @Given("the user enters username and password")
    public void the_user_enters_username_and_password() {
        String jdbcUrl = PropertiesFileReader.getProperty("jdbcUrl");
        String jdbcusername = PropertiesFileReader.getProperty("jdbcusername");
        System.out.println("what is jdbcusername??" + jdbcusername);
        String jdbcpassword = PropertiesFileReader.getProperty("jdbcpassword");

        try {
            conn = DriverManager.getConnection(jdbcUrl, jdbcusername, jdbcpassword);
            System.out.println("✅ Connected to the database successfully!");
        } catch (Exception e) {
            System.err.println("❌ Failed to connect: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @When("the user connects to the database")
    public void the_user_connects_to_the_database() {
        // Connection already established in the previous step
        if (conn != null) {
            System.out.println(" Database connection is active.");
        } else {
            System.err.println(" Database connection is not active.");
        }
    }

    @Then("the user executes the select query {string}")
    public void the_user_executes_the_select_query(String string) {

        String query = "SELECT * FROM invoiceline " +
                "WHERE invoicelinedate > TO_DATE('19-01-2026', 'DD-MM-YYYY') " +
                "AND status IN (8600, 8700)";

        System.out.println(query + "Send query");

        try (PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            if (!rs.isBeforeFirst()) {
                System.out.println("❌ No records found.");
            } else {
                while (rs.next()) {
                    System.out.println("📄 Record ID: " + rs.getString("id"));
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Error executing SELECT query: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Then("the user executes the update query {string}")
    public void the_user_executes_the_update_query(String string) {
        String updateQuery =
                "UPDATE invoiceline " +
                        "SET status = 8400 " +
                        "WHERE invoicelinedate > TO_DATE('19-01-2026', 'DD-MM-YYYY') " +
                        "AND status IN (8600, 8700)";
        System.out.println(updateQuery + "update query");
        try (PreparedStatement stmt = conn.prepareStatement(updateQuery)) {

            int rowsAffected = stmt.executeUpdate();
            System.out.println("🛠️ Rows updated: " + rowsAffected);

        } catch (Exception e) {
            System.err.println("❌ Error executing UPDATE query: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Then("the user getting the ID {string}")
    public void the_user_getting_the_id(String string) {
         String gettingIdquery = string;

        try (PreparedStatement stmt2 = conn.prepareStatement(gettingIdquery);
             ResultSet rs2 = stmt2.executeQuery()) {

            while (rs2.next()) {
               ids = rs2.getString("id");


                System.out.println("📄 Record ID: " + rs2.getString("id"));

            }

        } catch (Exception e) {
            System.err.println("❌ Error executing SELECT query: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
