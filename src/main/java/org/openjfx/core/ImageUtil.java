package org.openjfx.core;

import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
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


    public static List<ImageWrapper> getImageList(List<File> imageFiles) {
        return IntStream.range(0, imageFiles.size())
                .parallel()

                .mapToObj(
                        i -> {
                            File currentFile = imageFiles.get(i);

                            BufferedImage bufferedImage = null;
                            try {
                                bufferedImage = ImageIO.read(
                                        currentFile
                                );
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            ImageWrapper imageWrapper = new ImageWrapper(
                                    i,
                                    currentFile,

                                    bufferedImage,
                                    null
                            );

                            return imageWrapper;


                        }
                )

                .filter(imageWrapper -> imageWrapper.bufferedImage != null)

                .map(
                        imageWrapper -> {

                            return new ImageWrapper(
                                    imageWrapper.index,
                                    imageWrapper.file,
                                    imageWrapper.bufferedImage,
                                    getImageFromBufferedImage(imageWrapper.bufferedImage)

                            );


                        }
                )
                .filter(imageWrapper -> imageWrapper.image != null)
                .sorted(
                        Comparator.comparingInt(ImageWrapper::getIndex)
                )

                .collect(Collectors.toList())

                ;
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
            return getImageFromBufferedImage(ImageIO.read(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Future<HashMap<File, Image>>> loadImage(List<File> imageFiles, LambdaInvoke lambdaInvoke) {
        List<Future<HashMap<File, Image>>> results = null;
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        List<ImageLoadingTask> tasks = new ArrayList<>(imageFiles.size());
        for (File file : imageFiles) {
            tasks.add(new ImageLoadingTask(file));
        }
        try {
            results = executorService.invokeAll(tasks);

            results.stream().forEach((imagePromise) -> {

                try {
                    while (!imagePromise.isDone()) {
                        Thread.sleep(300);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    HashMap<File, Image> record = imagePromise.get();
                    lambdaInvoke.invoke(record);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
        executorService.shutdown();

        return results;
    }

    public static class ImageLoadingTask implements Callable<HashMap<File, Image>> {

        private File file;

        public ImageLoadingTask(File file) {
            this.file = file;
        }

        @Override
        public HashMap<File, Image> call() throws Exception {
            HashMap<File, Image> hashMap = new HashMap<>();
            hashMap.put(
                    this.file,
                    getImageFromFile(file)
            );
            return hashMap;


        }

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

        public int getIndex() {
            return this.index;
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

                                                    convert(imageWrapper.file, outputDirectory, imageConvertFormat);

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

    private static void convert(File inputFile, File outputDirectory, String format) throws Exception {

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
        ProcessBuilder processBuilder = new ProcessBuilder();

        String binName = isWindows ? "magick.exe" : "magick";

        processBuilder.command(binName, "convert", inputFile.getAbsolutePath(), outputFile);


        Process process = processBuilder.start();

        BufferedReader reader =
                new BufferedReader(new InputStreamReader(process.getInputStream()));

        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        int exitCode = process.waitFor();
        System.out.println("\nExited with error code : " + exitCode);


//        } catch (Exception e) {
//
//            e.printStackTrace();
//
//        }


    }

}
