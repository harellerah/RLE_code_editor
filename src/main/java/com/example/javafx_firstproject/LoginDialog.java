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
        dialog.setTitle("התחברות");

        // Set OK/Cancel buttons
        ButtonType loginButtonType = new ButtonType("התחבר",
                ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("ביטול",
                ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, cancelButtonType);

        // Create username and password fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
//        usernameField.setPromptText("שם משתמש");
        PasswordField passwordField = new PasswordField();
//        passwordField.setPromptText("סיסמה");

        grid.add(new Label("שם משתמש:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("סיסמה:"), 0, 1);
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
                TokenStorage.setToken(jwt);
                System.out.println("Logged in!");
                HelloApplication.cUser = new User(username,
                        root.get("role").asText());
                return response.body();
            } else {
                throw new RuntimeException("Login failed: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

