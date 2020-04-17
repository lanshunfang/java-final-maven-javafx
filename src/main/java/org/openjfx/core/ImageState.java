package org.openjfx.core;

import javafx.scene.layout.StackPane;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class ImageState {

    private Channel messaging = Messaging.getInstance();

    // holding all selected image files
    public ArrayList<File> imageFileList = new ArrayList<>();

    // holding all generated image files and its images
    public ArrayList<ImageUtil.ImageWrapper> imageWrapperList = new ArrayList<>();

    // represent the JavaFX image node (with stackPane)
    public HashMap<File, StackPane> fileImageContainerHashMap = new HashMap<>();

    // max image files allowed
    public int maxImageFiles = 50;

    // Show if the UI is in Editing mode
    public boolean isEditing;
    // Show if the UI is in converting mode
    public boolean isConverting;
    // Show if background converting is on-going
    public boolean isConvertingInProgress;

    // Hold the converting format (*.jpg / *.png, etc.)
    public String convertFormat = "";

    // Hold the filter params to ImageMagic binary (resize, greyscale, etc.)
    public HashMap<String, ArrayList<String>> convertFilterParams = new HashMap();

    // The label display of the filter params
    public ArrayList<String> convertFilterParamsDisplay = new ArrayList<>();

    // singleton
    private static ImageState inst;
    public static ImageState getInstance() {
        if (inst == null) {
            inst = new ImageState();
        }
        return inst;
    }


    // prevent from calling directly, consumer must use getInstance
    private ImageState() {}

    /**
     * Main a local image model to represent the image files
     *
     * @param consumer
     */
    public void updateImageModel(Consumer<List<ImageUtil.ImageWrapper>> consumer) {
        ImageUtil.updateImageListParallel(
                imageFileList,
                imageWrapperList,

                (loadResult) -> {
                    messaging.postMessage(MessageObject.SubjectEnum.ProgressUpdate,
                            new Channel.ProgressData(loadResult.progress, "Loading")
                    );

                    ImageUtil.safeJavaFxExecute((data) -> {
                        messaging.postMessage(MessageObject.SubjectEnum.OnAnImageLoaded, loadResult);

                    });

                },
                (imageWrapperList) -> {
                    messaging.postMessage(MessageObject.SubjectEnum.ProgressUpdate,
                            new Channel.ProgressData(1, "Loaded")
                    );
                    this.imageWrapperList.addAll(imageWrapperList);
                    consumer.accept(imageWrapperList);
                });

    }

}
