module hellofx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.datatransfer;
    requires java.desktop;
    requires java.logging;
    requires javafx.swing;
    requires org.kordamp.bootstrapfx.core;
    requires jdk.jfr;

    opens org.openjfx to javafx.fxml;
    exports org.openjfx;
}