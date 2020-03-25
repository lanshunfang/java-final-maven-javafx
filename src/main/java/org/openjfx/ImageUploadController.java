package org.openjfx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.openjfx.core.MsIsConstant;

import javax.imageio.ImageIO;

public class ImageUploadController {


    @FXML
    public GridPane gridPane;
    @FXML
    public ImageView imageUploadPreviewContainerEl;
    @FXML
    public Image imageUploadPreviewEl;

    @FXML
    private void switchToDownload() throws IOException {
        App.setRoot(MsIsConstant.ComponentEnum.ImageUpload);
    }

    ImageUploadController() {
    }

    @FXML
    public void onPickImageAction() {
        List<File> files = App.openFileChooser();
        gridPane.getChildren().clear();

        int columnCount = 5;
        int columnIndex = 0;
        int rowIndex = 0;

        for (File file: files) {
            try {
                Image image = SwingFXUtils.toFXImage(ImageIO.read(file), null);

                if (columnIndex > 4) {
                    rowIndex++;
                }
                columnIndex %= 5;

                ImageView imageView = new ImageView();
                imageView.setImage(
                        image
                );

                gridPane.add(imageView, columnIndex, rowIndex);

                columnIndex++;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

//        files.forEach(
//                file -> {
//
//
//                }
//        );

    }

}
