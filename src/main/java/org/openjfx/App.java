package org.openjfx;

import java.awt.Desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javafx.scene.layout.BorderPane;
import org.kordamp.bootstrapfx.scene.layout.Panel;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.openjfx.core.MsIsConstant.*;

import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Stage stage;
    private static Scene scene;

    private static ScreenController screenController;

    public static Desktop desktop = Desktop.getDesktop();

    @Override
    public void start(Stage stage) throws IOException {

        Parent imageListScene = loadFXML(ComponentEnum.ImageList);
        scene = new Scene(imageListScene, 800, 600);

        ScreenController screenController = new ScreenController(scene);
        screenController.addScreen(ComponentEnum.ImageList.toString(), imageListScene);
        screenController.addScreen(ComponentEnum.ImageDetail.toString(), loadFXML(ComponentEnum.ImageDetail));
//        screenController.activate(ComponentEnum.ImageList.toString());

        this.screenController = screenController;

        stage.setScene(scene);
        scene.getStylesheets().add("org/kordamp/bootstrapfx/bootstrapfx.css");
        stage.sizeToScene();
        stage.show();
        this.stage = stage;

    }

//    private void initContainer() {
//        Panel panel = new Panel("This is the title");
//        panel.getStyleClass().add("panel-primary");                            //(2)
//        BorderPane content = new BorderPane();
//        content.setPadding(new Insets(20));
//                 //(2)
//        content.setCenter(button);
//        panel.setBody(content);
//    }

    public static void setRoot(ComponentEnum componentEnum) {
        try {
            screenController.activate(componentEnum.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void configureFileChooser(
            final FileChooser fileChooser
    ) {
        fileChooser.setTitle("Choose Pictures");

//        FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter("Images","*.jpg", "*.png");
        FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter(
                "Images",
                "*.jpg",
                "*.JPG",
                "*.jpeg",
                "*.JPEG",
                "*.png",
                "*.PNG",
                "*.gif",
                "*.GIF"
        );
        fileChooser.getExtensionFilters().add(fileExtensions);

    }

    static List<File> openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        configureFileChooser(fileChooser);

        return fileChooser.showOpenMultipleDialog(stage);

    }
    static File openDirectoryChooser() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        return directoryChooser.showDialog(stage);

    }

    public static void openFile(File file) {
        try {
            desktop.open(file);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    static void openUrl(String url) {
        try {
            desktop.browse(new URL(url).toURI());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Parent loadFXML(ComponentEnum componentEnum) throws IOException {
        String componentName = componentEnum.toString();
        return FXMLLoader.load(App.class.getResource(componentName + "/" + componentName + ".fxml"));
//        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource(componentName + "/" + componentName + ".fxml"));
//        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }

}