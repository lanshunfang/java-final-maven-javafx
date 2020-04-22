package org.openjfx;

import java.awt.Desktop;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.openjfx.core.MsIsConstant.*;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Stage stage;
    private static Scene scene;

    private static ScreenController screenController;

    public static Desktop desktop = Desktop.getDesktop();

    @Override
    public void start(Stage stg) throws IOException {

        Parent imageListScene = loadFXML(ComponentEnum.ImageList);
        scene = new Scene(imageListScene, 800, 600);

        // init all screens
        ScreenController sc = new ScreenController(scene, stg);
        sc.addScreen(ComponentEnum.ImageList.toString(), imageListScene);
        sc.addScreen(ComponentEnum.ImageDetail.toString(), loadFXML(ComponentEnum.ImageDetail));
        sc.addScreen(ComponentEnum.ImageGeoMap.toString(), loadFXML(ComponentEnum.ImageGeoMap));

        screenController = sc;

        stg.setScene(scene);

        // load bootstrap css
        scene.getStylesheets().add("org/kordamp/bootstrapfx/bootstrapfx.css");
        stg.sizeToScene();
        stg.show();
        stage = stg;
    }


    public static void setRoot(ComponentEnum componentEnum) {
        try {
            screenController.activate(componentEnum.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Only allow pick jpg/png/gif image files
     *
     * @param fileChooser
     */
    private static void configureFileChooser(
            final FileChooser fileChooser
    ) {
        fileChooser.setTitle("Choose Pictures");

        String formats[] = Arrays.stream(ImageConvertingFormatEnum.values()).map(
                (item) -> item.formatValues
        )
                .collect(
                        Collector.of(
                                () -> new ArrayList<String>(),
                                (prev, currFormatValues) -> {

                                    Stream.of(currFormatValues).forEach(
                                            formatValue -> {
                                                prev.add("*." + formatValue.toUpperCase());
                                                prev.add("*." + formatValue.toLowerCase());
                                            }
                                    );

                                },
                                BinaryOperator.maxBy((a, b) -> 1),
                                (result) -> result.toArray(new String[0])
                        )
                );

        FileChooser.ExtensionFilter fileExtensions = new FileChooser.ExtensionFilter(
                "Images",
                formats
        );
        fileChooser.getExtensionFilters().add(fileExtensions);

    }

    /**
     * Open the file picker
     *
     * @return
     */
    static List<File> openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        configureFileChooser(fileChooser);

        return fileChooser.showOpenMultipleDialog(stage);

    }

    /**
     * Open folder picker
     *
     * @return
     */
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