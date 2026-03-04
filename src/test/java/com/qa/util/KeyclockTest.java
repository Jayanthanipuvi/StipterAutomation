package com.qa.util;

import com.jcraft.jsch.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class KeyclockTest {

    public static void startKeycloak() {
        String host = "node23280-test-smop.ams1.jls.docktera.net";
        String user = "root";
        String privateKey = "C:/Keys/id_rsa"; // path to private key
        String passphrase = "Ipuvi@12345"; // or null if not needed
        int sshPort = 22;
        String keycloakUrl = "http://" + host + ":8080/auth"; // Keycloak endpoint check

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(privateKey, passphrase);

            System.out.println("🔐 Connecting to server via SSH...");
            Session session = jsch.getSession(user, host, sshPort);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000); // 10-second timeout

            System.out.println("✅ Connected to Keycloak host: " + host);

            // Check if Keycloak is already running
            ChannelExec checkChannel = (ChannelExec) session.openChannel("exec");
            checkChannel.setCommand("pgrep -f keycloak || echo 'not running'");
            checkChannel.setInputStream(null);
            InputStream checkIn = checkChannel.getInputStream();
            checkChannel.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(checkIn));
            String output = reader.readLine();
            checkChannel.disconnect();

            if (output == null || output.contains("not running")) {
                System.out.println("🟡 Keycloak not running. Starting it now...");
                ChannelExec startChannel = (ChannelExec) session.openChannel("exec");
                startChannel.setCommand("cd /opt/jboss/keycloak/bin && ./standalone.sh -b 0.0.0.0 &");
                startChannel.setInputStream(null);
                startChannel.setErrStream(System.err);
                startChannel.connect();
                System.out.println("🚀 Keycloak startup command executed.");
                Thread.sleep(15000); // Wait for Keycloak to initialize
                startChannel.disconnect();
            } else {
                System.out.println("🟢 Keycloak is already running.");
            }

            session.disconnect();
            System.out.println("🔒 SSH session closed.");

            // Optional: Verify Keycloak is accessible before proceeding
            if (isKeycloakUp(keycloakUrl)) {
                System.out.println("✅ Keycloak is up and accessible at: " + keycloakUrl);
            } else {
                System.err.println("❌ Keycloak did not respond at: " + keycloakUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to connect/start Keycloak!");
        }
    }

    // Simple HTTP check to confirm Keycloak is up
    private static boolean isKeycloakUp(String urlString) {
        try {
            System.out.println("🔍 Checking Keycloak health at: " + urlString);
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            return responseCode == 200 || responseCode == 302;
        } catch (Exception e) {
            return false;
        }


    }

    public static void main(String[] args) {
        startKeycloak();
    }
}
