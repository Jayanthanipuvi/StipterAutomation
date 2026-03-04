#@cak
#Feature: Trigger CAK export jobs in MOP Platform
#
#  Background:
#    Given I have a valid CAK access token
#
#  Scenario Outline: Trigger CAK job and verify the response
#    When I trigger the CAK job for version "<Job>" with BSN "<BSN>" and facilitator "<FacilitatorId>"
#    Then the API response status should be 200
#    And I verify that the job for version "<Job>" has been executed in the table
#
#
#    When I validate the "<Statustype>", "<CakStart>", "<CakEnd>"
#
#
#    Examples:
#      | FacilitatorId | ContractID | BSN       | Product | StartDate  | EndDate    | Job | Statustype | CakStart   | CakEnd     | ExpectedResult |
#      | 24            | 1010574    | 999970471 | jayca   | 01-01-2020 | 01-01-2030 | 3.0 | 1          | 06-08-2024 | 01-01-2030 | pass           |
##      | 24            | 1010572    | 999969596 | jayca   | 21-11-2024 | 29-07-2030 | 2.0 | 2          | 21-11-2024 |            | pass           |
#
#
#
#
