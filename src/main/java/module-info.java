module javafinalmsis2020 {
    requires javafx.controls;
    requires transitive javafx.graphics;
    requires javafx.fxml;
    requires transitive java.desktop;

    requires java.datatransfer;
    requires java.logging;
    requires javafx.swing;
    requires org.kordamp.bootstrapfx.core;

    requires jdk.jfr;
    requires metadata.extractor;
    requires javafx.web;
    requires jdk.jsobject;

    opens org.openjfx to javafx.fxml;
    exports org.openjfx;
    
}