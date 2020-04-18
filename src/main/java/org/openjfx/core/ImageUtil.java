package org.openjfx.core;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.concurrent.*;

public class ImageUtil {

    /**
     * Wrap parallel converting artifact with progress
     */
    public static class ConvertResult {
        public ImageWrapper imageWrapper;
        public boolean isDone;

        public double progress = 0;

        public ConvertResult(ImageWrapper imageWrapper, boolean isDone) {
            this.imageWrapper = imageWrapper;
            this.isDone = isDone;
        }
    }

    /**
     * Wrap parallel image loading result with progress
     */
    public static class LoadResult {
        public ImageWrapper imageWrapper;
        public double progress = 0;

        public LoadResult(ImageWrapper imageWrapper) {
            this.imageWrapper = imageWrapper;
        }
    }

    /**
     * Generate ImageView by an image with styleClasses and width / height
     * @param image
     * @param cssClasses
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    public static ImageView getImageViewByImage(Image image, String cssClasses, int imageWidth, int imageHeight) {
        ImageView imageView = new ImageView();
        Image defaultImage = getDefaultImage();

        imageView.getStyleClass().add(cssClasses);

        imageView.setImage(
                image != null ? image : defaultImage
        );

        configImageView(imageView, imageHeight, imageWidth);

        return imageView;

    }


    /**
     * Generate ImageView by file with given spec
     *
     * @param file
     * @param cssClasses
     * @param imageWidth
     * @param imageHeight
     * @return
     */
    public static ImageView getImageViewByFile(File file, String cssClasses, int imageWidth, int imageHeight) {
        Image image = getDefaultImage();

        try {
            image = getImageFromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getImageViewByImage(image, cssClasses, imageWidth, imageHeight);

    }


    /**
     * Config Image view with spec
     *
     * @param imageView
     * @param imageHeight
     * @param imageWidth
     * @return
     */
    public static ImageView configImageView(ImageView imageView, int imageHeight, int imageWidth) {
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(imageHeight);
        imageView.setFitWidth(imageWidth);
        imageView.setSmooth(true);
        return imageView;
    }

    /**
     * Get default image placeholder
     *
     * @return
     */
    public static Image getDefaultImage() {
        return new Image(MsIsConstant.PathEnum.ImagePlaceholder.toString());
    }

    /**
     * Wrap default image into ImageView
     *
     * @return
     */
    public static ImageView getDefaultImageView() {
        return getImageViewByImage(getDefaultImage(), "", 100, 100);
    }

    /**
     * Create images in parallel with progress support in UI
     *
     * - Any generated images would be shown in UI immediately
     *
     * @param imageFiles - All selected image files from file dialog
     * @param cachedImageWrapperList - We need to update existed generated wrapper list
     *                               to reduce computing and page blink
     * @param consumerInProgress - Callback on progress with given generated images from file
     * @param consumerDone - Call back when done
     * @return
     */
    public static Task updateImageListParallel(
            List<File> imageFiles,
            ArrayList<ImageWrapper> cachedImageWrapperList,
            Consumer<LoadResult> consumerInProgress,
            Consumer<List<ImageWrapper>> consumerDone
    ) {

        // run parallel task which will update main UI thread in JavaFX way
        Task javafxTask = new Task<Void>() {
            @Override
            public Void call() {

                try {

                    ArrayList<ImageUtil.LoadResult> counter = new ArrayList<>();

                    double imageLength = imageFiles.size();

                    getForkJoinPool().submit(
                            () -> {
                                List<ImageWrapper> imageWrapperList = IntStream.range(0, imageFiles.size())
                                        .mapToObj(
                                                i -> {
                                                    File currentFile = imageFiles.get(i);

                                                    ImageWrapper imageWrapper = new ImageWrapper(
                                                            i,
                                                            currentFile,

                                                            null
                                                    );

                                                    return new LoadResult(imageWrapper);
                                                }
                                        )

                                        // All generated images would be filtered out to reduce CPU computing
                                        .filter(
                                                loadResult ->
                                                        !cachedImageWrapperList
                                                                .stream()
                                                                .anyMatch(
                                                                        (theImageWrapper)
                                                                                -> theImageWrapper.file == loadResult.imageWrapper.file
                                                                )
                                        )
                                        .parallel()
                                        .peek(
                                                loadResult -> {

                                                    loadResult.imageWrapper.image = getImageThumbnailFromFile(
                                                            loadResult.imageWrapper.file
                                                    );

                                                }
                                        )
                                        // the place to report progress with the generated images
                                        .peek(result -> {
                                            counter.add(result);
                                            result.progress = (double) counter.size() / imageLength;

                                            consumerInProgress.accept(result);

                                        })
                                        .filter(loadResult -> loadResult.imageWrapper.image != null)
                                        .sorted(
                                                Comparator.comparingInt((a) -> a.imageWrapper.index)
                                        )

                                        .map(loadResult -> loadResult.imageWrapper)

                                        .collect(Collectors.toList());

                                // Done call back
                                consumerDone.accept(imageWrapperList);
                            }
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        // make the thread and make it in background to not stop main UI thread
        Thread convertingThread = new Thread(javafxTask);

        convertingThread.setDaemon(true);
        convertingThread.start();

        return javafxTask;

    }

    /**
     * Generate image from file
     * @param file
     * @return
     */
    public static Image getImageFromFile(File file) {
        try {
            return new Image(file.toURI().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Generate image thumbnail from given file
     *
     * @param file
     * @return
     */
    static Image getImageThumbnailFromFile(File file) {
        try {
            return new Image(file.toURI().toString(), 100, 100, true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Wrap a selected file with generated image, index, file
     */
    public static class ImageWrapper {
        // the index of its imageFileList order, used for sorting after parallel generating
        // so that the gridPane could show it in expected order
        public int index;
        public File file;
        public Image image;

        // if set to true, the file is deleted
        // We use this flag to offer user a chance to un delete a file and
        // to reduce page blinking (gridPane children nodes count changes)
        public boolean isMarkedToDelete = false;

        public ImageWrapper(int index, File file, Image image) {
            this.index = index;
            this.image = image;
            this.file = file;
        }

    }

    /**
     * As we update UI while processing images, we need this to prevent children threads from updating main UI thread.
     * @param consumer
     */
    public static void safeJavaFxExecute(Consumer<Boolean> consumer) {
        Platform.runLater(() -> {
            consumer.accept(true);
        });
    }

    /**
     * Maintain an independent fork join pool so that the processing would not be hijacked by other job
     * @return
     */
    private static ForkJoinPool getForkJoinPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        return new ForkJoinPool(cores > 1 ? cores : 2);
    }

    /**
     * Convert images with ImageMagick in parallel
     *
     * @param imageWrapperList - The image wrapper list generated
     * @param outputDirectory - the download folder (to save files)
     * @param imageConvertFormat - JPG? PNG? The selected file format
     * @param imageConvertParams - The filters
     * @param fileSelector - Offer stream selection;
     *                     Say there is no need to convert the image
     *                     when the image is marked as deletion or User click "Back" button.
     * @param consumerInProgress - Progress report
     * @param consumerDone - Completion call back
     * @return
     */
    public static Task convertParallel(
            ArrayList<ImageWrapper> imageWrapperList,
            File outputDirectory,
            String imageConvertFormat,
            HashMap<String, ArrayList<String>> imageConvertParams,
            Predicate<ImageWrapper> fileSelector,
            Consumer<ConvertResult> consumerInProgress,
            Consumer<Boolean> consumerDone
    ) {

        Task javafxTask = new Task<Void>() {
            @Override
            public Void call() {

                boolean isAllDone = false;

                try {


                    ArrayList<ImageUtil.ConvertResult> counter = new ArrayList<>();

                    double imageLength = imageWrapperList.size();

                    isAllDone = getForkJoinPool().submit(
                            () -> imageWrapperList
                                    .stream()
                                    .parallel()
                                    // apply file selector
                                    .filter((imageWrapper) -> fileSelector.test(imageWrapper))

                                    .map(
                                            imageWrapper -> {
                                                ConvertResult result = new ConvertResult(imageWrapper, false);


                                                try {

                                                    // call convert subroutine
                                                    convert(
                                                            imageWrapper.file,
                                                            outputDirectory,
                                                            imageConvertFormat,
                                                            imageConvertParams
                                                    );

                                                    // keep the flag so that we could know if any stream fails (exits)
                                                    result.isDone = true;

                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }

                                                return result;
                                            }
                                    )

                                    .peek(result -> {

                                        counter.add(result);
                                        result.progress = (double) counter.size() / imageLength;

                                        consumerInProgress.accept(result);

                                    })

                                    .allMatch(convertResult -> convertResult.isDone)
                    ).get();

                } catch (Exception e) {
                    e.printStackTrace();
                }

                // if any stream fails, we pass it into completion callback
                Stream.of(isAllDone).forEach(consumerDone);

                return null;

            }
        };


        Thread convertingThread = new Thread(javafxTask);

        convertingThread.setDaemon(true);
        convertingThread.start();

        return javafxTask;
    }

    /**
     * Get time stamp with current time
     * @return
     */
    private static String getTimeStamp() {

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String time = dateFormat.format(now);
//        File dir = new File(time);

        return time;

    }

    /**
     * Create the output child folder with timestamp
     * @param selectedOutputDirectory
     * @param prefix
     * @return
     */
    public static File initOutputFolderWithTimestamp(File selectedOutputDirectory, String prefix) {

        String outputFolderPath = selectedOutputDirectory.getAbsolutePath() + File.separatorChar + prefix + getTimeStamp();

        File directory = new File(outputFolderPath);

        if (!directory.exists()) {
            directory.mkdir();
        }


        return directory;
    }

    /**
     * Call ImageMagick to convert the image file
     * @param inputFile - The image file to convert
     * @param outputDirectory - The directory to save converted images
     * @param format - The image format (e.g. JPG, PNG)
     * @param extraCommandLineParams - For filters, etc. e.g. -threshold 15% -type bilevel
     *
     * @throws Exception
     */
    private static void convert(File inputFile, File outputDirectory, String format, HashMap<String, ArrayList<String>> extraCommandLineParams) throws Exception {

        String fileName = inputFile.getName().split("\\.")[0];

        // generated file path / name
        StringBuilder outputFileStreamBuilder = new StringBuilder(outputDirectory.getAbsolutePath());
        outputFileStreamBuilder.append(File.separatorChar);
        outputFileStreamBuilder.append(fileName);
        outputFileStreamBuilder.append("-converted");
        outputFileStreamBuilder.append(".");
        outputFileStreamBuilder.append(format);

        String outputFile = outputFileStreamBuilder.toString();

        boolean isWindows = System.getProperty("os.name")
                .toLowerCase().startsWith("windows");

        String binName = isWindows ? "magick.exe" : "magick";

        ArrayList<String> commandLine = new ArrayList<>(Arrays.asList(
                binName,
                "convert",
                inputFile.getAbsolutePath()
        ));

        extraCommandLineParams.entrySet().forEach(
                (entry) -> {
                    commandLine.addAll(entry.getValue());
                }
        );

        commandLine.add(outputFile);

        System.out.println("[INFO] Command line to execute");
        System.out.println(commandLine);

        try {

            Process process = Runtime.getRuntime()
                    .exec(
                            Arrays.copyOf(
                                    commandLine.toArray(),
                                    commandLine.size(),
                                    String[].class
                            )
                    );
            System.out.println("ImageMagick Exit status = " + process.waitFor());
        } catch (
                InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Check image file MIME type before show it into gridPane
     *
     * - Files with .jpg, .png extension names are not necessary images. So the file picker would not be sufficient
     * - MIME check is necessary.
     *
     * @param files
     * @return
     */
    public static List<File> filterValidImageFiles(List<File> files) {

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

    /**
     * Show or hide a JavaFX node
     *
     * @param node
     * @param isShow
     */
    public static void setNodeVisibility(Node node, boolean isShow) {
        node.setVisible(isShow);
        node.setManaged(isShow);
    }

    /**
     * Get tooltip node by text
     * @param tooltip
     * @return
     */
    public static Tooltip getTooltip(String tooltip) {
        Tooltip tooltipE = new Tooltip(tooltip);
        tooltipE.getStyleClass().addAll("tooltip-info");
        return tooltipE;
    }

}
