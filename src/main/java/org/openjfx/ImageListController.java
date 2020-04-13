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
    @FXML
    public SplitMenuButton convertFilterSplitMenuButton;

    public HashMap<File, StackPane> fileImageContainerHashMap = new HashMap<>();

    public ProgressBar progressBar;
    public Text progressText;

    private ArrayList<File> imageFileList = new ArrayList<>();
    private ArrayList<ImageWrapper> imageWrapperList = new ArrayList<>();

    private int maxImageFiles = 50;

    private Channel messaging = Messaging.getInstance();

    @FXML
    Button pickButton;

    private boolean isEditing;
    private boolean isConverting;
    private boolean isConvertingInProgress;

    private String convertFormat = "";
    private HashMap<String, ArrayList<String>> convertFilterParams = new HashMap();
    private ArrayList<String> convertFilterParamsDisplay = new ArrayList<>();

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
        this.pickButton.setTooltip(
                this.getTooltip("Maximum images: " + maxImageFiles)
        );
    }

    private Tooltip getTooltip(String tooltip) {
        Tooltip tooltipE = new Tooltip(tooltip);
        tooltipE.getStyleClass().addAll("tooltip-info");
        return tooltipE;
    }

    private void updateImageModel(Consumer<List<ImageWrapper>> consumer) {
        ImageUtil.updateImageListParallel(
                imageFileList,
                imageWrapperList,

                (loadResult) -> {
//                    this.notifyInfo(String.format("Loading %d image(s)", imageWrapperList.size(), ));
                    safeUpdateProgress(loadResult.progress, "Loading");

                    ImageUtil.safeJavaFxExecute((data) -> {
                        showLoadedImageToContainer(loadResult);
                    });

                },
                (imageWrapperList) -> {
                    safeUpdateProgress(1, "Loaded");

//                    this.imageWrapperList.clear();
                    this.imageWrapperList.addAll(imageWrapperList);
                    consumer.accept(imageWrapperList);
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
//
//    private void repaintImageList() {
//
//        this.notifyInfo("Loading images");
//
//        gridPane.getChildren().clear();
//
//        this.toggleEditItemWrapper();
//
//        if (imageFileList.size() == 0) {
//            setDefaultImagePlaceholder();
//            return;
//        }
//
//        int columnCount = 3;
//
//        for (ImageWrapper imageWrapper : imageWrapperList) {
//
//            try {
//
//                if (imageWrapper.isMarkedToDelete) {
//                    continue;
//                }
//
//                int columnIndex = imageWrapper.index % columnCount;
//
//                int rowIndex = imageWrapper.index / columnCount;
//
//                StackPane stackPane = new StackPane();
//                stackPane.getStyleClass().add("grid-cell");
//
//                Node deleteHandler = getDeleteHandler(imageWrapper);
//
//                StringBuilder styleClasses = new StringBuilder();
//                if (columnIndex == 0) {
//                    styleClasses.append("first-column");
//                }
//                if (rowIndex == 0) {
//                    styleClasses.append("first-row");
//                }
//
//                ImageView imageView = ImageUtil.getImageViewByImage(imageWrapper.image, styleClasses.toString(), 100, 100);
//
//                this.navigateToDetailOnClick(imageView, imageWrapper.file);
//
//                stackPane.getChildren().add(
//                        imageView
//                );
//
//                if (deleteHandler != null) {
//                    stackPane.getChildren().add(
//                            deleteHandler
//                    );
//                }
//
//                Node formatLabel = this.getImageFormatTag(imageWrapper.file);
//                if (formatLabel != null) {
//                    stackPane.getChildren().add(
//                            formatLabel
//                    );
//                    StackPane.setAlignment(formatLabel, Pos.TOP_RIGHT);
//                }
//
//                fileImageContainerHashMap.put(imageWrapper.file, stackPane);
//                gridPane.add(stackPane, columnIndex, rowIndex);
//
//                columnIndex++;
//
//            } catch (Exception err) {
//                err.printStackTrace();
//            }
//        }
//
//        this.notifyInfo(this.imageFileList.size() > 0 ? "Images loaded" : "");
//
//    }

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

    private void updateDeleteHandlerVisibility(StackPane stackPane) {

        Node deleteButton = stackPane.lookup("." + StyleClass.NodeClassEnum.DeleteButton.toString());
        this.setNodeVisibility(deleteButton, this.isEditing);

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

        this.setNodeVisibility(removeBtn, false);

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

            if (this.isEditing || this.isConverting) {
                return;
            }

            setFileToDetailsView(file);
            Router.navigateToDetailView();
        });
    }

    private void setFileToDetailsView(File file) {
        messaging.postMessage(SubjectEnum.ImageIdToShow, file);
    }
//
//    private void setDefaultImagePlaceholder() {
//
//        gridPane.getChildren().clear();
//        gridPane.add(
//                ImageUtil.configImageView(new ImageView(
//                                ImageUtil.getDefaultImage()
//                        ),
//                        100,
//                        100
//                ),
//                0,
//                0
//        );
//
//    }

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
        } else {
            this.notifyInfo(String.format("Loading %s image(s)", imageWrapperList.size()));
        }

        this.prepareImageLoadingContainerList();

        this.updateImageModel(
                (imageWrapperList) -> {
                    ImageUtil.safeJavaFxExecute((data) -> {
//                        this.repaintImageList();
                        this.notifyInfo(String.format("%s new image(s) loaded", imageWrapperList.size()));
                        this.toggleEditItemWrapper();

                    });

                }
        );

    }

    private void prepareImageLoadingContainerList() {

        int columnCount = 3;

        for (int index = 0; index < this.imageFileList.size(); index++) {

            try {

                File currentFile = this.imageFileList.get(index);

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


    @FXML
    public void onEditImageListAction(Event event) {
        this.isEditing = !this.isEditing;

        if (this.isEditing) {
            this.isConverting = false;
        }

        this.renderEditConvertState();

    }

    private void toggleEditState() {
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

    private void listenImageConvertingMessage() {

        this.messaging.onMessage(SubjectEnum.ImageConvertingInProgress, (inProgressData) -> {
            ImageUtil.ConvertResult convertResult = (ImageUtil.ConvertResult) inProgressData;
            safeUpdateProgress(convertResult.progress, "Converting");

        });
    }

    private void safeUpdateProgress(double progress, String prefix) {
        Platform.runLater(() -> {
            double displayProgress = progress < 0.03 ? 0.03 : progress;
            this.setProgress(displayProgress, prefix);
        });
    }

    private void setProgress(double percentage, String prefix) {
        if (percentage >= 1) {
            this.progressBar = null;
            this.progressText = null;
            this.notifyInfo("Almost done");
            return;
        }

        percentage = percentage < 0.95 ? percentage : 0.95;

        if (this.progressBar == null) {
            this.prepareProgressBar();
        }

        this.progressText.setText(
                String.format("%s: %3.2f%%", prefix, percentage * 100)
        );

        this.progressBar.setProgress(percentage);
    }

    private void startConvert() {
        File outputDirectory = App.openDirectoryChooser();
        if (outputDirectory == null) {
            return;
        }

        final File outputDirectoryUniq = ImageUtil.initOutputFolderWithTimestamp(outputDirectory, "converted-");

        this.toggleConvertingState();

        safeUpdateProgress(0.05, "Converting");

        ImageUtil.convertParallel(
//                this.imageFileList,
                this.imageWrapperList,
                outputDirectoryUniq,
                this.getFormat(),
                this.convertFilterParams,
                (imageWrapper) -> this.isConvertingInProgress && !imageWrapper.isMarkedToDelete,
                (inProgressData) -> {
                    this.messaging.postMessage(SubjectEnum.ImageConvertingInProgress, inProgressData);
                },
                (isAllSuccess) -> {
                    this.finishConvert(outputDirectoryUniq, isAllSuccess);
                }
        );


    }

    private void finishConvert(File outputDirectory, boolean isAllSuccess) {
        safeUpdateProgress(1, "Done");

        ImageUtil.safeJavaFxExecute((data) -> {
            this.isConvertingInProgress = false;
            this.toggleConvertingState();

            this.informResult(isAllSuccess, outputDirectory);

            this.goBackToList();
        });


    }

    private void informResult(boolean isAllSuccess, File outputDirectory) {

        HBox generateContainerHBox = new HBox();
        VBox msgContainerVBox = new VBox();

        generateContainerHBox.setAlignment(Pos.CENTER);

        String savedFolder = outputDirectory.getAbsolutePath();

        Text msgText = new Text(
                isAllSuccess
                        ? "All done. "
                        + (

                        this.imageFileList.size() > 1
                                ? this.imageFileList.size() + " images(" + this.convertFormat + ") are"
                                : this.imageFileList.size() + " image(" + this.convertFormat + ")  is")
                        + " saved in "
                        : "Some images could not be converted. Please check "
        );

        String filterDisplay = this.convertFilterParamsDisplay.size() > 0
                ? "Applied filter(s): " + convertFilterParamsDisplay.toString()
                : "";

        Hyperlink folderPathText = new Hyperlink(savedFolder);

        folderPathText.setOnMouseClicked(mouseEvent -> {
            App.openFile(outputDirectory);
        });

        folderPathText.setUnderline(true);

        Button openDirectoryNode = new Button("Open");
        openDirectoryNode.getStyleClass().addAll("btn", "btn-default");
        openDirectoryNode.setOnAction(event -> {
            App.openFile(outputDirectory);
        });

        msgContainerVBox.getChildren().addAll(
                msgText,
                folderPathText,
                new Text(filterDisplay)

        );

        generateContainerHBox.getChildren().addAll(
                msgContainerVBox,
                NodeUtil.getPaddingNode(),
                openDirectoryNode
        );

        if (isAllSuccess) {
            this.notifyInfo(generateContainerHBox);
        } else {
            this.notifyWarn(generateContainerHBox);
        }

    }

    private String getFormat() {
        return this.convertFormat;
    }

    private void toggleConvertingState() {

        this.startConvertButton.setText(this.isConvertingInProgress ? "Converting" : "Pick download directory");
        this.startConvertButton.setDisable(this.isConvertingInProgress || this.getFormat().equals("Select Format"));
        this.convertFormatSplitMenuButton.setDisable(this.isConvertingInProgress);
        this.convertFilterSplitMenuButton.setDisable(this.isConvertingInProgress);

        this.renderEditConvertState();

    }

    private void renderEditConvertState() {
        this.toggleEditState();
        this.toggleConvertMode();
        Iterator iterator = this.fileImageContainerHashMap.entrySet().iterator();

        while (iterator.hasNext()) {
            this.updateDeleteHandlerVisibility((StackPane) ((Map.Entry) iterator.next()).getValue());
        }
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

    private void initFormatSplitMenu() {
        List<MenuItem> menuItems = Stream.of(ImageConvertingFormatEnum.values()).map(
                enumItem -> {
                    MenuItem menuItem = new MenuItem(enumItem.toString());
                    menuItem.setOnAction(
                            e -> {
                                String format = ((MenuItem) e.getTarget()).getText();
                                this.setConvertFormat(format);
                                this.convertFormatSplitMenuButton.setText(format);
//                                this.repaintImageList();
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
    }

    private void initFilterSplitMenu() {
        List<MenuItem> menuItems = Stream.of(ImageConvertingFilterEnum.values()).map(
                enumItem -> {
                    MenuItem menuItem = new MenuItem(enumItem.toString());
                    menuItem.setOnAction(
                            e -> {
                                this.setConvertFilter(enumItem);
                            }
                    );
                    return menuItem;
                }
        ).collect(
                Collectors.toList()
        );

        this.convertFilterSplitMenuButton.getItems().clear();
        this.convertFilterSplitMenuButton.getItems().addAll(
                menuItems
        );

    }

    private void initConvertTools() {

        this.initFormatSplitMenu();
        this.initFilterSplitMenu();

        this.toggleConvertMode();
        this.toggleImageListOpacity();

        this.startConvertButton.setTooltip(
                this.getTooltip("Start to convert")
        );

        this.setNodeVisibility(this.startConvertButton, false);

        this.listenImageConvertingMessage();

    }

    private void setConvertFormat(String format) {
        this.convertFormat = format;
        this.setNodeVisibility(this.startConvertButton, true);
    }

    private void setFilterButtonTextAndTooltip() {

        this.convertFilterParamsDisplay.clear();

        this.convertFilterParams.entrySet().forEach(
                (entry) -> {

                    this.convertFilterParamsDisplay.add(
                            String.format(
                                    "%s: %s",
                                    entry.getKey(),
                                    entry.getValue().toString()
                            )
                    );
                }
        );

        StringJoiner stringJoiner = new StringJoiner("\n");

        String displayOnButton = String.format(
                "%s filter(s)",
                this.convertFilterParamsDisplay.size()
        );

        this.convertFilterSplitMenuButton.setText(displayOnButton);

        this.convertFilterParamsDisplay.forEach(
                (item) -> {
                    stringJoiner.add(item);
                }
        );
        this.convertFilterSplitMenuButton.setTooltip(
                getTooltip(
                        stringJoiner.toString()
                )
        );
    }

    private void setConvertFilter(ImageConvertingFilterEnum filterOption) {
        switch (filterOption) {

            case ClearAll: {
                this.convertFilterParams.clear();
                this.convertFilterParamsDisplay.clear();
                this.convertFilterSplitMenuButton.setText("Select Filter (optional)");

            }
            break;

            default: {

                if (filterOption.isAccept1Param) {
                    promptForInput(filterOption.hint, filterOption.displayValue, filterOption.defaultParamValue, (enteredValue) -> {
                        this.actionOnFilterSelect(
                                filterOption,
                                String.format(
                                        filterOption.value,
                                        enteredValue
                                )
                        );
                    });
                } else {
                    this.actionOnFilterSelect(filterOption, filterOption.value);

                }


            }
            break;

        }

        this.setFilterButtonTextAndTooltip();

    }

    private void actionOnFilterSelect(ImageConvertingFilterEnum filterOption, String params) {
        if (this.convertFilterParams.containsKey(filterOption.displayValue)) {
            this.convertFilterParams.remove(filterOption.displayValue);
            return;
        }

        this.convertFilterParams.put(
                filterOption.displayValue,
                new ArrayList<>(
                        Arrays.asList(
                                params.split(" ")
                        )
                )
        );

    }

    private void promptForInput(String hint, String prefixLabel, String defaultValue, Consumer<String> consumer) {
        TextInputDialog textInputDialog = new TextInputDialog(defaultValue);
        textInputDialog.setHeaderText(hint);
        textInputDialog.setTitle("Provide value");
        textInputDialog.setContentText(prefixLabel);
        textInputDialog.showAndWait().ifPresent(consumer);
    }

    private void prepareProgressBar() {

        VBox vBox = new VBox();
        ProgressBar progressBar = new ProgressBar(.1);

        this.progressBar = progressBar;
        this.progressText = new Text("Converting");

        vBox.getChildren().addAll(
                this.progressText,
                this.progressBar
        );
        this.notifyInfo(vBox);
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

        if (message.equals("")) {
            this.toggleNotification(false);
            return;
        }
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
