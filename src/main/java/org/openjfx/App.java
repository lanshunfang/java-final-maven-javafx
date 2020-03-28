package org.openjfx;

import java.awt.Desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;
import java.io.IOException;
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

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML(ComponentEnum.ImageList), 800, 600);


        ScreenController screenController = new ScreenController(scene);
        screenController.addScreen(ComponentEnum.ImageList.toString(), loadFXML(ComponentEnum.ImageList));
        screenController.addScreen(ComponentEnum.ImageDetail.toString(), loadFXML(ComponentEnum.ImageDetail));
        screenController.activate(ComponentEnum.ImageList.toString());

        this.screenController = screenController;

        stage.setScene(scene);
        stage.show();
        this.stage = stage;

    }

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

        FileChooser.ExtensionFilter fileExtensions =new FileChooser.ExtensionFilter("Images","*.jpg", "*.png");
        fileChooser.getExtensionFilters().add(fileExtensions);

    }

    static List<File> openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        configureFileChooser(fileChooser);

        return fileChooser.showOpenMultipleDialog(stage);

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