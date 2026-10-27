package com.BRP;

import java.util.ArrayList;
import java.util.List;

public class NpsLk01ValidationResult {

    private final String testCase;
    private final String bsn;
    private final List<FieldValidation> validations = new ArrayList<>();
    private int apiStatus = -1;
    private Long databaseRecordId;

    public NpsLk01ValidationResult(String testCase, String bsn) {
        this.testCase = testCase;
        this.bsn = bsn;
    }

    public void setApiStatus(int apiStatus) {
        this.apiStatus = apiStatus;
    }

    public void setDatabaseRecordId(Long databaseRecordId) {
        this.databaseRecordId = databaseRecordId;
    }

    public void addValidation(String field, String expected, String actual,
                              boolean passed, String failureDetails) {
        validations.add(new FieldValidation(field, expected, actual, passed, failureDetails));
    }

    public void addFailure(String field, String failureDetails) {
        addValidation(field, "", "", false, failureDetails);
    }

    public String testCase() {
        return testCase;
    }

    public String bsn() {
        return bsn;
    }

    public int apiStatus() {
        return apiStatus;
    }

    public Long databaseRecordId() {
        return databaseRecordId;
    }

    public List<FieldValidation> validations() {
        return List.copyOf(validations);
    }

    public boolean passed() {
        return !validations.isEmpty() && validations.stream().allMatch(FieldValidation::passed);
    }

    public String overallStatus() {
        return passed() ? "PASS" : "FAIL";
    }

    public record FieldValidation(
            String field,
            String expected,
            String actual,
            boolean passed,
            String failureDetails
    ) {
    }
}
