package org.openjfx;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
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
import org.openjfx.core.MessageObject.*;
import org.openjfx.core.MsIsConstant.*;

public class ImageListController {

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
    public Button goBackButton;
    @FXML
    public Button startConvertButton;

    @FXML
    public SplitMenuButton convertFormatSplitMenuButton;

    public ProgressBar progressBar;

    private ArrayList<File> imageFileList = new ArrayList<>();

    private int maxImageFiles = 50;

    private Channel messaging = Messaging.getInstance();

    @FXML
    Button pickButton;

    private boolean isEditing;
    private boolean isConverting;
    private boolean isConvertingInProgress;

    private String convertFormat;

    @FXML
    public void initialize() {
        this.initLayout();
        this.showWelcome();
        this.setMaxImageFiles();
        this.toggleEditItemWrapper();
        this.initConvertTools();
    }

    @FXML
    public void openProjectUrl() {
        App.openUrl(this.projectUrl.getText());
    }

    private void setMaxImageFiles() {
        Tooltip tooltip = new Tooltip("Maximum images: " + maxImageFiles);
        tooltip.getStyleClass().addAll("tooltip-info");
        this.pickButton.setTooltip(tooltip);
    }

    private void repaintImageList() {

        gridPane.getChildren().clear();

        this.toggleEditItemWrapper();

        if (imageFileList.size() == 0) {
            setDefaultImagePlaceholder();
            return;
        }

        int columnCount = 3;

        List<ImageUtil.ImageWrapper> imageWrappers = ImageUtil.getImageList(imageFileList);

        for (ImageUtil.ImageWrapper imageWrapper : imageWrappers) {

            try {

                int columnIndex = imageWrapper.index % columnCount;

                int rowIndex = imageWrapper.index / columnCount;

                StackPane stackPane = new StackPane();
                stackPane.getStyleClass().add("grid-cell");

                Node deleteHandler = getDeleteHandler(imageWrapper.file);

                StringBuilder styleClasses = new StringBuilder();
                if (columnIndex == 0) {
                    styleClasses.append("first-column");
                }
                if (rowIndex == 0) {
                    styleClasses.append("first-row");
                }

                ImageView imageView = ImageUtil.getImageViewByImage(imageWrapper.image, styleClasses.toString(), 100, 100);

                this.navigateToDetailOnClick(imageView, imageWrapper.file);

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

                gridPane.add(stackPane, columnIndex, rowIndex);

                columnIndex++;

            } catch (Exception err) {
                err.printStackTrace();
            }
        }

    }

