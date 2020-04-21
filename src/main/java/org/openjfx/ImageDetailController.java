package org.openjfx;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.openjfx.core.*;

import java.io.File;
import java.io.IOException;

import com.drew.lang.GeoLocation;
import com.drew.metadata.exif.GpsDirectory;


public class ImageDetailController {
    //receive messaging of Channel and pass the messaging to the ImageDetail scene
    private Channel messaging = Messaging.getInstance();


    @FXML
    Button gotoGeoMapBtn;


    //@FXML annotation inject values defined in an FXML file into references in the controller class
    @FXML
    /**
     * switch to the ImageList scene
     */
    public void switchToList() {
        Router.navigateToListView();
    }

    //@FXML annotation inject values defined in an FXML file into references in the controller class
    @FXML
    /**
     * switch to the GeoMap scene
     */
    public void switchToGeoMap() {
        if (isValidLocationFound()) {
            messaging.postMessage(MessageObject.SubjectEnum.GeoMapToShow, new Double[]{
                    longitudeValue,
                    latitudeValue
            });
        }
        Router.navigateToGeoMapView();
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

    private Double latitudeValue;
    private Double longitudeValue;

    @FXML
    /**
     * use initialize method to get the metadata of the image file and set the metadata properties on the UI display
     */
    public void initialize() {
        //incoming file by the Channel
        messaging.onMessage(MessageObject.SubjectEnum.ImageIdToShow, (file) -> {

            if (this.imageViewContainer == null) {
                return;
            }

            //get image and restrict the maximum width to 400
            Image image = ImageUtil.getImageFromFile((File) file);
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
            Metadata metadata = null;
            try {
                metadata = ImageMetadataReader.readMetadata((File) file);
            } catch (ImageProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //get the width and height of the image
            int width = (int)image.getWidth();
            int height = (int)image.getHeight();
            //set text of Label width and height on the UI display
            this.width.setText("" + width);
            this.height.setText("" + height);
            //get ExifIFD0Directory from metadata
            ExifIFD0Directory exifIFD0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            //initialize make and model of camera
            String NA = MsIsConstant.NAPlaceholder;
            String make = NA;
            String model = NA;
            if (exifIFD0Directory != null) {
                //get the value of make and model properties
                make = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MAKE);
                model = exifIFD0Directory.getString(ExifIFD0Directory.TAG_MODEL);
                if (make == null || model == null) {
                    make = NA;
                    model = NA;
                }
            }
            //set text of Label make and model of camera on the UI display
            this.cameraMakeAndModel.setText(String.format("%s, %s", make, model));

            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            showGeo(gpsDirectory);

        });
    }

    /**
     * Show GEO Locations
     * @param gpsDirectory
     */
    private void showGeo(GpsDirectory gpsDirectory) {
        //get GpsDirectory from metadata
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

        updateGeo(latitude, longitude);

        toggleGoToMapBtn();

        // set text of Label latitude and longitude on the UI display
        String latitudeDisplay = MsIsConstant.NAPlaceholder;
        String longitudeDisplay = MsIsConstant.NAPlaceholder;
        if (isValidLocationFound()) {
            //if latitude is positive, then set it as north latitude
            //if latitude is negative, then set it as south latitude
            latitudeDisplay = latitude >= 0
                    ? String.format("%.2f", latitude) + "° N"
                    : String.format("%.2f", -latitude) + "° S";

            //if longitude is positive, then set it as east longitude
            // vice versa
            longitudeDisplay = longitude >= 0
                    ? String.format("%.2f", longitude) + "° E"
                    : String.format("%.2f", -longitude) + "° W";
        }

        this.latitude.setText(latitudeDisplay);
        this.longitude.setText(longitudeDisplay);

    }

    /**
     * Toggle of Show On Map button on fly
     */
    private void toggleGoToMapBtn() {
        boolean isShow = isValidLocationFound();
        NodeUtil.setNodeVisibility(this.gotoGeoMapBtn, isShow);
    }

    /**
     * Detect if the location is valid
     * @return
     */
    private boolean isValidLocationFound() {
        return latitudeValue != null && longitudeValue != null;
    }

    /**
     * Save GEO location info
     * @param latitude
     * @param longitude
     */
    private void updateGeo(Double latitude, Double longitude) {
        latitudeValue = latitude;
        longitudeValue = longitude;
    }
}