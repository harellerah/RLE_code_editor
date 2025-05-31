package com.example.javafx_firstproject;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.input.MouseButton;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class HelloController {
    @FXML
    private MenuBar menuBar; // Reference to the MenuBar in your FXML
    @FXML
    private TabPane tabPane; // Reference to TabPane from FXML
    @FXML
    private ListView<File> fileDescriptor;
    @FXML
    private Label bottomMsg;
    private File selectedDir;
    private Set<File> expandedDirectories;

    public void runCode(ActionEvent event) {
        //take file content
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        String file = selectedDir.getAbsolutePath();
        if (currentTab != null) {
            file += currentTab.getText();
            BorderPane borderPane = (BorderPane) currentTab.getContent();
            TextArea textArea = (TextArea) borderPane.getCenter();
            bottomMsg.setText("running " + file);
        }
        //input into interpreter
        File programFile = new File("");
        String programOutput = "";
        int exitCode = 0;
        try {
            // Specify the Python script path and Python executable
            ProcessBuilder pb = new ProcessBuilder("python3", Paths.get("") +
                    "/script.py", programFile.getAbsolutePath());
            pb.redirectErrorStream(true); // Merge error and output streams

            // Start the process
            Process process = pb.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                programOutput += line;
            }

            // Wait for the process to complete and get exit code
            exitCode = process.waitFor();
            programOutput += "\n" + "Exit Code: " + exitCode;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        //write interpreter output
        if (exitCode != 0) {
            //write error msg in red
        } else {
            //write output
        }
        bottomMsg.setText("finished ");
    }

    public void openDir(ActionEvent event) {
        // Get the Stage
        Stage stage = (Stage) menuBar.getScene().getWindow();

        // Open DirectoryChooser
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select a Directory");
        directoryChooser.setInitialDirectory(new File("C:/Users"));
        selectedDir = directoryChooser.showDialog(stage);
        expandedDirectories = new HashSet<>();

        if (selectedDir != null) {
            // Get the directory path
            String path = selectedDir.getAbsolutePath(); // e.g., C:\OneDrive\מסמכים

            // Extract the directory name using split
            String[] parts = path.split("\\\\");

            //Design list
            // Set up custom cell factory to display file names
            fileDescriptor.setCellFactory(lv -> new ListCell<File>() {
                private final ImageView iconView = new ImageView();
                {
                    // Set icon size (adjust as needed)
                    iconView.setFitWidth(16);
                    iconView.setFitHeight(16);
                }
                @Override
                protected void updateItem(File file, boolean empty) {
                    super.updateItem(file, empty);
                    if (empty || file == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(file.getName());
                        // Set icon based on file type
                        try {
                            if (file.isDirectory()) {
                                iconView.setImage(new Image(getClass().getResourceAsStream("/icons/folder.png")));
                                if (expandedDirectories.contains(file)) {
                                    getStyleClass().add("expanded-directory");
                                } else {
                                    getStyleClass().remove("expanded-directory");
                                }
                            }else {
                                String name = file.getName().toLowerCase();
                                getStyleClass().remove("expanded-directory");
                                if (name.endsWith(".txt")) {
                                    iconView.setImage(new Image(getClass().getResourceAsStream("/icons/text.png")));
                                } else {
                                    iconView.setImage(new Image(getClass().getResourceAsStream("/icons/default.png")));
                                }
                            }
                            setGraphic(iconView);
                            if (expandedDirectories.contains(file.getParentFile())) {
                                getStyleClass().add("expanded-directory");
                            }
                        } catch (Exception e) {
                            setGraphic(null);
                            System.err.println("Failed to load icon for " + file.getName() + ": " + e.getMessage());
                        }
                    }
                }
            });



            // Handle double-clicks
            fileDescriptor.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                    // Define the context menu
                    ContextMenu contextMenu = new ContextMenu();
                    MenuItem openItem = new MenuItem("Open");
                    MenuItem deleteItem = new MenuItem("Delete");
                    MenuItem createItem = new MenuItem("New file");
                    contextMenu.getItems().addAll(openItem, deleteItem, createItem);

                    // ListView hover tracking
                    fileDescriptor.setOnMouseEntered(e -> {
                        contextMenu.hide();
                    });

                    // Use onAction or wrap each MenuItem with a listener
                    for (MenuItem item : contextMenu.getItems()) {
                        item.setOnAction(e -> {
                            // Perform item action
                            System.out.println(((MenuItem) e.getSource()).getText() + " clicked");
                            contextMenu.hide();
                        });
                    }
                    int index = fileDescriptor.getSelectionModel().getSelectedIndex();
                    if (index != -1) {
                        fileDescriptor.getSelectionModel().select(index); // ensure correct item is selected
                        contextMenu.show(fileDescriptor, mouseEvent.getScreenX(), mouseEvent.getScreenY());
                    }
                }
                else if (mouseEvent.getClickCount() == 2) {
                    int selectedIndex = fileDescriptor.getSelectionModel().getSelectedIndex();
                    if (selectedIndex >= 0) {
                        File selectedFile = fileDescriptor.getItems().get(selectedIndex);
                        if (selectedFile.isFile()) {
                            openFile(selectedFile);
                            if (selectedFile.getName().toLowerCase().endsWith(".txt")) {
                                try {
                                    // Load file content into current tab's TextArea
                                    String content = Files.readString(selectedFile.toPath(), StandardCharsets.UTF_8);
                                    Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
                                    if (currentTab != null) {
                                        BorderPane borderPane = (BorderPane) currentTab.getContent();
                                        TextArea textArea = (TextArea) borderPane.getCenter();
                                        textArea.setText(content);
                                        updateLineNumbers(borderPane, content);
                                        bottomMsg.setText("Loaded " + selectedFile.getName());
                                    }
                                } catch (IOException e) {
                                    bottomMsg.setText("Error reading file: " + e.getMessage());
                                }
                            }
                        } else if (selectedFile.isDirectory()) {
                            boolean isExpanded = false;
                            if (selectedIndex + 1 < fileDescriptor.getItems().size()) {
                                File nextItem = fileDescriptor.getItems().get(selectedIndex + 1);
                                if (nextItem.getParentFile().equals(selectedFile)) {
                                    isExpanded = true;
                                }
                            }

                            if (isExpanded) {
                                // Collapse
                                int index = selectedIndex + 1;
                                while (index < fileDescriptor.getItems().size() &&
                                        fileDescriptor.getItems().get(index).getParentFile().equals(selectedFile)) {
                                    fileDescriptor.getItems().remove(index);
                                }
                                expandedDirectories.remove(selectedFile);
                            } else {
                                // Expand
                                File[] files = selectedFile.listFiles();
                                if (files != null && files.length > 0) {
                                    int insertIndex = selectedIndex + 1;
                                    for (File file : files) {
                                        fileDescriptor.getItems().add(insertIndex++, file);
                                    }
                                    expandedDirectories.add(selectedFile);
                                } else {
                                    bottomMsg.setText("No files found in " + selectedFile.getName());
                                }
                            }
                            fileDescriptor.refresh();
                        }
                    }
                }
            });

            // Populate ListView with file names
            File[] files = selectedDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileDescriptor.getItems().add(file);
                }
            } else {
                bottomMsg.setText("No files found or directory inaccessible.");
            }
        }
    }

    public void openFile(File selectedFile){
        // Manipulate the selected file
        if (selectedFile != null) {
            try {
                System.out.println("Selected file: " + selectedFile.getAbsolutePath());

                if (WorkingFilesManager.openFilesList.contains(selectedFile.getAbsolutePath())){
                    System.out.println("file already open");
                    return;
                }
                WorkingFilesManager.currentOpenFileIndex++;
                if (WorkingFilesManager.currentOpenFileIndex == WorkingFilesManager.openFilesList.size()
                        || WorkingFilesManager.currentOpenFileIndex == 0) {
                    WorkingFilesManager.openFilesList.add(selectedFile.getAbsolutePath());
                }
                else {
                    WorkingFilesManager.openFilesList.add(WorkingFilesManager.currentOpenFileIndex,
                            selectedFile.getAbsolutePath());
                }


                if (newTabAtIndex(WorkingFilesManager.currentOpenFileIndex)) {
                    System.out.println("file opened successfully");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("No file selected.");
        }
    }

    private boolean newTabAtIndex(int index){
        // Create a new Tab
        Tab newTab =
                new Tab(WorkingFilesManager.openFilesList.get(index).split(
                        "\\\\")
                        [WorkingFilesManager.openFilesList.get(index).split(
                                "\\\\").length - 1]);

        // Create TextArea with line numbers
        TextArea textArea = null;
        try {
            textArea = new TextArea(Files.readString(Path.of(WorkingFilesManager.openFilesList.get(index))));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BorderPane borderPane = new BorderPane();
        borderPane.setCenter(textArea);

        ListView<Integer> lineNumbers = new ListView<>();
        lineNumbers.setPrefWidth(30);
        lineNumbers.getStyleClass().add("line-numbers");
        ObservableList<Integer> numbers = FXCollections.observableArrayList();
        numbers.add(1);
        lineNumbers.setItems(numbers);
        borderPane.setLeft(lineNumbers);

        textArea.textProperty().addListener((obs, oldText, newText) -> {
            int lineCount = newText.isEmpty() ? 1 : newText.split("\n", -1).length;
            numbers.clear();
            for (int i = 1; i <= lineCount; i++) {
                numbers.add(i);
            }
        });

        // Sync scrolling
        TextArea finalTextArea = textArea;
        textArea.scrollTopProperty().addListener((obs, oldVal, newVal) -> {
            double scrollFraction = newVal.doubleValue() / finalTextArea.getHeight();
            int lineCount = finalTextArea.getText().isEmpty() ? 1 : finalTextArea.getText().split("\n", -1).length;
            int targetIndex = (int) (scrollFraction * lineCount);
            if (targetIndex < lineNumbers.getItems().size()) {
                lineNumbers.scrollTo(Math.max(0, targetIndex - 1));
            }
        });

        // Update line numbers on text change
        textArea.textProperty().addListener((obs, oldText, newText) -> {
            updateLineNumbers(borderPane, newText);
        });

        // Create a new Tab
        newTab.setContent(borderPane);
        newTab.setClosable(true);

        // Handle tab close request
        newTab.setOnCloseRequest(event -> {
            String content = getTabText(newTab);
            if (isChanged(newTab)) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Save tab content before closing?");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            File saveFile = new File(selectedDir, newTab.getText());
                            writeToFile(saveFile, content, false);
                            bottomMsg.setText("Saved tab content to " + saveFile.getName());
                            if (!fileDescriptor.getItems().contains(saveFile)) {
                                fileDescriptor.getItems().add(saveFile);
                            }
                        } catch (IOException e) {
                            bottomMsg.setText("Error saving tab content: " + e.getMessage());
                            event.consume();
                        }
                    }
                });
            }
            WorkingFilesManager.openFilesList.remove(selectedDir + "\\" + newTab.getText());
            WorkingFilesManager.currentOpenFileIndex--;
            bottomMsg.setText(newTab.getText() + " closed");
        });

        // Add the tab at the specific index
        tabPane.getTabs().add(index, newTab);

        // Optional: Select the new tab
        tabPane.getSelectionModel().select(newTab);

        bottomMsg.setText(newTab.getText() + " opened");

        return true;
    }

    private boolean isChanged(Tab newTab) {
        String tabContent = getTabText(newTab);
        String fileContent = "";
        try {
            fileContent =
                    Files.readString(Path.of(WorkingFilesManager.openFilesList.get(WorkingFilesManager.currentOpenFileIndex)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String result = fileContent.replace("\r", "");
        return !result.equals(tabContent);
    }

    private String getTabText(Tab tab) {
        if (tab != null && tab.getContent() instanceof BorderPane) {
            BorderPane bp = (BorderPane) tab.getContent();
            TextArea ta = (TextArea) bp.getCenter(); // or wherever you put it
            return ta.getText();
        }
        return "";
    }

    public void saveAs(ActionEvent event) {
        // Get the Stage
        Stage stage = (Stage) menuBar.getScene().getWindow();

        // Open FileChooser to save a file
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save File");
        fileChooser.setInitialDirectory(selectedDir != null ? selectedDir : new File("C:/Users"));
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            // Get text from current tab
            String textToWrite = getCurrentTabText();
            try {
                // Write (overwrite) to the file
                writeToFile(file, textToWrite, false);
                bottomMsg.setText("Saved to " + file.getName());
                // Update ListView if the file is in the current directory
                if (selectedDir != null && file.getParentFile().equals(selectedDir)) {
                    fileDescriptor.getItems().add(file);
                }
            } catch (IOException e) {
                bottomMsg.setText("Error saving file: " + e.getMessage());
            }
        }
    }

    private void updateLineNumbers(BorderPane borderPane, String text) {
        ListView<Integer> lineNumbers = (ListView<Integer>) borderPane.getLeft();
        ObservableList<Integer> numbers = lineNumbers.getItems();
        int lineCount = text.isEmpty() ? 1 : text.split("\n", -1).length;
        numbers.clear();
        for (int i = 1; i <= lineCount; i++) {
            numbers.add(i);
        }
    }

    private String getCurrentTabText() {
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        return getTabText(currentTab);
    }

    private void writeToFile(File file, String content, boolean append) throws IOException {
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8),
                append ? new java.nio.file.StandardOpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND}
                        : new java.nio.file.StandardOpenOption[]{java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING});
    }

}