package org.openjfx;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.FadeTransition;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.openjfx.core.*;
import org.openjfx.core.MessageObject.*;
import org.openjfx.core.MsIsConstant.*;

public class ImageListController {

    @FXML
    public GridPane gridPane;

    @FXML
    public Image imagePlaceholder;
    @FXML
    public HBox defaultActionContainer;
    @FXML
    public HBox editItemContainer;
    @FXML
    public Button editButton;
    @FXML
    public Button convertButton;

    @FXML
    public HBox convertingWrapper;
    @FXML
    public Button cancelConvertButton;
    @FXML
    public Button startConvertButton;

    @FXML
    public ComboBox convertFormatComboBox;

    private ArrayList<File> imageFileList = new ArrayList<>();

    private int maxImageFiles = 50;

    private Channel messaging = Messaging.getInstance();

    @FXML
    Label maxImageLabel;

    private boolean isEditing;
    private boolean isConverting;
    private boolean isConvertingInProgress;

    @FXML
    public void initialize() {
        this.setMaxImageFiles();
        this.toggleEditItemWrapper();
        this.initConvertTools();
    }

    private void setMaxImageFiles() {
        this.maxImageLabel.setText("Maximum images: " + maxImageFiles);
    }

    private void repaintImageList() {

        gridPane.getChildren().clear();

        this.toggleEditItemWrapper();

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
                this.repaintImageList();
            });
        }

        return removeBtn;
//        return null;
    }

    private void navigateToDetailOnClick(ImageView imageView, File file) {
        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {

            if (this.isEditing || this.isConverting) {
                return;
            }

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

        this.repaintImageList();
    }


    @FXML
    public void onEditImageListAction(Event event) {
        this.isEditing = !this.isEditing;

        if (this.isEditing) {
            this.isConverting = false;
        }

        this.renderEditConvertState();

    }

    private void toggleEditState() {
        this.repaintImageList();

        if (this.isEditing) {
            this.editButton.setText("Done Editing");
        } else {
            this.editButton.setText("Edit");
        }
    }

    @FXML
    public void onConvertImageListAction(Event event) {
        this.isConverting = !this.isConverting;

        if (this.isConverting) {
            this.isEditing = false;
        }

        this.renderEditConvertState();
    }

    @FXML
    public void onCancelConvertImageListAction(Event event) {
        this.isConverting = false;
        this.isConvertingInProgress = false;
        this.toggleStartConvertButtonState();
        this.renderEditConvertState();
    }
    @FXML
    public void onStartConvertImageListAction(Event event) {
        this.isConvertingInProgress = true;

        this.toggleStartConvertButtonState();

    }

    private void toggleStartConvertButtonState() {
        this.startConvertButton.setText(this.isConvertingInProgress ? "Converting" : "Start Convert");
        this.startConvertButton.setDisable(this.isConvertingInProgress);
        this.convertFormatComboBox.setDisable(this.isConvertingInProgress);
    }

    private void renderEditConvertState() {
        this.toggleEditState();
        this.toggleConvertMode();
    }

    private void toggleConvertMode() {

        this.toggleDefaultActionContainer();
        this.toggleConvertWrapper();
        this.toggleImageListOpacity();
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

    private void toggleEditItemWrapper() {
        boolean isShow = this.imageFileList.size() > 0;
        this.editItemContainer.setVisible(isShow);
        this.editItemContainer.setManaged(isShow);
    }

    private void toggleDefaultActionContainer() {
        this.defaultActionContainer.setVisible(!this.isConverting);
        this.defaultActionContainer.setManaged(!this.isConverting);

    }
    private void toggleConvertWrapper() {
        this.convertingWrapper.setVisible(this.isConverting);
        this.convertingWrapper.setManaged(this.isConverting);

    }

    private void initConvertTools() {
        this.convertFormatComboBox.getItems().clear();
        this.convertFormatComboBox.getItems().addAll(
                Arrays.asList(ImageConvertingFormatEnum.values())
        );
        this.convertFormatComboBox.getSelectionModel().selectFirst();

        this.toggleConvertMode();
        this.toggleImageListOpacity();
    }

    private void toggleImageListOpacity() {
        double opacity = this.isConverting ? 0.5 : 1;
        FadeTransition ft = new FadeTransition(Duration.millis(500), this.gridPane);
        ft.setToValue(opacity);
        ft.play();

    }



}