    @FXML
    public void onCloseNotificationAction() {
        this.setNodeVisibility(this.globalNotificationContainer, false);
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

        if (this.isConvertingInProgress) {
            ext += " -> " + this.convertFormatSplitMenuButton.getText();
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

        if (this.isConvertingInProgress) {
            label.setPrefWidth(70);
        } else {
            label.setPrefWidth(23);
        }

        return label;
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
        this.toggleNotification(false);

        List<File> files = App.openFileChooser();

        if (files == null) {
            return;
        }

        this.imageFileList.addAll(this.filterValidImageFiles(files));

        if (this.imageFileList.size() > maxImageFiles) {
            this.notifyInfo("Only allow 50 images. Auto clear " + (this.imageFileList.size() - maxImageFiles) + " images");
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
            this.notifyInfo("Select the image format to convert to");
        }

        this.renderEditConvertState();

    }

    @FXML
    public void onGoBackImageListAction(Event event) {
        this.goBackToList();
    }

    private void goBackToList() {
        this.isConverting = false;
        this.isConvertingInProgress = false;
        this.toggleConvertingState();
    }

    @FXML
    public void onStartConvertImageListAction(Event event) {
        this.isConvertingInProgress = true;

        this.startConvert();
    }

    private void startConvert() {
        File outputDirectory = App.openDirectoryChooser();
        if (outputDirectory == null) {
            return;
        }

        outputDirectory = ImageUtil.initOutputFolderWithTimestamp(outputDirectory, "converted-");

        this.toggleConvertingState();

        boolean isAllSuccess = ImageUtil.convertParallel(this.imageFileList, outputDirectory, this.getFormat());

        this.finishConvert(outputDirectory, isAllSuccess);
    }

    private void finishConvert(File outputDirectory, boolean isAllSuccess) {
        this.isConvertingInProgress = false;
        this.toggleConvertingState();

//        App.openFile(outputDirectory);

        this.informResult(isAllSuccess, outputDirectory);

        this.goBackToList();

    }

    private void informResult(boolean isAllSuccess, File outputDirectory) {

        HBox msgContainerHBox = new HBox();

        msgContainerHBox.setAlignment(Pos.CENTER);

        String savedFolder = outputDirectory.getAbsolutePath();

        Text msgText = new Text(
                isAllSuccess ?    "All done. " + (

                        this.imageFileList.size() > 1
                                ? this.imageFileList.size() + " images are"
                                : this.imageFileList.size() + " image is"
                ) + " saved in "
                        :  "Some images could not be converted. Please check "
        );

        Text folderPathText = new Text(savedFolder);

        folderPathText.setUnderline(true);

        Button openDirectoryNode = new Button("Open");
        openDirectoryNode.getStyleClass().addAll("btn", "btn-default");
        openDirectoryNode.setOnAction(event -> {
            App.openFile(outputDirectory);
        });

        msgContainerHBox.getChildren().addAll(
                msgText,
                folderPathText,
                NodeUtil.getPaddingNode(),
                openDirectoryNode
        );

        if (isAllSuccess) {
            this.notifyInfo(msgContainerHBox);
        } else {
            this.notifyWarn(msgContainerHBox);
        }

    }

    private String getFormat() {
        return this.convertFormat;
    }

    private void toggleConvertingState() {
        this.startConvertButton.setText(this.isConvertingInProgress ? "Converting" : "Pick download directory");
        this.startConvertButton.setDisable(this.isConvertingInProgress || this.getFormat().equals("Select Format"));
        this.convertFormatSplitMenuButton.setDisable(this.isConvertingInProgress);

        if (this.isConvertingInProgress) {
            this.progressBar = this.prepareProgressBar();
        }

        this.renderEditConvertState();

    }

    private void updateProgress() {

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
        this.setNodeVisibility(this.editItemContainer, isShow);

    }

    private void initLayout() {
        this.borderPaneContainer.prefHeight(500);
        this.borderPaneContainer.prefWidth(500);
        BorderPane.setAlignment(this.centerWrapper, Pos.CENTER);
        BorderPane.setAlignment(this.copyright, Pos.CENTER);


    }

    private void showWelcome() {
        this.notifyInfo("Pick an image(s) to start");
    }

    private void toggleDefaultActionContainer() {
        this.setNodeVisibility(this.defaultActionContainer, !this.isConverting);

    }

    private void toggleConvertWrapper() {
        this.setNodeVisibility(this.convertingWrapper, this.isConverting);
    }

    private void initConvertTools() {

        List<MenuItem> menuItems = Stream.of(ImageConvertingFormatEnum.values()).map(
                enumItem -> {
                    MenuItem menuItem = new MenuItem(enumItem.toString());
                    menuItem.setOnAction(
                            e -> {
                                String format = ((MenuItem) e.getTarget()).getText();
                                this.setConvertFormat(format);
                                this.convertFormatSplitMenuButton.setText(format);
                                this.repaintImageList();
                                this.notifyInfo("Pick a download directory");
                            }
                    );
                    return menuItem;
                }
        ).collect(
                Collectors.toList()
        );

        this.convertFormatSplitMenuButton.getItems().clear();
        this.convertFormatSplitMenuButton.getItems().addAll(
                menuItems
        );
//        this.convertFormatSplitMenuButton.setOnAction();

        this.toggleConvertMode();
        this.toggleImageListOpacity();

        this.startConvertButton.setTooltip(
                new Tooltip("Start to convert")
        );

        this.setNodeVisibility(this.startConvertButton, false);

    }

    private void setConvertFormat(String format) {
        this.convertFormat = format;
        this.setNodeVisibility(this.startConvertButton, true);
    }

    private ProgressBar prepareProgressBar() {
        VBox vBox = new VBox();
        ProgressBar progressBar = new ProgressBar(.1);
        vBox.getChildren().addAll(
                new Text("Converting"),
                progressBar
        );
        this.notifyInfo(vBox);
        return progressBar;
    }

    private void toggleImageListOpacity() {
        double opacity = this.isConverting ? 0.5 : 1;
        FadeTransition ft = new FadeTransition(Duration.millis(500), this.gridPane);
        ft.setToValue(opacity);
        ft.play();

    }

    private void notifyInfo(String message) {

        this.notify(message, "alert", "alert-info");

    }

    private void notify(String message, String... styleClasses) {
        Text alertMsg = new Text(message);
        this.notify(alertMsg, styleClasses);
        this.globalNotificationContainer.getStyleClass().addAll(
                styleClasses
        );

        this.toggleNotification(true);

    }

    private void notify(Node node, String... styleClasses) {

        this.globalNotificationContainer.getStyleClass().addAll(
                styleClasses
        );

        this.notify(node);

        this.toggleNotification(true);

    }

    private void notify(Node node) {

        this.globalNotificationTextFlowAlert.getChildren().clear();
        this.globalNotificationTextFlowAlert.getChildren().addAll(node);

        this.toggleNotification(true);

    }

    private void notifyInfo(Node node) {

        this.notify(node, "alert", "alert-info");

    }


    private void notifyWarn(String message) {
        this.notify(message, "alert", "alert-warn");

    }
    private void notifyWarn(Node node) {
        this.notify(node, "alert", "alert-warn");

    }

    private void toggleNotification(boolean isShow) {
        this.setNodeVisibility(this.globalNotificationContainer, isShow);
    }

    private void setNodeVisibility(Node node, boolean isShow) {
        node.setVisible(isShow);
        node.setManaged(isShow);
    }


}
