@sepajob
Feature: Login Functionality for database Afterrelease

  Scenario:  User logged with correct credential for database for checking Afterrelease
    Given the user enters username and password
    When the user connects to the database
    Then the user executes the select query "SELECT * FROM invoiceline WHERE invoicelinedate > TO_DATE('08-12-2025', 'DD-MM-YYYY') AND status IN (8600, 8700)"
    And the user executes the update query "UPDATE invoiceline SET status = 8400 WHERE invoicelinedate > TO_DATE('08-12-2025', 'DD-MM-YYYY') AND status IN (8600, 8700)"
#
#
#
#
