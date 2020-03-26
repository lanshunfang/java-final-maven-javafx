package org.openjfx;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.embed.swing.SwingFXUtils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.openjfx.core.MsIsConstant.*;

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
        App.setRoot(ComponentEnum.ImageUpload);
    }

    private ArrayList<File> imageFileList = new ArrayList<>();

    public ImageUploadController() {
    }

    private void showImages() {

        gridPane.getChildren().clear();

        if (imageFileList.size() == 0) {
            setDefaultImagePlaceholder();
            return;
        }

        int columnCount = 3;
        int columnIndex = 0;
        int rowIndex = 0;

        int imageWidth = 100;
        int imageHeight = 100;

        for (int fileIndex = 0; fileIndex < imageFileList.size(); fileIndex++) {

            File file = imageFileList.get(fileIndex);

            try {
                Image image = SwingFXUtils.toFXImage(ImageIO.read(file), null);

                if (columnIndex >= columnCount) {
                    rowIndex++;
                }
                columnIndex %= columnCount;

                StackPane stackPane = new StackPane();
                stackPane.getStyleClass().add("grid-cell");

                ImageView imageView = new ImageView();

                if (columnIndex == 0) {
                    imageView.getStyleClass().add("first-column");
                }
                if (rowIndex == 0) {
                    imageView.getStyleClass().add("first-row");
                }

                imageView.setImage(
                        image
                );

                configImageView(imageView, imageHeight, imageWidth);

                Button removeBtn = new Button("Remove");

                ImageView btnImageView = new ImageView(
                        new Image(
                                PathEnum.ClosePng.toString()

                        )
                );

                configImageView(btnImageView, 16, 16);

                btnImageView.getStyleClass().add("delete-image-btn");

                removeBtn.setGraphic(

                        btnImageView
                );

                {
                    removeBtn.setOnAction(deleteEvent -> {
                        imageFileList.remove(file);
                        this.showImages();
                    });
                }

                stackPane.getChildren().addAll(imageView);
                stackPane.getChildren().addAll(removeBtn);

                gridPane.add(stackPane, columnIndex, rowIndex);
//                gridPane.add(imageView, columnIndex, rowIndex);
//                gridPane.add(removeBtn, columnIndex, rowIndex);

                columnIndex++;

            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    private void setDefaultImagePlaceholder() {

        gridPane.add(
                configImageView(new ImageView(
                                new Image(PathEnum.ImagePlaceholder.toString())
                        ),
                        100,
                        100
                ),
                0,
                0
        );

    }

    private ImageView configImageView(ImageView imageView, int imageHeight, int imageWidth) {
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(imageHeight);
        imageView.setFitWidth(imageWidth);
        imageView.setSmooth(true);
        return imageView;
    }

    @FXML
    public void onPickImageAction(Event event) {

        List<File> files = App.openFileChooser();

        if (files == null) {
            return;
        }

        this.imageFileList.addAll(files);

        this.showImages();

    }

}
