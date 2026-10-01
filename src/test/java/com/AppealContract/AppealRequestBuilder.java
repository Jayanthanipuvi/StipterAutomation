package com.AppealContract;

import java.util.Map;

public class AppealRequestBuilder {



        public static String buildApi1Request(Map<String, String> r) {

            return "{"
                    + "\"age\": " + r.get("age") + ","
                    + "\"id\": " + r.get("id") + ","
                    + "\"baPersonId\": \"" + r.get("baPersonId") + "\","
                    + "\"liveAtStreet\": \"" + r.get("liveAtStreet") + "\","
                    + "\"liveAtNumber\": \"" + r.get("liveAtNumber") + "\","
                    + "\"liveAtZipCode\": \"" + r.get("liveAtZipCode") + "\","
                    + "\"liveAtCity\": \"" + r.get("liveAtCity") + "\","
                    + "\"lastName\": \"" + r.get("lastName") + "\","
                    + "\"gender\": \"" + r.get("gender") + "\","
                    + "\"telephone\": \"" + r.get("telephone") + "\","
                    + "\"email\": \"" + r.get("email") + "\","
                    + "\"facilitatorId\": 24,"
                    + "\"signalDate\": \"" + r.get("signalDate") + "\","
                    + "\"dialogueSignaledById\": 46,"
                    + "\"initials\": \"" + r.get("initials") + "\","
                    + "\"birthDate\": \"" + r.get("birthDate") + "\","
                    + "\"facilityTypeId\": 2,"
                    + "\"dialogueUnknownBsnId\": 7"
                    + "}";
        }


    public static String buildApi2Request(Map<String, String> r) {

        String phone = r.get("telephone").trim();

        return "{"
                + "\"address\": {"
                + "\"id\": 0,"
                + "\"street\": " + getValueOrNull(r.get("street")) + ","
                + "\"neighbourhood\": null,"
                + "\"number\": " + getValueOrNull(r.get("number")) + ","
                + "\"numberLetter\": " + getValueOrEmpty(r.get("numberLetter")) + ","
                + "\"numberExtra\": " + getValueOrEmpty(r.get("numberExtra")) + ","
                + "\"numberInfo\": " + getValueOrEmpty(r.get("numberInfo")) + ","
                + "\"zipCode\": " + getValueOrNull(r.get("zipCode")) + ","
                + "\"city\": " + getValueOrNull(r.get("city")) + ","
                + "\"cityId\": 0,"
                + "\"land\": \"Nederland\""
                + "},"
                + "\"titleId\": null,"
                + "\"authorityTypeId\": 1,"
                + "\"relationTypeId\": 16,"
                + "\"telephone\": " + getValueOrNull(formatPhone(r.get("telephone"))) + ","
                + "\"email\": " + getValueOrNull(r.get("email")) + ","
                + "\"mobile\": null,"
                + "\"authorityRelation\": null,"
                + "\"contactProhibition\": null,"
                + "\"familyMember\": null,"
                + "\"householdMember\": null,"
                + "\"firstName\": null,"
                + "\"lastName\": " + getValueOrNull(r.get("contactLastName")) + ","
                + "\"initials\": null,"
                + "\"infix\": null,"
                + "\"id\": 0,"
                + "\"gender\": " + getValueOrNull(r.get("gender")) + ","
                + "\"bsn\": null,"
                + "\"birthDate\": null,"
                + "\"contactType\": \"1\","
                + "\"organisation\": null"
                + "}";
    }

    private static String getValueOrNull(String value) {
        return (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("NULL"))
                ? "null"
                : "\"" + value + "\"";
    }

    private static String getValueOrEmpty(String value) {
        return (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("NULL"))
                ? "\"\""
                : "\"" + value + "\"";
    }

    private static String formatPhone(String value) {

        if (value == null || value.trim().isEmpty()) return null;

        value = value.trim();

        // If already correct format → return
        if (value.matches("\\d{3}-\\d{7}")) {
            return value;
        }

        // If plain number → convert
        if (value.matches("\\d{10}")) {
            return value.substring(0, 3) + "-" + value.substring(3);
        }

        return value; // fallback
    }


    public static String buildApi3Request(Map<String, String> r) {

        return "{"
                + "\"id\": " + r.get("id") + ","
                + "\"deliveryUnitId\": " + r.get("deliveryUnitId") + ","
                + "\"handicapCardId\": " + getValueOrNull(r.get("handicapCardId")) + ","
                + "\"payFrequencyId\": " + r.get("payFrequencyId") + ","
                + "\"payMethodId\": " + r.get("payMethodId") + ","
                + "\"serviceId\": " + r.get("serviceId") + ","
                + "\"serviceGroupId\": " + r.get("serviceGroupId") + ","
                + "\"preferenceSupplierId\": " + r.get("preferenceSupplierId") + ","
                + "\"counterPreferenceSupplierId\": " + r.get("counterPreferenceSupplierId") + ","
                + "\"amount\": " + r.get("amount") + ","
                + "\"deliveryAddressId\": " + r.get("deliveryAddressId") + ","
                + "\"deliveryStreet\": " + getValueOrNull(r.get("deliveryStreet")) + ","
                + "\"deliveryNumber\": " + getValueOrNull(r.get("deliveryNumber")) + ","
                + "\"deliveryZipCode\": " + getValueOrNull(r.get("deliveryZipCode")) + ","
                + "\"deliveryCity\": " + getValueOrNull(r.get("deliveryCity")) + ","
                + "\"researchId\": \"" + r.get("researchId") + "\","
                + "\"productId\": " + r.get("productId") + ","
                + "\"internRemark\": " + getValueOrNull(r.get("internRemark")) + ","
                + "\"startDate\": \"" + r.get("startDate") + "\","
                + "\"endDate\": \"" + r.get("endDate") + "\","
                + "\"urgent\": " + r.get("urgent") + ","
                + "\"activityId\": " + getValueOrNull(r.get("activityId")) + ","
                + "\"contractCareFrequencyId\": " + r.get("contractCareFrequencyId")
                + "}";
    }





    }

