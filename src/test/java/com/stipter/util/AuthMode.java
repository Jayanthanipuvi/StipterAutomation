package com.stipter.util;

public enum AuthMode {
    KEYCLOAK,
    COOKIE_MODE;

    public static AuthMode from(String value) {
        if (value == null) return COOKIE_MODE;
        try {
            return AuthMode.valueOf(value.trim().toUpperCase());


        } catch (IllegalArgumentException e) {
            return COOKIE_MODE;
        }
    }
}
