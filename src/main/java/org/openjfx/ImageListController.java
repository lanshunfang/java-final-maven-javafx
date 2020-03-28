package org.openjfx;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import org.openjfx.core.*;
import org.openjfx.core.MessageObject.*;
import org.openjfx.core.MsIsConstant.*;

public class ImageListController {

    @FXML
    public GridPane gridPane;

    @FXML
    public Image imagePlaceholder;
    @FXML
    public Button editButton;

    private ArrayList<File> imageFileList = new ArrayList<>();

    private int maxImageFiles = 50;

    private Channel messaging = Messaging.getInstance();

    @FXML
    Label maxImageLabel;

    private boolean isEditing;

    @FXML
    public void initialize() {
        this.setMaxImageFiles();
        this.toggleEditButton();
    }

    private void setMaxImageFiles() {
        this.maxImageLabel.setText("Maximum images: " + maxImageFiles);
    }

    private void showImages() {

        gridPane.getChildren().clear();

        this.toggleEditButton();

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

                StringBuilder styleClasses = new StringBuilder();
                if (columnIndex == 0) {
                    styleClasses.append("first-column");
                }
                if (rowIndex == 0) {
                    styleClasses.append("first-row");
                }

                ImageView imageView = ImageUtil.getImageViewByFile(file, styleClasses.toString(), 100, 100);
                this.navigateToDetailOnClick(imageView, file);

                stackPane.getChildren().add(
                        imageView
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

        if (!this.isEditing) {
            return null;
        }

        Button removeBtn = new Button("Remove");

        ImageView btnImageView = new ImageView(
                new Image(
                        PathEnum.ClosePng.toString()

                )
        );

        ImageUtil.configImageView(btnImageView, 16, 16);

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

    private void navigateToDetailOnClick(ImageView imageView, File file) {
        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
            setFileToView(file);
            Router.navigateToDetailView();
        });
    }

    private void setFileToView(File file) {
        messaging.postMessage(SubjectEnum.ImageIdToShow, file);
    }

    private void setDefaultImagePlaceholder() {

        gridPane.add(
                ImageUtil.configImageView(new ImageView(
                                ImageUtil.getDefaultImage()
                        ),
                        100,
                        100
                ),
                0,
                0
        );

    }

    @FXML
    public void onPickImageAction(Event event) {

        List<File> files = App.openFileChooser();

        if (files == null) {
            return;
        }

        this.imageFileList.addAll(this.filterValidImageFiles(files));

        if (this.imageFileList.size() > maxImageFiles) {
            this.imageFileList.subList(0, maxImageFiles).clear();
        }

        this.showImages();
    }


    @FXML
    public void onEditImageListAction(Event event) {
        this.isEditing = !this.isEditing;
        this.showImages();

        if (this.isEditing) {
            this.editButton.setText("Done");
        } else {
            this.editButton.setText("Edit");
        }
    }

    private List<File> filterValidImageFiles(List<File> files) {

        return files.stream().filter(file -> {

            try {
                String mimetype = Files.probeContentType(file.toPath());

                if (mimetype != null && mimetype.split("/")[0].equals("image")) {
                    return true;
                }
                return false;
            } catch (Exception e) {
                return false;
            }

        }).collect(Collectors.toList());

    }

    private void toggleEditButton() {
        boolean isShow = this.imageFileList.size() > 0;
        this.editButton.setVisible(isShow);
        this.editButton.setManaged(isShow);
    }


}
