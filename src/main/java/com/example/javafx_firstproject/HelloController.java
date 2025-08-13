package com.example.javafx_firstproject;

import com.fasterxml.jackson.core.type.TypeReference;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.entity.EntityBuilder;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.fxmisc.richtext.StyleClassedTextArea;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.hc.client5.http.classic.methods.HttpPost;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;


public class HelloController {
    @FXML
    private MenuBar menuBar; // Reference to the MenuBar in your FXML
    @FXML
    private TabPane tabPane; // Reference to TabPane from FXML
    @FXML
    private ListView<File> fileDescriptor;
    @FXML
    private Label bottomMsg;
    @FXML
    private TextFlow outputArea;
    private File selectedDir;
    private Set<File> expandedDirectories;

    public void importFile(ActionEvent event) throws IOException, InterruptedException {
        if (selectedDir == null) {
            bottomMsg.setText("אנא פתח תיקייה כדי לייבא אליה קבצים");
            return;
        }
        if (!TokenStorage.hasToken()) {
            System.out.println("Not authenticated!");
            Optional<String> token = LoginDialog.showAndWait();
            if (token.isEmpty()) return;
        }

        Optional<String> fn = ImportDialog.showAndWait();
        if (fn.isEmpty()) return;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/files/download/" + fn.get() + "?user=" + HelloApplication.cUser.getUsername() + "&role=" + HelloApplication.cUser.getRole()))
                .header("Authorization", "Bearer " + TokenStorage.getToken())
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            // Save file to disk
            try (InputStream inputStream = response.body()) {
                // Extract filename from header
                String disposition = response.headers().firstValue("Content-Disposition").orElse("");
                String filename = "downloaded_file"; // fallback

                if (disposition.contains("filename=")) {
                    filename = disposition.substring(disposition.indexOf("filename=") + 9).replace("\"", "");
                }
                    System.out.println(filename);
                    Files.copy(inputStream,
                            Path.of(selectedDir.getAbsolutePath() + "\\" + filename), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
            System.out.println("File downloaded successfully to " + selectedDir.getAbsolutePath());
            bottomMsg.setText("הקובץ הורד בהצלחה ל:" + selectedDir.getAbsolutePath());
        } else {
            System.out.println("Failed to download file. Status code: " + response.statusCode());
            bottomMsg.setText("שגיאה מספר:" + response.statusCode());
        }
    }

    public void exportFile(ActionEvent event) {
        if (tabPane.getSelectionModel().isEmpty()) {
            bottomMsg.setText("אנא פתח קובץ בכדי לייצא אותו");
            return;
        }
        if (!TokenStorage.hasToken()) {
            System.out.println("Not authenticated!");
            Optional<String> token = LoginDialog.showAndWait();
            if (token.isEmpty()) return;
        }

        // Encode values to ensure they are URL-safe
        String encodedParam1Value = URLEncoder.encode(HelloApplication.cUser.getUsername(),
                StandardCharsets.UTF_8);
        String encodedParam2Value =
                URLEncoder.encode(HelloApplication.cUser.getRole(),
                StandardCharsets.UTF_8);

//        String uriStringWithParams = "http://localhost:8080/files/upload" +
//                "?" + "uploader" + "=" + encodedParam1Value +
//                "&" + "perm" + "=" + tagsString;

        // Prepare file
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null) return;
        File programFile = new File(selectedDir.getAbsolutePath() + "\\" + currentTab.getText());

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("http://localhost:8080/files/upload");
            post.setHeader("Authorization", "Bearer " + TokenStorage.getToken());

            FileBody fileBody = new FileBody(programFile);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.addPart("file", fileBody);
            builder.addTextBody("uploader", encodedParam1Value);
            builder.addTextBody("role", encodedParam2Value);
            builder.addTextBody("type", "submission");
            builder.addTextBody("assignment", ExportDialog.showAndWait().get());

            post.setEntity(builder.build());

            try (CloseableHttpResponse response = client.execute(post)) {
                String responseText = EntityUtils.toString(response.getEntity());
                System.out.println("Status: " + response.getCode());
                System.out.println("Response: " + responseText);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runCode(ActionEvent event) {
        if (tabPane.getSelectionModel().isEmpty()) {
            bottomMsg.setText("אנא פתח קובץ בכדי להריץ אותו");
            return;
        }
        //take file content
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        String file = selectedDir.getAbsolutePath();
        if (currentTab != null) {
            file += "\\"+currentTab.getText();
            BorderPane borderPane = (BorderPane) currentTab.getContent();
            TextArea textArea = (TextArea) borderPane.getCenter();
            bottomMsg.setText("מריץ את: " + file);
        }
        //input into interpreter
        File programFile = new File(file);
        String programOutput = "";
        int exitCode = 0;
        boolean exception = false;
        try {
            // Specify the Python script path and Python executable
            ProcessBuilder pb = new ProcessBuilder("python3", Paths.get("").toAbsolutePath() +
                    "\\CompilerFiles\\shell.py", programFile.getAbsolutePath());
            pb.redirectErrorStream(true); // Merge error and output streams

            // Crucial: Set PYTHONIOENCODING for the subprocess
            pb.environment().put("PYTHONIOENCODING", "utf-8");

            // Start the process
            Process process = pb.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                programOutput += line + "\n";
            }

            // Wait for the process to complete and get exit code
            exitCode = process.waitFor();
            exception = programOutput.endsWith("ERROR\n");
            programOutput = programOutput.substring(0,
                    programOutput.lastIndexOf("\n"));
            programOutput = programOutput.substring(0,
                    programOutput.lastIndexOf("\n"));
            programOutput += "\n\n" + "Exit Code: " + exitCode;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        //write interpreter output
        if (exitCode != 0 || exception) {
            //write error msg in red
            System.out.println(programOutput);
//            outputArea.setTextFormatter();
            Text redText = new Text(programOutput);
            redText.setFill(Color.RED);  // Set text color
            outputArea.getChildren().clear();  // remove all children
            outputArea.getChildren().add(redText);
        } else {
            System.out.println(programOutput);
            Text redText = new Text(programOutput);
            outputArea.getChildren().clear();  // remove all children
            outputArea.getChildren().add(redText);
        }
        bottomMsg.setText("סיום ");
    }

    public void openDir(ActionEvent event) {
        // Get the Stage
        Stage stage = (Stage) menuBar.getScene().getWindow();
        // Open DirectoryChooser

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("בחר תיקייה");
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
                    MenuItem openItem = new MenuItem("פתח");
                    MenuItem deleteItem = new MenuItem("מחק");
                    MenuItem createItem = new MenuItem("חדש");
                    contextMenu.getItems().addAll(openItem, deleteItem, createItem);

                    // ListView hover tracking
                    fileDescriptor.setOnMouseEntered(e -> {
                        contextMenu.hide();
                    });

                    createItem.setOnAction( e -> {
                        int i = 0;
                            try {
                                if (!new File("temp.rl").createNewFile()) {
                                    while (! new File("temp"+i+".rl").createNewFile())
                                        i++;
                                }
                            } catch (IOException ex) {
                              //TODO
                            }
                        openFile(new File("temp"+i+".rl"));
                        contextMenu.hide();
                    });
                    // Use onAction or wrap each MenuItem with a listener
//                    for (MenuItem item : contextMenu.getItems()) {
//                        item.setOnAction(e -> {
//                            // Perform item action
//                            System.out.println(((MenuItem) e.getSource()).getText() + " clicked");
//                            contextMenu.hide();
//                        });
//                    }
                    int index = fileDescriptor.getSelectionModel().getSelectedIndex();
                    if (index != -1) {
                        fileDescriptor.getSelectionModel().select(index); // ensure correct item is selected
                        contextMenu.show(fileDescriptor, mouseEvent.getScreenX(), mouseEvent.getScreenY());
                    }

                    deleteItem.setOnAction( e -> {
                        Path filePath =
                                fileDescriptor.getItems().get(index).toPath();
                        try {
                            if (Files.deleteIfExists(filePath)) {
                                System.out.println("File deleted successfully (or it didn't exist): " + filePath.getFileName());
                            } else {
                                System.out.println("File did not exist, or could not be deleted for another reason: " + filePath.getFileName());
                                // You might still get other IOExceptions here if it exists but can't be deleted.
                            }
                        } catch (IOException exep) {
                            System.err.println("Error deleting file: " + exep.getMessage());
                        }
                        contextMenu.hide();
                    });
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
                                        bottomMsg.setText("נטען " + selectedFile.getName());
                                    }
                                } catch (IOException e) {
                                    bottomMsg.setText("שגיאה בקריאת הקובץ: " + e.getMessage());
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
                                    bottomMsg.setText("לא נמצא קובץ בתוך " + selectedFile.getName());
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
                fileDescriptor.getItems().removeAll(fileDescriptor.getItems());
                for (File file : files) {
                    fileDescriptor.getItems().add(file);
                }
            } else {
                bottomMsg.setText("לא נמצאו קבצים או שהתיקייה אינה נגישה.");
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
                    tabPane.getSelectionModel().select(WorkingFilesManager.openFilesList.indexOf(selectedFile.getAbsolutePath()));
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
//            highlightWords(textArea,
//                    text,
//                    loadKeywordsFromJson(Paths.get("").toAbsolutePath() +
//                            "\\CompilerFiles\\keywords.json"));
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
        // Run after the scene is ready (e.g., in initialize or after layout pass)
//        textArea.sceneProperty().addListener((obs, oldScene, newScene) -> {
//            if (newScene != null) {
//                Platform.runLater(() -> {
//                    ScrollBar vScroll = findVerticalScrollBar(textArea);
//                    if (vScroll != null) {
//                        vScroll.valueProperty().addListener((scrollObs, oldVal, newVal) -> {
//                            double scrollFraction = newVal.doubleValue();
//                            String text = textArea.getText();
//                            int lineCount = text.isEmpty() ? 1 : (int) text.chars().filter(ch -> ch == '\n').count() + 1;
//                            int targetIndex = (int) (scrollFraction * lineCount);
//                            lineNumbers.scrollTo(Math.max(0, targetIndex - 1));
//                        });
//                    }
//                });
//            }
//        });



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
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "תרצה " +
                        "לשמור את תוכן הקובץ לפני סגירה?");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        try {
                            File saveFile = new File(selectedDir, newTab.getText());
                            writeToFile(saveFile, content, false);
                            bottomMsg.setText("התוכן נשמר לתוך: " + saveFile.getName());
                            if (!fileDescriptor.getItems().contains(saveFile)) {
                                fileDescriptor.getItems().add(saveFile);
                            }
                        } catch (IOException e) {
                            bottomMsg.setText("שגיאה בעת שמירת התוכן: " + e.getMessage());
                            event.consume();
                        }
                    }
                });
            }
            WorkingFilesManager.openFilesList.remove(selectedDir + "\\" + newTab.getText());
            WorkingFilesManager.currentOpenFileIndex--;
            bottomMsg.setText(newTab.getText() + " נסגר");
        });

        // Add the tab at the specific index
        tabPane.getTabs().add(index, newTab);

        // Optional: Select the new tab
        tabPane.getSelectionModel().select(newTab);

        bottomMsg.setText(newTab.getText() + " נפתח");

        return true;
    }

    public void highlightWords(StyleClassedTextArea area, String inputText, Set<String> keywords) {
        area.clear();
        area.setNodeOrientation(NodeOrientation.RIGHT_TO_LEFT);
        area.getStyleClass().add("right-align");
        String[] tokens = inputText.split("(?<=\\s)|(?=\\s)"); // keep spaces as separate tokens
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (!trimmedToken.isEmpty() && keywords.contains(trimmedToken.toUpperCase())) {
                area.append(token, "highlight"); // apply CSS style class
            } else {
                area.append(token, "normal"); // fallback style
            }
        }
    }


    private ScrollBar findVerticalScrollBar(StyleClassedTextArea area) {
        for (Node node : area.lookupAll(".scroll-bar")) {
            if (node instanceof ScrollBar scrollBar &&
                    scrollBar.getOrientation() == Orientation.VERTICAL) {
                return scrollBar;
            }
        }
        return null;
    }

    private boolean isChanged(Tab newTab) {
        String tabContent = getTabText(newTab);
        String fileContent = "";
        try {
            fileContent =
                    Files.readString(Path.of(WorkingFilesManager.openFilesList.get(tabPane.getTabs().indexOf(newTab))));
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

    public void save(ActionEvent event) {
        if (tabPane.getSelectionModel().isEmpty()) {
            bottomMsg.setText("אנא פתח קובץ בכדי לשמור אותו");
            return;
        }
        // Get the Stage
        Stage stage = (Stage) menuBar.getScene().getWindow();

        // Prepare file
        Tab currentTab = tabPane.getSelectionModel().getSelectedItem();
        if (currentTab == null) return;
        File file = new File(selectedDir.getAbsolutePath() + "\\" + currentTab.getText());

        // Get text from current tab
        String textToWrite = getCurrentTabText();
        try {
            // Write (overwrite) to the file
            writeToFile(file, textToWrite, false);
            bottomMsg.setText("נשמר");
            // Update ListView if the file is in the current directory
            if (selectedDir != null && file.getParentFile().equals(selectedDir)) {
                fileDescriptor.getItems().add(file);
            }
        } catch (IOException e) {
            bottomMsg.setText("שגיאה בעת שמירת הקובץ " + e.getMessage());
        }

    }


    public void saveAs(ActionEvent event) {
        if (tabPane.getSelectionModel().isEmpty()) {
            bottomMsg.setText("אנא פתח קובץ בכדי לשמור אותו");
            return;
        }
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
                bottomMsg.setText("נשמר לתוך " + file.getName());
                // Update ListView if the file is in the current directory
                if (selectedDir != null && file.getParentFile().equals(selectedDir)) {
                    fileDescriptor.getItems().add(file);
                }
            } catch (IOException e) {
                bottomMsg.setText("שגיאה בעת שמירת הקובץ " + e.getMessage());
            }
        }
    }

    public Set<String> loadKeywordsFromJson(String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> map = mapper.readValue(new File(filePath),
                    new TypeReference<Map<String, String>>() {});
            return new HashSet<>(map.values()); // return keys as the keywords set
        } catch (Exception e) {
            e.printStackTrace();
            return Set.of();
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