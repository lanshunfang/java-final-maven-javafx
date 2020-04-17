package org.openjfx;

import java.io.File;
import java.util.*;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.openjfx.core.*;
import org.openjfx.core.ImageUtil.ImageWrapper;
import org.openjfx.core.MessageObject.*;
import org.openjfx.core.MsIsConstant.*;

public class ImageListController {

    ImageState imageState;

    // the notification controller from included fxml
    @FXML
    public VBox notification;
    @FXML
    public NotificationController notificationController;

    // the buttons controller from included fxml
    @FXML
    public VBox imageListHandlers;
    @FXML
    public ImageListHandlersController imageListHandlersController;

    @FXML
    public BorderPane borderPaneContainer;
    @FXML
    public GridPane gridPane;

    @FXML
    public VBox centerWrapper;

    @FXML
    public TextFlow copyright;
    @FXML
    public Hyperlink projectUrl;

    @FXML
    public Image imagePlaceholder;

    private Channel messaging = Messaging.getInstance();

    @FXML
    public void initialize() {

        this.initCommunicationInstance();
        this.initMessagingListen();

        this.initLayout();
        this.setNotificationToChildren();

    }

    @FXML
    public void openProjectUrl() {
        App.openUrl(this.projectUrl.getText());
    }

    /**
     * Store notification controller reference to all children need it
     */
    private void setNotificationToChildren() {
        this.imageListHandlersController.setNotification(this.notificationController);
    }

    private void initMessagingListen() {
        listenOnPickImages();
        listenImageLoaded();
        listenEditConvertStateUpdate();
        listenImageCleared();
    }

    private void initCommunicationInstance() {
        imageState = ImageState.getInstance();
    }

    /**
     * Listen on image loaded messaging
     */
    private void listenImageLoaded() {
        messaging.onMessage(SubjectEnum.OnAnImageLoaded, (data) -> {
            showLoadedImageToContainer((ImageUtil.LoadResult)data);
        });
    }

    /**
     * Listen on clear images button
     */
    private void listenImageCleared() {

        messaging.onMessage(SubjectEnum.OnImageFileListCleared, (data) -> {
            this.gridPane.getChildren().clear();
            this.gridPane.getChildren().add(
                    ImageUtil.getDefaultImageView()
            );

        });
    }

    /**
     * Any loaded images should be shown asap while loading a large chunk of files
     *
     * @param loadResult
     */
    private void showLoadedImageToContainer(ImageUtil.LoadResult loadResult) {
        ImageWrapper imageWrapper = loadResult.imageWrapper;

        StackPane stackPane = imageState.fileImageContainerHashMap.get(imageWrapper.file);

        Node deleteHandler = getDeleteHandler(imageWrapper);

        ImageView imageView = ImageUtil.getImageViewByImage(imageWrapper.image, "", 100, 100);

        // when click, we navigate to detail view
        this.navigateToDetailOnClick(imageView, imageWrapper.file);

        stackPane.getChildren().clear();

        stackPane.getChildren().add(
                imageView
        );

        if (deleteHandler != null) {
            stackPane.getChildren().add(
                    deleteHandler
            );
        }

        Node formatLabel = this.getImageFormatTag(imageWrapper.file);
        if (formatLabel != null) {
            stackPane.getChildren().add(
                    formatLabel
            );
            StackPane.setAlignment(formatLabel, Pos.TOP_RIGHT);
        }

    }

