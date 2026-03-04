
Feature: Invoice Declaration Validation

  @declaration
    Scenario Outline: Validate invoice declaration process
      Given the user logs in, generates a token from the configured authentication endpoint, and stores the token.
      And Given the user posts with token using csv file row  <RowNumber>

      And the user triggers the job API using the stored TOKEN <RowNumber>

      And the user run the query for checking the status

      Examples:

        | RowNumber |
        | 0         |

#
#
#
#
##      //clean up
##      //api properties
##      //WIKI
##
##
##
##
##      //smoke, regression etc
#
#
#
#
#
#
#
#
