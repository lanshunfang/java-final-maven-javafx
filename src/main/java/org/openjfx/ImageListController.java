package org.openjfx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.embed.swing.SwingFXUtils;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.openjfx.core.MessageObject.*;
import org.openjfx.core.Messaging;
import org.openjfx.core.Channel;
import org.openjfx.core.MsIsConstant;
import org.openjfx.core.MsIsConstant.*;
import org.openjfx.core.Router;

import javax.imageio.ImageIO;

public class ImageListController {


    @FXML
    public GridPane gridPane;

    @FXML
    public ImageView imageUploadPreviewContainerEl;
    @FXML
    public Image imageUploadPreviewEl;

    @FXML
    private void switchToDownload() throws IOException {
        App.setRoot(ComponentEnum.ImageList);
    }

    private ArrayList<File> imageFileList = new ArrayList<>();

    private Channel messaging = Messaging.getInstance();

    public ImageListController() {
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

        for (int fileIndex = 0; fileIndex < imageFileList.size(); fileIndex++) {

            File file = imageFileList.get(fileIndex);

            try {

                if (columnIndex >= columnCount) {
                    rowIndex++;
                }
                columnIndex %= columnCount;

                StackPane stackPane = new StackPane();
                stackPane.getStyleClass().add("grid-cell");

                Node deleteHandler = getDeleteHandler(file);

                stackPane.getChildren().add(
                        this.getImageView(columnIndex, rowIndex, file)
                );

                if (deleteHandler != null) {
                    stackPane.getChildren().add(
                            deleteHandler
                    );
                }

                gridPane.add(stackPane, columnIndex, rowIndex);

                columnIndex++;

            } catch (Exception err) {
                err.printStackTrace();
            }
        }
    }

    private Node getDeleteHandler(File file) {

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
                setFileToView(null);
                this.showImages();
            });
        }

        return removeBtn;
//        return null;
    }

    private ImageView getImageView(int columnIndex, int rowIndex, File file) {
        int imageWidth = 100;
        int imageHeight = 100;

        ImageView imageView = new ImageView();
        Image image = this.getDefaultImage();
        try {
            image = SwingFXUtils.toFXImage(ImageIO.read(file), null);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        this.navigateToDetailOnClick(imageView, file);

        return imageView;
    }

    private void navigateToDetailOnClick(ImageView imageView, File file) {
        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            setFileToView(file);
            Router.navigateToDetailView();
        });
    }

    private void setFileToView(File file) {
        messaging.postMessage(SubjectEnum.ImageIdToShow, file);
    }

    private Image getDefaultImage() {
        return new Image(PathEnum.ImagePlaceholder.toString());
    }

    private void setDefaultImagePlaceholder() {

        gridPane.add(
                configImageView(new ImageView(
                                this.getDefaultImage()
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

        this.imageFileList.addAll(this.filterValidImageFiles(files));

        this.showImages();

    }

    private List<File> filterValidImageFiles(List<File> files) {

        return files.stream().filter(file -> {

            try {
                String mimetype = Files.probeContentType(file.toPath());
//mimetype should be something like "image/png"

                if (mimetype != null && mimetype.split("/")[0].equals("image")) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }

        }).collect(Collectors.toList());

    }


}
