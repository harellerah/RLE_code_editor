package com.example.javafx_firstproject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.Arrays;
public class ImportDialog {

//    public static Optional<String> showAndWait() {
//        Dialog<String> dialog = new Dialog<>();
//        dialog.setTitle("ייבא קובץ");
//
//        // Set OK/Cancel buttons
//        ButtonType loginButtonType = new ButtonType("ייבא",
//                ButtonBar.ButtonData.OK_DONE);
//        ButtonType cancelButtonType = new ButtonType("ביטול",
//                ButtonBar.ButtonData.CANCEL_CLOSE);
//        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, cancelButtonType);
//
//        // Create username and password fields
//        GridPane grid = new GridPane();
//        grid.setHgap(10);
//        grid.setVgap(10);
//
//        TextField usernameField = new TextField();
//        usernameField.setPromptText("לדוגמה: test.rl");
//
//        grid.add(new Label("שם הקובץ לייבוא"), 0, 0);
//        grid.add(usernameField, 1, 0);
//
//
//        dialog.getDialogPane().setContent(grid);
//
//        // Enable/Disable login button depending on input
//        Node loginButton = dialog.getDialogPane().lookupButton(loginButtonType);
//        loginButton.setDisable(true);
//
//        usernameField.textProperty().addListener((obs, oldVal, newVal) ->
//                loginButton.setDisable(newVal.trim().isEmpty()));
//
//        // Convert result to token if OK pressed
//        dialog.setResultConverter(dialogButton -> {
//            if (dialogButton == loginButtonType) {
//                return usernameField.getText();
//            }
//            return null;
//        });
//
//        return dialog.showAndWait();
//    }

    public static Optional<String> showAndWait() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("ייבא קובץ");

        // Set OK/Cancel buttons
        ButtonType importButtonType = new ButtonType("ייבא", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("ביטול", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(importButtonType, cancelButtonType);

        // Create layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Text field
        TextField fileNameField = new TextField();
        fileNameField.setPromptText("לדוגמה: test.rl");

        // File list
        ListView<String> fileListView = new ListView<>();
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest assignments_request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/files/assignments"))
                .header("Authorization", "Bearer " + TokenStorage.getToken())
                .GET()
                .build();

        HttpRequest submissions_request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/files/submissions" +
                        "?username=" + HelloApplication.cUser.getUsername() +
                        "&role=" + HelloApplication.cUser.getRole()))
                .header("Authorization", "Bearer " + TokenStorage.getToken())
                .GET()
                .build();

//        try {
//            HttpResponse<InputStream> assignments_response = client.send(assignments_request, HttpResponse.BodyHandlers.ofInputStream());
//            HttpResponse<InputStream> submissions_response = client.send(submissions_request, HttpResponse.BodyHandlers.ofInputStream());
//            if (assignments_response.statusCode() == 200) {
//
//            } else {
//                //error
//            }
//            if (submissions_response.statusCode() == 200) {
//
//            } else {
//                //error
//            }
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
        try {
            fileListView.getItems().addAll(fetchAssignments("assignments"
                    , ""));
            fileListView.getItems().addAll(fetchSubmissionFilenames(HelloApplication.cUser.getRole()
                    , HelloApplication.cUser.getUsername()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        fileListView.setPrefHeight(150); // set height for scrolling

        // When user clicks an item, populate the text field
        fileListView.setOnMouseClicked(event -> {
            String selected = fileListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                fileNameField.setText(selected.substring(selected.indexOf(" ")+1));
            }
        });

        // Add to grid
        grid.add(new Label("קבצים זמינים"), 0, 0);
        grid.add(fileListView, 1, 0);

        grid.add(new Label("שם הקובץ לייבוא"), 0, 1);
        grid.add(fileNameField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Enable/Disable import button depending on input
        Node importButton = dialog.getDialogPane().lookupButton(importButtonType);
        importButton.setDisable(true);
        fileNameField.textProperty().addListener((obs, oldVal, newVal) ->
                importButton.setDisable(newVal.trim().isEmpty())
        );

        // Return the chosen file name when OK pressed
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == importButtonType) {
                return fileNameField.getText();
            }
            return null;
        });

        return dialog.showAndWait();
    }

    public static ArrayList<String> fetchAssignments(String endpoint,
                                                     String params) throws IOException, InterruptedException {
        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/files/" +
                        endpoint + params))
                .header("Authorization", "Bearer " + TokenStorage.getToken()) // remove
                // if no auth
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse JSON array into ArrayList<String>
        ObjectMapper mapper = new ObjectMapper();
        String[] assignmentsArray = mapper.readValue(response.body(), String[].class);

        ArrayList<String> assignments = new ArrayList<>();
        String prefix = "מטלה: "; // constant beginning

        Arrays.stream(assignmentsArray)
                .map(name -> prefix + name) // prepend constant beginning
                .forEach(assignments::add);

        return assignments;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SubmissionDTO {
        public String filename;
    }

    public static ArrayList<String> fetchSubmissionFilenames(String role, String username)
            throws IOException, InterruptedException {

        HttpClient client = HttpClient.newHttpClient();

        String url = String.format("%s/submissions?role=%s&username=%s",
                "http://localhost:8080/files", role, username);

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET();

        if (TokenStorage.hasToken()) {
            requestBuilder.header("Authorization", "Bearer " + TokenStorage.getToken());
        }

        HttpRequest request = requestBuilder.build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        ObjectMapper mapper = new ObjectMapper();
        SubmissionDTO[] submissionsArray = mapper.readValue(response.body(), SubmissionDTO[].class);

        ArrayList<String> filenames = new ArrayList<>();
        Arrays.stream(submissionsArray)
                .map(s -> "הגשה: " + s.filename)
                .forEach(filenames::add);

        return filenames;
    }
}
