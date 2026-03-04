package com.qa.util;

public class OSUtils {

    public static String getOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return "Windows";
        } else if (os.contains("mac")) {
            return "Mac";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return "Linux";
        } else {
            return "Unknown OS";
        }
    }
}