    /**
     * Get the format label (jpg/png) over the image
     * - When in converting, we also show its target format
     * @param file
     * @return
     */
    private Node getImageFormatTag(File file) {
        String fileName = file.getName();
        String ext = Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".") + 1))
                .get();

        if (ext.isBlank()) {
            return null;
        }

        if (imageState.isConverting) {
            ext += " -> " + imageState.convertFormat;
        }

        Label label = new Label(ext);
        label.getStyleClass().addAll(
                StyleClass.FontStyleEnum.FontItalic.toString(),
                StyleClass.FontStyleEnum.FontSmall.toString(),
                "bg-white-opacity-7",
                StyleClass.PaddingEnum.Padding5.toString()

        );

        label.setTextAlignment(TextAlignment.RIGHT);
        label.setTextFill(Color.web("#ffffff"));
        label.setPrefHeight(10);

        if (imageState.isConvertingInProgress) {
            label.setPrefWidth(70);
        } else {
            label.setPrefWidth(23);
        }

        return label;
    }

    /**
     * Delete button only show in Edit mode
     * @param stackPane
     */
    private void updateDeleteHandlerVisibility(StackPane stackPane) {

        Node deleteButton = stackPane.lookup("." + StyleClass.NodeClassEnum.DeleteButton.toString());
        ImageUtil.setNodeVisibility(deleteButton, imageState.isEditing);

    }

    /**
     * Get delete button and bound event
     *
     * @param imageWrapper
     * @return
     */
    private Node getDeleteHandler(ImageWrapper imageWrapper) {

        Button removeBtn = new Button("Remove");

        ImageView btnImageView = new ImageView(
                new Image(
                        PathEnum.ClosePng.toString()

                )
        );

        ImageUtil.configImageView(btnImageView, 16, 16);

        removeBtn.getStyleClass().add(StyleClass.NodeClassEnum.DeleteButton.toString());

        removeBtn.setGraphic(
                btnImageView
        );

        {
            removeBtn.setOnAction(deleteEvent -> {
//                imageFileList.remove(imageWrapper.file);
                setFileToDetailsView(null);

                imageWrapper.isMarkedToDelete = !imageWrapper.isMarkedToDelete;
                // imageWrapper.isMarkedToDelete ? "Un-delete" :
                this.deletePhotoNode(imageWrapper, (Button) deleteEvent.getTarget());
            });
        }

        ImageUtil.setNodeVisibility(removeBtn, false);

        return removeBtn;
    }

    /**
     * Action on delete the photo
     *
     * @param imageWrapper
     * @param deleteBtn
     */
    private void deletePhotoNode(ImageWrapper imageWrapper, Button deleteBtn) {
        if (!imageState.fileImageContainerHashMap.containsKey(imageWrapper.file)) {
            System.out.println("[WARN] Photo is not found in hashmap");
        }
        StackPane photoNode = imageState.fileImageContainerHashMap.get(imageWrapper.file);

        photoNode.setOpacity(imageWrapper.isMarkedToDelete ? 0.2 : 1);
        deleteBtn.setText(imageWrapper.isMarkedToDelete ? "Un-delete" : "Remove");

    }

    /**
     * Navigate to detail view
     *
     * @param imageView
     * @param file
     */
    private void navigateToDetailOnClick(ImageView imageView, File file) {
        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {

            if (imageState.isEditing || imageState.isConverting) {
                return;
            }

            setFileToDetailsView(file);
            Router.navigateToDetailView();
        });
    }

    /**
     * Messaging the file to show for detail view
     *
     * @param file
     */
    private void setFileToDetailsView(File file) {
        messaging.postMessage(SubjectEnum.ImageIdToShow, file);
    }

    private void listenOnPickImages() {
        messaging.onMessage(SubjectEnum.OnImageFileListChanged, (data) -> {
            this.prepareImageLoadingContainerList();
        });
    }

    /**
     * Right after pick file, we generated a placeholder grid for loading all images
     * - The grid will be update cell by cell on parallel image generation stream
     */
    private void prepareImageLoadingContainerList() {

        int columnCount = 3;

        for (int index = 0; index < imageState.imageFileList.size(); index++) {

            try {

                File currentFile = imageState.imageFileList.get(index);

                if (imageState.fileImageContainerHashMap.containsKey(currentFile)) {
                    continue;
                }

                int columnIndex = index % columnCount;

                int rowIndex = index / columnCount;

                StackPane stackPane = new StackPane();
                stackPane.getStyleClass().add("grid-cell");

                StringBuilder styleClasses = new StringBuilder();
                if (columnIndex == 0) {
                    styleClasses.append("first-column");
                }
                if (rowIndex == 0) {
                    styleClasses.append("first-row");
                }

                ImageView imageView = ImageUtil.getImageViewByImage(null, styleClasses.toString(), 100, 100);

                stackPane.getChildren().add(
                        imageView
                );

                Node formatLabel = new Label(String.format("%s", currentFile.getName()));

                VBox vBox = new VBox();
                vBox.getChildren().addAll(
                        formatLabel
                );

                stackPane.getChildren().add(
                        vBox
                );

                StackPane.setAlignment(vBox, Pos.CENTER);

                imageState.fileImageContainerHashMap.put(currentFile, stackPane);
                gridPane.add(stackPane, columnIndex, rowIndex);

                columnIndex++;

            } catch (Exception err) {
                err.printStackTrace();
            }
        }

    }


    /**
     * Listen of edit mode and convert mode state changes
     */
    private void listenEditConvertStateUpdate() {
        messaging.onMessage(SubjectEnum.EditConvertStateUpdate, (data) -> {
            renderEditConvertState();
        });
    }

    private void renderEditConvertState() {
        this.toggleImageListOpacity();
        Iterator iterator = imageState.fileImageContainerHashMap.entrySet().iterator();

        while (iterator.hasNext()) {
            this.updateDeleteHandlerVisibility((StackPane) ((Map.Entry) iterator.next()).getValue());
        }
    }

    /**
     * Set the layout dimension
     */
    private void initLayout() {
        this.borderPaneContainer.prefHeight(500);
        this.borderPaneContainer.prefWidth(500);
        BorderPane.setAlignment(this.centerWrapper, Pos.CENTER);
        BorderPane.setAlignment(this.copyright, Pos.CENTER);


    }

    /**
     * When editing, the image list is semi-transparent
     */
    private void toggleImageListOpacity() {
        double opacity = imageState.isConverting ? 0.5 : 1;
        FadeTransition ft = new FadeTransition(Duration.millis(500), this.gridPane);
        ft.setToValue(opacity);
        ft.play();

    }


}
