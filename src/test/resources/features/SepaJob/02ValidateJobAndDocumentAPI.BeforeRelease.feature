@sepajob
Feature: Use API to Perform Regression Testing

  Scenario: Configure, Trigger SEPA Job, Fetch ID, and Download Document via REST Assured

    Given the user logs in and generates a TOKEN using auth mode and stores it
    When the user triggers the job API  using the stored TOKEN

    And the user enters username and password
    And the user connects to the database
    And the user getting the ID "select id from message_metadata where messagetype =10155 and status=1 order by statuschangedon desc limit 1"


    Then the user downloads the document from API using the retrieved ID and verifies the response
    Then the user encodes the downloaded document to Base64 and zips it
    Then moves the downloaded file to the designated to before class







#    When the user selects the Quartz Job Controller
#    Then the user clicks on the endpoint `/api/v1/jobs/triggerGroup/{triggerGroup}/triggerName/{triggerName}/trigger`
#    And enters a `triggerGroup` as `"INVOICE_SERVICE"`
#    And inputs a `triggerName` as `"INVOICE_SEPAEXPORT_TRIGGER"`
#    And receives a response command
#
#    And the user enters username and password
#    And the user connects to the database
#    And the user getting the ID "select id from message_metadata where messagetype =10155 and status=1 order by statuschangedon desc limit 1"
#
#    And navigates to the Document Controller
#    And selects the API endpoint `/api/v1/document/download/facilitator/{facilitatorId}/id/{ids}`
#    And enters a `facilitatorId` as `"0"`
#    And user provides a id
#    And clicks the Execute button
#    And verifies the response and downloads the file
#    And moves the downloaded file to the designated folder
#














#   And compares the XML files from before and after the release

