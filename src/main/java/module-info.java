module com.example.javafx_firstproject {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.example.javafx_firstproject to javafx.fxml;
    exports com.example.javafx_firstproject;
}