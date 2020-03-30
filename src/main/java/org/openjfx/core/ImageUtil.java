package org.openjfx.core;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//import magick.*;

public class ImageUtil {

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

    public static void convertParallel(ArrayList<File> imageFileList, File outputDirectory, String format) {

        imageFileList.stream().parallel().forEach(
                file -> {
                    convert(file, outputDirectory, format);
                }
        );

    }

    private static void convert(File inputFile, File outputDirectory, String format) {

        String fileName = inputFile.getName().split("\\.")[0];
        StringBuilder outputFileStreamBuilder = new StringBuilder(outputDirectory.getAbsolutePath());
        outputFileStreamBuilder.append(File.separatorChar);
        outputFileStreamBuilder.append(fileName);
        outputFileStreamBuilder.append("-converted");
        outputFileStreamBuilder.append(".");
        outputFileStreamBuilder.append(format);

        String outputFile = outputFileStreamBuilder.toString();

        try {

            boolean isWindows = System.getProperty("os.name")
                    .toLowerCase().startsWith("windows");
            ProcessBuilder processBuilder = new ProcessBuilder();

//            if (!isWindows) {
//                processBuilder.command("magick", "convert", inputFile.getAbsolutePath(), outputFile);
//            }

            processBuilder.command("magick", "convert", inputFile.getAbsolutePath(), outputFile);


            Process process = processBuilder.start();

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            int exitCode = process.waitFor();
            System.out.println("\nExited with error code : " + exitCode);



        } catch (Exception e) {

            e.printStackTrace();

        }


    }

}
