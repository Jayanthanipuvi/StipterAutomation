@NPSLK01
Feature: NPSLK01 BRP Message

  Scenario: Send NPSLK01 message and receive successful response
    Given I send the NPSLK01 message to the TEST BRP endpoint
    Then I should receive a successful NPSLK01 response