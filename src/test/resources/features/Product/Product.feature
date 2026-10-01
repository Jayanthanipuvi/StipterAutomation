@Product

Feature: Product API Automation

  Scenario: Create and validate Products from Excel
    Given I have valid Product API authentication
    When I create Products from Excel sheet "Product"
    Then Product creation report should be generated
