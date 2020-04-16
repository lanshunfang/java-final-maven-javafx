package org.openjfx;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import org.openjfx.core.*;

import java.io.File;

/**
 * Handle all notifications
 */
public class NotificationController {

    @FXML
    public HBox globalNotificationContainer;
    @FXML
    public TextFlow globalNotificationTextFlowAlert;

    private Channel messaging = Messaging.getInstance();
    ImageState imageState = ImageState.getInstance();

    public ProgressBar progressBar;
    public Text progressText;

    @FXML
    public void initialize() {

        this.initMessaging();
        this.showWelcome();
    }

    /**
     * On close button clicked
     */
    @FXML
    public void onCloseNotificationAction() {
        messaging.postMessage(MessageObject.SubjectEnum.OnCloseNotification, true);
    }

    private void showWelcome() {
        this.notifyInfo("Pick an image(s) to start");
    }

    private void initMessaging() {

        this.listenOnProgressUpdate();
        this.listenCloseNotificationAction();
        this.listenImageConvertingMessage();
    }

    public void toggleNotification(boolean isShow) {
        ImageUtil.setNodeVisibility(this.globalNotificationContainer, isShow);
    }

    /**
     * Notification with info level
     * @param message
     */
    public void notifyInfo(String message) {

        this.notify(message, "alert", "alert-info");

    }

    public void notify(String message, String... styleClasses) {

        if (message.equals("")) {
            this.toggleNotification(false);
            return;
        }
        Text alertMsg = new Text(message);
        this.notify(alertMsg, styleClasses);

    }

    /**
     * Notification with style classes
     * @param node
     * @param styleClasses
     */
    public void notify(Node node, String... styleClasses) {

        this.globalNotificationContainer.getStyleClass().addAll(
                styleClasses
        );

        this.notify(node);

    }

    public void notify(Node node) {

        this.toggleNotification(true);

        this.globalNotificationTextFlowAlert.getChildren().clear();
        this.globalNotificationTextFlowAlert.getChildren().addAll(node);


    }

    public void notifyInfo(Node node) {

        this.notify(node, "alert", "alert-info");

    }

    /**
     * Notification with warn level
     * @param message
     */
    public void notifyWarn(String message) {
        this.notify(message, "alert", "alert-warn");

    }

    public void notifyWarn(Node node) {
        this.notify(node, "alert", "alert-warn");

    }


    /**
     * Progress bar management
     *
     * @param percentage
     * @param prefix - The Label of the progress
     */
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


    private void listenImageConvertingMessage() {

        this.messaging.onMessage(MessageObject.SubjectEnum.ImageConvertingInProgress, (inProgressData) -> {
            ImageUtil.ConvertResult convertResult = (ImageUtil.ConvertResult) inProgressData;
            safeUpdateProgress(convertResult.progress, "Converting");

        });
    }

    /**
     * Generate progress bar
     */
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

    private void listenOnProgressUpdate() {
        messaging.onMessage(MessageObject.SubjectEnum.ProgressUpdate, (loadResult) -> {
            loadResult = (Channel.ProgressData) loadResult;
            safeUpdateProgress(((Channel.ProgressData) loadResult).percentage, ((Channel.ProgressData) loadResult).prefix);

        });
    }

    /**
     * Thread safe for UI update
     * @param progress
     * @param prefix
     */
    private void safeUpdateProgress(double progress, String prefix) {
        Platform.runLater(() -> {
            double displayProgress = progress < 0.03 ? 0.03 : progress;
            this.setProgress(displayProgress, prefix);
        });
    }

    private void listenCloseNotificationAction() {
        messaging.onMessage(MessageObject.SubjectEnum.OnCloseNotification, (data) -> {
            ImageUtil.setNodeVisibility(this.globalNotificationContainer, false);

        });
    }


    /**
     * Show the result
     * @param isAllSuccess - if false, some image converting failed
     * @param outputDirectory - Image saving folder
     */
    public void informResult(boolean isAllSuccess, File outputDirectory) {

        HBox generateContainerHBox = new HBox();
        VBox msgContainerVBox = new VBox();

        generateContainerHBox.setAlignment(Pos.CENTER);

        String savedFolder = outputDirectory.getAbsolutePath();

        Text msgText = new Text(
                isAllSuccess
                        ? "All done. "
                        + (

                        imageState.imageFileList.size() > 1
                                ? imageState.imageFileList.size() + " images(" + imageState.convertFormat + ") are"
                                : imageState.imageFileList.size() + " image(" + imageState.convertFormat + ")  is")
                        + " saved in "
                        : "Some images could not be converted. Please check "
        );

        String filterDisplay = imageState.convertFilterParamsDisplay.size() > 0
                ? "Applied filter(s): " + imageState.convertFilterParamsDisplay.toString()
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
}
