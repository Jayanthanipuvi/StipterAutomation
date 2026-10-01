@AppealContract
Feature: Detect OS

  Scenario: Determine the operating system
    Given the system OS is detected
    Then verify the OS is correctly identified
