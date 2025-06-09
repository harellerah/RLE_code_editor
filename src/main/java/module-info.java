module com.example.javafx_firstproject {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;
    requires org.fxmisc.richtext;
    requires java.net.http;
    requires org.apache.httpcomponents.client5.httpclient5;
    requires org.apache.httpcomponents.core5.httpcore5;


    opens com.example.javafx_firstproject to javafx.fxml;
    exports com.example.javafx_firstproject;
}