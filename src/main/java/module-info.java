module com.example.chatgui {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.kordamp.bootstrapfx.core;

    opens com.example.chatgui to javafx.fxml;
    exports com.example.chatgui;
}