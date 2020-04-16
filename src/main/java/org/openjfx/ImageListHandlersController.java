package org.openjfx;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import org.openjfx.core.*;
import org.openjfx.core.MessageObject.SubjectEnum;
import org.openjfx.core.MsIsConstant.ImageConvertingFilterEnum;
import org.openjfx.core.MsIsConstant.ImageConvertingFormatEnum;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImageListHandlersController {

    private Channel messaging = Messaging.getInstance();

    ImageState imageState;
    Notification notification;


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

    @FXML
    Button pickButton;

    @FXML
    public void initialize() {
        this.initCommunicationInstance();
        this.initMessagingListen();

        this.toggleEditItemWrapper();
        this.setMaxImageFiles();
        this.initConvertTools();

    }

    private void initMessagingListen() {
        listenEditConvertStateUpdate();
    }

    private void initCommunicationInstance() {
        imageState = ImageState.getInstance();
        notification = Notification.getInstance();
    }

    private void setMaxImageFiles() {
        this.pickButton.setTooltip(
                ImageUtil.getTooltip("Maximum images: " + imageState.maxImageFiles)
        );
    }

    @FXML
    public void onPickImageAction(Event event) {

        List<File> files = App.openFileChooser();

        if (files == null) {
            return;
        }

        imageState.imageFileList.addAll(ImageUtil.filterValidImageFiles(files));

        if (imageState.imageFileList.size() > imageState.maxImageFiles) {
            notification.notifyInfo("Only allow 50 images. Auto clear " + (imageState.imageFileList.size() - imageState.maxImageFiles) + " images");
            imageState.imageFileList.subList(0, imageState.maxImageFiles).clear();
        } else {
            notification.notifyInfo(String.format("Loading %s image(s)", imageState.imageWrapperList.size()));
        }

        messaging.postMessage(SubjectEnum.OnPickImages, true);

        imageState.updateImageModel(
                (imageWrapperList) -> {
                    ImageUtil.safeJavaFxExecute((data) -> {
//                        this.repaintImageList();
                        notification.notifyInfo(String.format("%s new image(s) loaded", imageWrapperList.size()));
                        this.toggleEditItemWrapper();

                    });

                }
        );

    }


    @FXML
    public void onEditImageListAction(Event event) {
        imageState.isEditing = !imageState.isEditing;

        if (imageState.isEditing) {
            imageState.isConverting = false;
        }

        messaging.postMessage(SubjectEnum.EditConvertStateUpdate, true);

    }


    @FXML
    public void onConvertImageListAction(Event event) {
        imageState.isConverting = !imageState.isConverting;

        if (imageState.isConverting) {
            imageState.isEditing = false;
            notification.notifyInfo("Select the image format to convert to");
        }

        messaging.postMessage(SubjectEnum.EditConvertStateUpdate, true);

    }


    private void toggleEditItemWrapper() {
        boolean isShow = imageState.imageFileList.size() > 0;
        ImageUtil.setNodeVisibility(this.editItemContainer, isShow);

    }


    private void toggleConvertWrapper() {
        ImageUtil.setNodeVisibility(this.convertingWrapper, imageState.isConverting);
    }

    private void listenEditConvertStateUpdate() {
        messaging.onMessage(SubjectEnum.EditConvertStateUpdate, (data) -> {
            toggleEditState();
            toggleDefaultActionContainer();
            toggleConvertWrapper();
        });
    }

    private void toggleDefaultActionContainer() {
        ImageUtil.setNodeVisibility(this.defaultActionContainer, !imageState.isConverting);

    }

    private void toggleEditState() {
        if (imageState.isEditing) {
            this.editButton.setText("Done Editing");
        } else {
            this.editButton.setText("Edit");
        }
    }


    @FXML
    public void onGoBackImageListAction(Event event) {
        this.goBackToList();
    }

    private void goBackToList() {
        imageState.isConverting = false;
        imageState.isConvertingInProgress = false;
        this.toggleConvertingState();
    }

    private void promptForInput(String hint, String prefixLabel, String defaultValue, Consumer<String> consumer) {
        TextInputDialog textInputDialog = new TextInputDialog(defaultValue);
        textInputDialog.setHeaderText(hint);
        textInputDialog.setTitle("Provide value");
        textInputDialog.setContentText(prefixLabel);
        textInputDialog.showAndWait().ifPresent(consumer);
    }

    private void toggleConvertingState() {

        this.startConvertButton.setText(imageState.isConvertingInProgress ? "Converting" : "Pick download directory");
        this.startConvertButton.setDisable(imageState.isConvertingInProgress || imageState.convertFormat.equals("Select Format"));
        this.convertFormatSplitMenuButton.setDisable(imageState.isConvertingInProgress);
        this.convertFilterSplitMenuButton.setDisable(imageState.isConvertingInProgress);

        messaging.postMessage(SubjectEnum.EditConvertStateUpdate, true);

    }

    private void setConvertFilter(ImageConvertingFilterEnum filterOption) {
        switch (filterOption) {

            case ClearAll: {
                imageState.convertFilterParams.clear();
                imageState.convertFilterParamsDisplay.clear();
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
        if (imageState.convertFilterParams.containsKey(filterOption.displayValue)) {
            imageState.convertFilterParams.remove(filterOption.displayValue);
            return;
        }

        imageState.convertFilterParams.put(
                filterOption.displayValue,
                new ArrayList<>(
                        Arrays.asList(
                                params.split(" ")
                        )
                )
        );

    }

    private void setFilterButtonTextAndTooltip() {

        imageState.convertFilterParamsDisplay.clear();

        imageState.convertFilterParams.entrySet().forEach(
                (entry) -> {

                    imageState.convertFilterParamsDisplay.add(
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
                imageState.convertFilterParamsDisplay.size()
        );

        this.convertFilterSplitMenuButton.setText(displayOnButton);

        imageState.convertFilterParamsDisplay.forEach(
                (item) -> {
                    stringJoiner.add(item);
                }
        );
        this.convertFilterSplitMenuButton.setTooltip(
                ImageUtil.getTooltip(
                        stringJoiner.toString()
                )
        );
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
                                notification.notifyInfo("Pick a download directory");
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

        //this.toggleConvertMode();
        messaging.postMessage(SubjectEnum.EditConvertStateUpdate, true);
//        this.toggleImageListOpacity();

        this.startConvertButton.setTooltip(
                ImageUtil.getTooltip("Start to convert")
        );

        ImageUtil.setNodeVisibility(this.startConvertButton, false);


    }

    private void setConvertFormat(String format) {
        imageState.convertFormat = format;
        ImageUtil.setNodeVisibility(this.startConvertButton, true);
    }


    @FXML
    public void onStartConvertImageListAction(Event event) {
        imageState.isConvertingInProgress = true;

        this.startConvert();
    }

    private void startConvert() {
        File outputDirectory = App.openDirectoryChooser();
        if (outputDirectory == null) {
            return;
        }

        final File outputDirectoryUniq = ImageUtil.initOutputFolderWithTimestamp(outputDirectory, "converted-");

        this.toggleConvertingState();

        messaging.postMessage(MessageObject.SubjectEnum.ProgressUpdate,
                new Channel.ProgressData(0.05, "Converting")
        );

//        safeUpdateProgress(0.05, "Converting");

        ImageUtil.convertParallel(
//                this.imageFileList,
                imageState.imageWrapperList,
                outputDirectoryUniq,
                imageState.convertFormat,
                imageState.convertFilterParams,
                (imageWrapper) -> imageState.isConvertingInProgress && !imageWrapper.isMarkedToDelete,
                (inProgressData) -> {
                    this.messaging.postMessage(SubjectEnum.ImageConvertingInProgress, inProgressData);
                },
                (isAllSuccess) -> {
                    this.finishConvert(outputDirectoryUniq, isAllSuccess);
                }
        );


    }


    private void finishConvert(File outputDirectory, boolean isAllSuccess) {

        messaging.postMessage(MessageObject.SubjectEnum.ProgressUpdate,
                new Channel.ProgressData(1, "Done")
        );

        ImageUtil.safeJavaFxExecute((data) -> {
            imageState.isConvertingInProgress = false;
            this.toggleConvertingState();

            notification.informResult(isAllSuccess, outputDirectory);

            this.goBackToList();
        });


    }
}
