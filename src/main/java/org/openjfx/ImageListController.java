package org.openjfx;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.openjfx.core.*;
import org.openjfx.core.ImageUtil.ImageWrapper;
import org.openjfx.core.MessageObject.*;
import org.openjfx.core.MsIsConstant.*;

public class ImageListController {

    ImageState imageState;
    Notification notification;

    @FXML
    public BorderPane borderPaneContainer;
    @FXML
    public GridPane gridPane;
    @FXML
    public HBox globalNotificationContainer;
    @FXML
    public TextFlow globalNotificationTextFlowAlert;
    @FXML
    public VBox centerWrapper;
    @FXML
    public TextFlow copyright;
    @FXML
    public Hyperlink projectUrl;

    @FXML
    public Image imagePlaceholder;


    public HashMap<File, StackPane> fileImageContainerHashMap = new HashMap<>();

    private Channel messaging = Messaging.getInstance();

    @FXML
    public void initialize() {

        this.initCommunicationInstance();
        this.initMessagingListen();

        this.initLayout();
        this.showWelcome();

    }

    @FXML
    public void openProjectUrl() {
        App.openUrl(this.projectUrl.getText());
    }

    private void initMessagingListen() {
        listenOnPickImages();
        listenImageLoaded();
        listenEditConvertStateUpdate();
    }

    private void initCommunicationInstance() {
        imageState = ImageState.getInstance();
        notification = Notification.getInstance(
                globalNotificationContainer,
                globalNotificationTextFlowAlert
        );
    }

    private void listenImageLoaded() {
        messaging.onMessage(SubjectEnum.OnAnImageLoaded, (data) -> {
            showLoadedImageToContainer((ImageUtil.LoadResult)data);
        });
    }

    private void showLoadedImageToContainer(ImageUtil.LoadResult loadResult) {
        ImageWrapper imageWrapper = loadResult.imageWrapper;

        StackPane stackPane = this.fileImageContainerHashMap.get(imageWrapper.file);

        Node deleteHandler = getDeleteHandler(imageWrapper);

        ImageView imageView = ImageUtil.getImageViewByImage(imageWrapper.image, "", 100, 100);

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

    @FXML
    public void onCloseNotificationAction() {
        messaging.postMessage(MessageObject.SubjectEnum.OnCloseNotification, true);
    }

    private Node getImageFormatTag(File file) {
        String fileName = file.getName();
        String ext = Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(f.lastIndexOf(".") + 1))
                .get();

        if (ext.isBlank()) {
            return null;
        }

        if (imageState.isConvertingInProgress) {
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

    private void updateDeleteHandlerVisibility(StackPane stackPane) {

        Node deleteButton = stackPane.lookup("." + StyleClass.NodeClassEnum.DeleteButton.toString());
        ImageUtil.setNodeVisibility(deleteButton, imageState.isEditing);

    }

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

    private void deletePhotoNode(ImageWrapper imageWrapper, Button deleteBtn) {
        if (!this.fileImageContainerHashMap.containsKey(imageWrapper.file)) {
            System.out.println("[WARN] Photo is not found in hashmap");
        }
        StackPane photoNode = this.fileImageContainerHashMap.get(imageWrapper.file);

        photoNode.setOpacity(imageWrapper.isMarkedToDelete ? 0.2 : 1);
        deleteBtn.setText(imageWrapper.isMarkedToDelete ? "Un-delete" : "Remove");

    }

    private void navigateToDetailOnClick(ImageView imageView, File file) {
        imageView.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {

            if (imageState.isEditing || imageState.isConverting) {
                return;
            }

            setFileToDetailsView(file);
            Router.navigateToDetailView();
        });
    }

    private void setFileToDetailsView(File file) {
        messaging.postMessage(SubjectEnum.ImageIdToShow, file);
    }

    private void listenOnPickImages() {
        messaging.onMessage(SubjectEnum.OnPickImages, (data) -> {
            this.prepareImageLoadingContainerList();
        });
    }

    private void prepareImageLoadingContainerList() {

        int columnCount = 3;

        for (int index = 0; index < imageState.imageFileList.size(); index++) {

            try {

                File currentFile = imageState.imageFileList.get(index);

                if (fileImageContainerHashMap.containsKey(currentFile)) {
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

                Node loadingImage = ImageUtil.getImageViewByImage(ImageUtil.getLoadingSpinner(), styleClasses.toString(), 100, 100);
                Node formatLabel = new Label(String.format("%s", currentFile.getName()));

                VBox vBox = new VBox();
                vBox.getChildren().addAll(
                        loadingImage,
                        formatLabel
                );

                stackPane.getChildren().add(
                        vBox
                );

                StackPane.setAlignment(vBox, Pos.CENTER);

                fileImageContainerHashMap.put(currentFile, stackPane);
                gridPane.add(stackPane, columnIndex, rowIndex);

                columnIndex++;

            } catch (Exception err) {
                err.printStackTrace();
            }
        }

    }


    private void listenEditConvertStateUpdate() {
        messaging.onMessage(SubjectEnum.EditConvertStateUpdate, (data) -> {
            renderEditConvertState();
        });
    }
    private void renderEditConvertState() {
        this.toggleImageListOpacity();
        Iterator iterator = this.fileImageContainerHashMap.entrySet().iterator();

        while (iterator.hasNext()) {
            this.updateDeleteHandlerVisibility((StackPane) ((Map.Entry) iterator.next()).getValue());
        }
    }

    private void initLayout() {
        this.borderPaneContainer.prefHeight(500);
        this.borderPaneContainer.prefWidth(500);
        BorderPane.setAlignment(this.centerWrapper, Pos.CENTER);
        BorderPane.setAlignment(this.copyright, Pos.CENTER);


    }

    private void showWelcome() {
        notification.notifyInfo("Pick an image(s) to start");
    }

    private void toggleImageListOpacity() {
        double opacity = imageState.isConverting ? 0.5 : 1;
        FadeTransition ft = new FadeTransition(Duration.millis(500), this.gridPane);
        ft.setToValue(opacity);
        ft.play();

    }


}
