package org.openjfx.core;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import javax.imageio.ImageIO;
import java.io.File;

public class ImageUtil {


    public static ImageView getImageViewByFile(File file) {
        return getImageViewByFile(file, "", 100, 100);
    }

    public static ImageView getImageViewByFile(File file, String cssClasses, int imageWidth, int imageHeight) {

        ImageView imageView = new ImageView();
        Image image = getDefaultImage();
        try {
            image = SwingFXUtils.toFXImage(ImageIO.read(file), null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        imageView.getStyleClass().add(cssClasses);

        imageView.setImage(
                image
        );

        configImageView(imageView, imageHeight, imageWidth);

        return imageView;
    }


    public static ImageView configImageView(ImageView imageView, int imageHeight, int imageWidth) {
        imageView.setPreserveRatio(true);
        imageView.setFitHeight(imageHeight);
        imageView.setFitWidth(imageWidth);
        imageView.setSmooth(true);
        return imageView;
    }

    public  static Image getDefaultImage() {
        return new Image(MsIsConstant.PathEnum.ImagePlaceholder.toString());
    }
}
