package org.openjfx.core;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

//import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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

    public static class ConvertResult {
        public ImageWrapper imageWrapper;
        public boolean isDone;

        public double progress = 0;

        public ConvertResult(ImageWrapper imageWrapper, boolean isDone) {
            this.imageWrapper = imageWrapper;
            this.isDone = isDone;
        }
    }

    public static class LoadResult {
        public ImageWrapper imageWrapper;
        public double progress = 0;

        public LoadResult(ImageWrapper imageWrapper) {
            this.imageWrapper = imageWrapper;
        }
    }

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

    public static ImageView getImageViewByFile(File file) {
        return getImageViewByFile(file, "", 100, 100);
    }

    public static ImageView getImageViewByFile(File file, String cssClasses, int imageWidth, int imageHeight) {
        Image image = getDefaultImage();

        try {
            image = getImageFromFile(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return getImageViewByImage(image, cssClasses, imageWidth, imageHeight);

    }


    public static ImageView configImageView(ImageView imageView, int imageHeight, int imageWidth) {
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(imageHeight);
        imageView.setFitWidth(imageWidth);
        imageView.setSmooth(true);
        return imageView;
    }

    public static Image getDefaultImage() {
        return new Image(MsIsConstant.PathEnum.ImagePlaceholder.toString());
    }

    public static Image getLoadingSpinner() {
        try {
            FileInputStream inputstream = new FileInputStream(MsIsConstant.PathEnum.LoadingGif.toString());
            return new Image(inputstream);
        } catch (Exception e) {

        }
        return null;

    }


    public static Task updateImageListParallel(
            List<File> imageFiles,
            ArrayList<ImageWrapper> cachedImageWrapperList,
            Consumer<LoadResult> consumerInProgress,
            Consumer<List<ImageWrapper>> consumerDone
    ) {

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

//                                                    BufferedImage bufferedImage = null;
//                                                    try {
//                                                        bufferedImage = ImageIO.read(
//                                                                currentFile
//                                                        );
//                                                    } catch (Exception e) {
//                                                        e.printStackTrace();
//                                                    }

                                                    ImageWrapper imageWrapper = new ImageWrapper(
                                                            i,
                                                            currentFile,

                                                            null,
                                                            null
                                                    );

                                                    return new LoadResult(imageWrapper);


                                                }
                                        )

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
//
//                                                    loadResult.imageWrapper.image = getImageFromBufferedImage(
//                                                            loadResult.imageWrapper.bufferedImage
//                                                    );
                                                    loadResult.imageWrapper.image = getImageThumbnailFromFile(
                                                            loadResult.imageWrapper.file
                                                    );

                                                }
                                        )
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

                                consumerDone.accept(imageWrapperList);
                            }
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }

                return null;
            }
        };

        Thread convertingThread = new Thread(javafxTask);

        convertingThread.setDaemon(true);
        convertingThread.start();

        return javafxTask;

    }

    static Image getImageFromBufferedImage(BufferedImage bufferedImage) {
        WritableImage wr = null;
        if (bufferedImage != null) {
            wr = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                for (int y = 0; y < bufferedImage.getHeight(); y++) {
                    pw.setArgb(x, y, bufferedImage.getRGB(x, y));
                }
            }
        }
        return wr;
    }

    static Image getImageFromFile(File file) {
        try {
//            return getImageFromBufferedImage(ImageIO.read(file));
            return new Image(file.toURI().toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    static Image getImageThumbnailFromFile(File file) {
        try {
//            return getImageFromBufferedImage(ImageIO.read(file));
            return new Image(file.toURI().toString(), 100, 100, true, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static class ImageWrapper {
        public int index;
        public File file;
        public BufferedImage bufferedImage;
        public Image image;

        public boolean isMarkedToDelete = false;

        public ImageWrapper(int index, File file, BufferedImage bufferedImage, Image image) {
            this.index = index;
            this.bufferedImage = bufferedImage;
            this.image = image;
            this.file = file;
        }

    }

    public static void safeJavaFxExecute(Consumer<Boolean> consumer) {
        Platform.runLater(() -> {
            consumer.accept(true);
        });
    }

    private static ForkJoinPool getForkJoinPool() {
        int cores = Runtime.getRuntime().availableProcessors();
        return new ForkJoinPool(cores > 1 ? cores : 2);
    }

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
                                    .filter((imageWrapper) -> fileSelector.test(imageWrapper))

                                    .map(
                                            imageWrapper -> {
                                                ConvertResult result = new ConvertResult(imageWrapper, false);


                                                try {

                                                    convert(
                                                            imageWrapper.file,
                                                            outputDirectory,
                                                            imageConvertFormat,
                                                            imageConvertParams
                                                    );

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

                Stream.of(isAllDone).forEach(consumerDone);

                return null;

            }
        };


        Thread convertingThread = new Thread(javafxTask);

        convertingThread.setDaemon(true);
        convertingThread.start();

        return javafxTask;
    }

    private static String getTimeStamp() {

        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
        String time = dateFormat.format(now);
//        File dir = new File(time);

        return time;

    }

    public static File initOutputFolderWithTimestamp(File selectedOutputDirectory, String prefix) {

        String outputFolderPath = selectedOutputDirectory.getAbsolutePath() + File.separatorChar + prefix + getTimeStamp();

        File directory = new File(outputFolderPath);

        if (!directory.exists()) {
            directory.mkdir();
        }


        return directory;
    }

    /**
     * @param inputFile
     * @param outputDirectory
     * @param format
     * @param extraCommandLineParams - For filters, etc. e.g. -threshold 15% -type bilevel
     * @throws Exception
     */
    private static void convert(File inputFile, File outputDirectory, String format, HashMap<String, ArrayList<String>> extraCommandLineParams) throws Exception {

        String fileName = inputFile.getName().split("\\.")[0];

        StringBuilder outputFileStreamBuilder = new StringBuilder(outputDirectory.getAbsolutePath());
        outputFileStreamBuilder.append(File.separatorChar);
        outputFileStreamBuilder.append(fileName);
        outputFileStreamBuilder.append("-converted");
        outputFileStreamBuilder.append(".");
        outputFileStreamBuilder.append(format);

        String outputFile = outputFileStreamBuilder.toString();

//        try {

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


//        } catch (Exception e) {
//
//            e.printStackTrace();
//
//        }


    }

}
