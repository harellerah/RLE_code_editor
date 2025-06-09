package com.example.javafx_firstproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.net.URI;
import java.util.Optional;

public class LoginDialog {

    public static Optional<String> showAndWait() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Login");

        // Set OK/Cancel buttons
        ButtonType loginButtonType = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, ButtonType.CANCEL);

        // Create username and password fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Password:"), 0, 1);
        grid.add(passwordField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Enable/Disable login button depending on input
        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
        loginButton.setDisable(true);

        usernameField.textProperty().addListener((obs, oldVal, newVal) ->
                loginButton.setDisable(newVal.trim().isEmpty() || passwordField.getText().trim().isEmpty()));
        passwordField.textProperty().addListener((obs, oldVal, newVal) ->
                loginButton.setDisable(newVal.trim().isEmpty() || usernameField.getText().trim().isEmpty()));

        // Convert result to token if OK pressed
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return login(usernameField.getText(), passwordField.getText());
            }
            return null;
        });

        return dialog.showAndWait();
    }

    private static String login(String username, String password) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = String.format("{\"username\":\"%s\", \"password\":\"%s\"}", username, password);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/auth/login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.body());
                String jwt = root.get("token").asText();
                TokenStorage.setToken(jwt);   // ✅ store for later use
                System.out.println("Logged in!");
                return response.body(); // Return JWT token
            } else {
                throw new RuntimeException("Login failed: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

