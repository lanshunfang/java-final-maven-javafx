package org.openjfx;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.openjfx.core.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;

//------------------------
import com.drew.lang.GeoLocation;
import com.drew.metadata.exif.GpsDirectory;

import javax.imageio.ImageIO;
//---------------------------
//import javax.imageio.metadata.IIOMetadata;
//import javafx.scene.media.Media.Metadata;
//import jdk.jfr.internal.consumer.ChunkHeader;

public class ImageDetailController {

    private Channel messaging = Messaging.getInstance();
    //private ChunkHeader ImageMetadataReader;

    @FXML
    public void switchToList() {
        Router.navigateToListView();
    }

    @FXML
    public HBox imageViewContainer;
    @FXML
    public Label width;
    @FXML
    public Label height;
    @FXML
    public Label latitude;//命名规范--naming convention
    @FXML
    public Label longitude;
    @FXML
    public Label cameraMakeAndModel;

    @FXML
    public void initialize() {
        messaging.onMessage(MessageObject.SubjectEnum.ImageIdToShow, (file) -> {
            //Metadata metadata=new Metadata();//ImageMetadataReader.readMetadata((File)file);
            //Image image =  ImageUtil.getImageViewByFile()getImageByFile((File) file, "", 600, 400);
            this.imageViewContainer.getChildren().clear();
            this.imageViewContainer.getChildren().add(
                    ImageUtil.getImageViewByFile((File) file, "", 600, 400)
            );
            Metadata metadata = ImageMetadataReader.readMetadata((File) file);

            BufferedImage bimg = ImageIO.read((File) file);

            int width = bimg.getWidth();
            int height = bimg.getHeight();
            this.width.setText(/*"Width: "+*/""+width);
            this.height.setText(/*"Height: "*/""+height);
            //System.out.println(width+", "+height);

            ExifIFD0Directory exifIFD0Directory=metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            String make = "null";
            String model = "null";
            if (exifIFD0Directory!=null){
                make=exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE);
                model=exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
            }
            this.cameraMakeAndModel.setText(String.format("%s, %s", make, model));
            //System.out.println("Camera Make and Model: "+exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE)+" : "+exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL));


            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            Double latitude= null;
            Double longitude = null;//变量作用域

            if (gpsDirectory!=null){//////数码image没进来，直接走到channel catch了
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation!=null){
                    latitude = geoLocation.getLatitude();
                    longitude = geoLocation.getLongitude();
                }
            }
            if (latitude!=null && longitude!=null){
                if (latitude>=0){
                    this.latitude.setText(/*"Latitude: "+*/String.format("%.2f",latitude)+"° N");
                }
                else this.latitude.setText(/*"Latitude: "+*/String.format("%.2f",latitude)+"° S");

                if (longitude>=0){
                    this.longitude.setText(/*"Longitude: "+*/String.format("%.2f",longitude)+"° E");
                }
                else this.longitude.setText(/*"Longitude: "+*/String.format("%.2f",longitude)+"° W");
            }
            else {
                this.latitude.setText("null");
                this.longitude.setText("null");
            }
            /*this.latitude.setText("Latitude: "+(latitude!=null?String.format("%.2f",latitude):"null"));///
            this.longitude.setText("Longitude: "+(longitude!=null?String.format("%.2f",longitude):"null"));///*/
            //1.没有location时没清值2.数码图片没有输出null 3.灯的照片输出null width
            //assertEquals(54.989666666666665, geoLocation.getLatitude(),0.001);
            //assertEquals(-1.9141666666666666, geoLocation.getLongitude(),0.001);
        });
    }
}