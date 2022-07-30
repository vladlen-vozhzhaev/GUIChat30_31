module com.example.chatgui {
    requires javafx.controls;
    requires javafx.fxml;
    requires json.simple;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.chatgui to javafx.fxml;
    exports com.example.chatgui;
}