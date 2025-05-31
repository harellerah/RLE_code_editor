package com.example.javafx_firstproject;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 800, 480);
        stage.setTitle("RLE Code Editor");


        // Set icon (must be in resources or accessible path)
        Image icon =
                new Image(getClass().getResourceAsStream("/icons/app_icon" +
                        ".png")); // Local file
        // Or use: new Image(getClass().getResourceAsStream("/icon.png")) if in resources
        stage.getIcons().add(icon);


        WorkingFilesManager.currentOpenFileIndex = -1;

        WorkingFilesManager.openFilesList = new ArrayList<>();

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}