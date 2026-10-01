package com.AppealContract;

import com.qa.util.OSUtils;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;


public class OsdetecterforAppeal {

    @Given("the system OS is detected")
    public void the_system_os_is_detected() {
        String os = OSUtils.getOS();
        System.out.println("Detected OS: " + os);

    }

    @Then("verify the OS is correctly identified")
    public void verify_the_os_is_correctly_identified() {
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
