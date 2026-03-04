package com.qa.util;


import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;

public class Osidentify {



    @Given("the system OS is detected")
    public void detectSystemOS() {
        String os = OSUtils.getOS();
        System.out.println("Detected OS: " + os);
    }

    @Then("verify the OS is correctly identified")
    public void verifyTheOSIsCorrectlyIdentified() {
        String detectedOS = OSUtils.getOS(); // Call the utility method to get the OS
        System.out.println("Detected OS: " + detectedOS); // Print the detected OS

        // Example: Validate the detected OS against known OS types
        if (detectedOS.equals("Windows") || detectedOS.equals("Mac") || detectedOS.equals("Linux")) {
            System.out.println("OS is correctly identified as: " + detectedOS);
        } else {


            throw new AssertionError("OS identification failed. Detected OS: " + detectedOS);
        }
    }


}
