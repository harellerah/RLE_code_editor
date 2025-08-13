package com.example.javafx_firstproject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

public class ExportDialog {
    public static Optional<String> showAndWait() {
        Dialog<String> dialog = new Dialog<>();
        // Get the Stage from the Dialog
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();

// Set the icon
        stage.getIcons().add(new Image(ExportDialog.class.getResourceAsStream("/icons/app_icon" +
                ".png"))); // or use a resource path
        dialog.setTitle("ייצא קובץ");

        // Set OK/Cancel buttons
        ButtonType importButtonType = new ButtonType("ייצא",
                ButtonBar.ButtonData.OK_DONE);
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
        grid.add(new Label("מטלות זמינות למענה"), 0, 0);
        grid.add(fileListView, 1, 0);

        grid.add(new Label("שם המטלה"), 0, 1);
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
}
