@AppealContract

Feature: Appeal API Testing

  Scenario: Validate Appeal Creation from Excel
    Given I have a valid GGK access token or cookie
    When I send all appeal requests from Excel sheet "Appeal"
    Then I validate appeal responses