#@declaration
#Feature: Declaration Upload Negative Scenarios
#
#  Background:
#    Given the user has valid credentials
#    And an access token is generated
#
#  Scenario: Upload with empty facilitator ID
#    When the user posts to "API.forCSV.Validation" with facilitatorId "" and supplierId "3874" using valid file "fileToUpload"
#    Then the response status should be like a 404
#
#  Scenario: Upload with empty supplier ID
#    When the user posts to "API.forCSV.Validation" with facilitatorId "24" and supplierId "" using valid file "fileToUpload"
#    Then the response status should be like a 404
#
#  Scenario: Upload with missing file
#    When the user posts to "API.forCSV.Validation" with facilitatorId "24" and supplierId "3874" without a file
#    Then the response status should be like a 500
#
#  Scenario: Upload with wrong file type (Excel instead of CSV)
#    When the user posts to "API.forCSV.Validation" with facilitatorId "24" and supplierId "3874" using invalid file "invalidfile"
#    Then the response status should be like a 429
##
#  Scenario: Upload with invalid facilitator ID
#    When the user posts to "API.forCSV.Validation" with facilitatorId "999999" and supplierId "3874" using valid file "validfilefortest"
#    Then the response status should be like a 405
#
#  Scenario: Upload with invalid supplier ID
#    When the user posts to "API.forCSV.Validation" with facilitatorId "24" and supplierId "999999" using valid file "validfilefortest"
#    Then the response status should be like a 405
