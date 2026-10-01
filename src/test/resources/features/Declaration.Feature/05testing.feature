


   Feature: Invoice Declaration Validation
     @declaration
     Scenario Outline: Validate single row
       Given the user validates row <RowNumber> using "<QueryKey>" from "<CsvKey>"

       Then generate the InvoiceLine report <RowNumber>


       Examples:
         | RowNumber | QueryKey | CsvKey          |
         | 0         | queryA   | Declaration.csv |













































