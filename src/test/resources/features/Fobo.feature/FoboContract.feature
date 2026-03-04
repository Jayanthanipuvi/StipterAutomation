@Fobo

Feature: GGK 301 Import Automation - WMO and JW

  As a QA engineer
  I want to send WMO and JW 301 messages using Excel data
  And validate API, message processing, and contract creation
  So that the end-to-end flow is verified

  Scenario: Execute 301 import for all rows from Excel and validate results
    Given I have a valid GGK access token or cookie
    When I send and process all 301 import messages from Excel sheet "GGK_301_Full_Input_Expected"
    
    Then I validate message and contract data in database for all processed rows
