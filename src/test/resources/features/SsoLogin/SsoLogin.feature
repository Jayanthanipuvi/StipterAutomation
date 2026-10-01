@SSO
Feature: MOP Microsoft SSO Login

  As a QA engineer
  I want to login to MOP using Microsoft SSO
  So that I can verify the SSO login flow

  Scenario: Successfully login to MOP using Microsoft SSO
    Given I open the MOP SSO login page
    When I login using the configured Microsoft account
    Then I should be successfully logged into the MOP dashboard