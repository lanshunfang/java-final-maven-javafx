package org.openjfx.core;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

public class ImageState {

    private Channel messaging = Messaging.getInstance();

    public ArrayList<File> imageFileList = new ArrayList<>();
    public ArrayList<ImageUtil.ImageWrapper> imageWrapperList = new ArrayList<>();

    public int maxImageFiles = 50;

    public boolean isEditing;
    public boolean isConverting;
    public boolean isConvertingInProgress;

    public String convertFormat = "";
    public HashMap<String, ArrayList<String>> convertFilterParams = new HashMap();
    public ArrayList<String> convertFilterParamsDisplay = new ArrayList<>();

    private static ImageState inst;
    public static ImageState getInstance() {
        if (inst == null) {
            inst = new ImageState();
        }
        return inst;
    }

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
