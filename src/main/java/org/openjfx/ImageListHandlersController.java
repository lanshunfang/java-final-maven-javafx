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
    private NotificationController notification;

    @FXML
    public HBox defaultActionContainer;

    // when clicked, all images would be cleared from grid
    @FXML
    Button clearAllButton;

    // pick images button
    @FXML
    Button pickButton;

    // by default, the edit buttons would be hidden unless user click Convert or Edit
    @FXML
    public HBox editItemContainer;

    // For now we only support Delete images on editing
    @FXML
    public Button editButton;

    // Bring out convert buttons
    @FXML
    public Button convertButton;

    @FXML
    public HBox convertingWrapper;
    @FXML

    // Go back to List view from Convert mode
    public Button goBackButton;

    // while clicking, a download folder selection dialog would be open
    // once select, the converting starts.
    @FXML
    public Button startConvertButton;

    // select image format like JPG or PNG
    @FXML
    public SplitMenuButton convertFormatSplitMenuButton;

    // ImageMagick filter selection
    @FXML
    public SplitMenuButton convertFilterSplitMenuButton;

    @FXML
    public void initialize() {
        this.initCommunicationInstance();
        this.initMessagingListen();

        this.toggleEditItemWrapper();
        this.updateClearAllButtonState();
        this.setMaxImageFiles();
        this.initConvertTools();

    }

    /**
     * Update the visibility of clear button
     */
    private void updateClearAllButtonState() {
        if (imageState.imageFileList.size() == 0) {
            NodeUtil.setNodeVisibility(this.clearAllButton, false);
        } else {
            NodeUtil.setNodeVisibility(this.clearAllButton, true);
        }
    }

    /**
     * API for parent controller register NotificationController to this child controller
     * @param notificationController
     */
    public void setNotification(NotificationController notificationController) {
        this.notification = notificationController;
    }

    /**
     * Listeners from Messaging init here
     */
    private void initMessagingListen() {
        listenEditConvertStateUpdate();
    }

    /**
     * Init inter component communication
     */
    private void initCommunicationInstance() {
        imageState = ImageState.getInstance();
    }

    /**
     * Tell the pick button the max items allowed
     */
    private void setMaxImageFiles() {
        this.pickButton.setTooltip(
                ImageUtil.getTooltip("Maximum images: " + imageState.maxImageFiles)
        );
    }

    /**
     * Clear all image actions
     *
     * - Once clicked, all states with images would be cleared as well as the grid pane
     * - Default image would be shown
     */
    @FXML
    void onClearImageAction() {
        imageState.imageFileList.clear();
        imageState.fileImageContainerHashMap.clear();
        imageState.imageWrapperList.clear();

        messaging.postMessage(SubjectEnum.OnImageFileListCleared, true);

        this.onImageFileListChanged();
    }

    /**
     * When pick images done, the image list would be updated.
     *
     * For existing images (files selected), their states would be cleared (say marked for deletion)
     * if they are selected again
     * @param event
     */
    @FXML
    public void onPickImageAction(Event event) {

        List<File> files = App.openFileChooser();

        if (files == null) {
            return;
        }

        List<File> validImageFiles = ImageUtil.filterValidImageFiles(files);

        validImageFiles.forEach((file) -> {
            if (imageState.imageFileList.contains(file)) {
                imageState.imageFileList.remove(file);
                imageState.fileImageContainerHashMap.remove(file);
            }

            imageState.imageFileList.add(file);

        });

        imageState.imageWrapperList = imageState.imageWrapperList.stream().filter(
                imageWrapper -> !validImageFiles.contains(imageWrapper.file)
        ).collect(Collectors.toCollection(ArrayList::new));

        this.onImageFileListChanged();

    }

    /**
     * Call back when image file list changed, say added or cleared (clear all)
     */
    private void onImageFileListChanged() {

        // only allow 50 images, all remnant would be removed
        if (imageState.imageFileList.size() > imageState.maxImageFiles) {
            notification.notifyInfo("Only allow 50 images. Auto clear " + (imageState.imageFileList.size() - imageState.maxImageFiles) + " images");
            imageState.imageFileList.subList(0, imageState.maxImageFiles).clear();
        } else {
            notification.notifyInfo(String.format("Loading %s image(s)", imageState.imageWrapperList.size()));
        }

        messaging.postMessage(SubjectEnum.OnImageFileListChanged, true);

        this.updateClearAllButtonState();

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


    /**
     * Toggle editing mode
     *
     * @param event
     */
    @FXML
    public void onEditImageListAction(Event event) {
        imageState.isEditing = !imageState.isEditing;

        if (imageState.isEditing) {
            imageState.isConverting = false;
        }

        messaging.postMessage(SubjectEnum.EditConvertStateUpdate, true);

    }


    /**
     * Toggle converting mode
     * @param event
     */
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
        NodeUtil.setNodeVisibility(this.editItemContainer, isShow);

    }


    private void toggleConvertWrapper() {
        NodeUtil.setNodeVisibility(this.convertingWrapper, imageState.isConverting);
    }

    private void listenEditConvertStateUpdate() {
        messaging.onMessage(SubjectEnum.EditConvertStateUpdate, (data) -> {
            toggleEditState();
            toggleDefaultActionContainer();
            toggleConvertWrapper();
        });
    }

    private void toggleDefaultActionContainer() {
        NodeUtil.setNodeVisibility(this.defaultActionContainer, !imageState.isConverting);

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

    /**
     * Switch back to image list from Editing mode
     */
    private void goBackToList() {
        imageState.isConverting = false;
        imageState.isConvertingInProgress = false;
        this.toggleConvertingState();
    }

    /**
     * Require for user input
     *
     * @param hint - The help tips
     * @param prefixLabel - The label of the input
     * @param defaultValue - The default value in the input
     * @param consumer - callback on done
     */
    private void promptForInput(String hint, String prefixLabel, String defaultValue, Consumer<String> consumer) {
        TextInputDialog textInputDialog = new TextInputDialog(defaultValue);
        textInputDialog.setHeaderText(hint);
        textInputDialog.setTitle("Provide value");
        textInputDialog.setContentText(prefixLabel);
        textInputDialog.showAndWait().ifPresent(consumer);
    }

    /**
     * Manage the converting button state
     */
    private void toggleConvertingState() {

        this.startConvertButton.setText(imageState.isConvertingInProgress ? "Converting" : "Pick download directory");
        this.startConvertButton.setDisable(imageState.isConvertingInProgress || imageState.convertFormat.equals("Select Format"));
        this.convertFormatSplitMenuButton.setDisable(imageState.isConvertingInProgress);
        this.convertFilterSplitMenuButton.setDisable(imageState.isConvertingInProgress);

        messaging.postMessage(SubjectEnum.EditConvertStateUpdate, true);

    }

    /**
     * Consume the filter metadata into UI behavior
     *
     * @param filterOption
     */
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


    /**
     * Action on a filter param is selected
     *
     * @param filterOption
     * @param params
     */
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

    /**
     * Update filter button when any filter is selected
     */
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


    /**
     * Popup format menu
     */
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

    /**
     * Pop up filter menu
     */
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

        NodeUtil.setNodeVisibility(this.startConvertButton, false);


    }

    private void setConvertFormat(String format) {
        imageState.convertFormat = format;
        NodeUtil.setNodeVisibility(this.startConvertButton, true);
    }


    @FXML
    public void onStartConvertImageListAction(Event event) {
        imageState.isConvertingInProgress = true;

        this.startConvert();
    }

    /**
     * Start convert process
     *
     */
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


    /**
     * Complete converting process
     * @param outputDirectory
     * @param isAllSuccess
     */
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
