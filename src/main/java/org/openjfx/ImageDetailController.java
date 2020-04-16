package org.openjfx;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.openjfx.core.*;

import java.io.File;

import com.drew.lang.GeoLocation;
import com.drew.metadata.exif.GpsDirectory;


public class ImageDetailController {
    //receive messaging of Channel and pass the messaging to the ImageDetail scene
    private Channel messaging = Messaging.getInstance();

    //@FXML annotation inject values defined in an FXML file into references in the controller class
    @FXML
    //function to switch to the ImageList scene
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
    public Label latitude;
    @FXML
    public Label longitude;
    @FXML
    public Label cameraMakeAndModel;

    @FXML
    //use initialize function to get the metadata of the image file and set the metadata properties on the UI display
    public void initialize() {
        //incoming file by the Channel
        messaging.onMessage(MessageObject.SubjectEnum.ImageIdToShow, (file) -> {
            //get image and restrict the maximum width to 400
            Image image = ImageUtil.getImageFromFile((File) file, 400);
            //get imageView and set its width and height
            ImageView imageView = ImageUtil.getImageViewByImage(image, "", 400, 400);
            //clear the HBox imageViewContainer
            this.imageViewContainer.getChildren().clear();
            //add  imageView to the HBox imageViewContainer
            this.imageViewContainer.getChildren().add(
                    imageView
            );
            //set an hgrow constraint on the imageView
            this.imageViewContainer.setHgrow(imageView, Priority.NEVER);
            //get metadata from the image file
            Metadata metadata = ImageMetadataReader.readMetadata((File) file);
            //get the width and height of the image
            double width = image.getWidth();
            double height = image.getHeight();
            //set text of Label width and height on the UI display
            this.width.setText("" + width);
            this.height.setText("" + height);
            //get ExifIFD0Directory from metadata
            ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            //initialize make and model of camera
            String make = "N/A";
            String model = "N/A";
            if (exifIFD0Directory != null) {
                //get the value of make and model properties
                make = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE);
                model = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
                if (make == null || model == null) {
                    make = "N/A";
                    model = "N/A";
                }
            }
            //set text of Label make and model of camera on the UI display
            this.cameraMakeAndModel.setText(String.format("%s, %s", make, model));

            //get GpsDirectory from metadata
            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            //initialize latitude and longitude
            Double latitude = null;
            Double longitude = null;
            //determine if GpsDirectory of metadata exits
            if (gpsDirectory != null) {
                //get the GeoLocation of gpsDirectory if gpsDirectory exits
                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
                if (geoLocation != null) {
                    //get latitude and longitude properties of geoLocation
                    latitude = geoLocation.getLatitude();
                    longitude = geoLocation.getLongitude();
                }
            }
            // set text of Label latitude and longitude on the UI display
            if (latitude != null && longitude != null) {
                //if latitude is positive, then set it as north latitude
                if (latitude >= 0) {
                    this.latitude.setText(String.format("%.2f", latitude) + "° N");
                }
                //if latitude is negative, then set it as south latitude
                else
                    this.latitude.setText(String.format("%.2f", -latitude) + "° S");
                //if longitude is positive, then set it as east longitude
                if (longitude >= 0) {
                    this.longitude.setText(String.format("%.2f", longitude) + "° E");
                }
                //if longitude is negative, then set it as west longitude
                else
                    this.longitude.setText(String.format("%.2f", -longitude) + "° W");
            } else {
                //set the text of Label if latitude and longitude do not exits
                this.latitude.setText("N/A");
                this.longitude.setText("N/A");
            }
        });
    }
}