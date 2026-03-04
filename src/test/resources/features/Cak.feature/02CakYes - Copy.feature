@cak
Feature: CAK Export Workflow

  Background:
    Given I have a valid CAK access token

  Scenario: End-to-End CAK Export Workflow
    When I create, trigger, and wait for CAK jobs for all rows in "CAKvalidation" using job metadata from "CAKJob"
    Then I verify the DB values for all executed rows