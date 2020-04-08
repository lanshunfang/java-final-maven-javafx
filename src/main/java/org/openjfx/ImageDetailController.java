package org.openjfx;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifThumbnailDirectory;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import org.openjfx.core.*;

import java.io.File;

//------------------------
import com.drew.lang.GeoLocation;
import com.drew.metadata.exif.GpsDirectory;
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
    public Label latitude;//命名规范--naming convention
    @FXML
    public Label longitude;

    @FXML
    public void initialize() {
        messaging.onMessage(MessageObject.SubjectEnum.ImageIdToShow, (file) -> {
            //Metadata metadata=new Metadata();//ImageMetadataReader.readMetadata((File)file);
            this.imageViewContainer.getChildren().clear();
            this.imageViewContainer.getChildren().add(
                    ImageUtil.getImageViewByFile((File) file, "", 600, 400)
            );
            Metadata metadata = ImageMetadataReader.readMetadata((File) file);
            //ExifSubIFDDirectory directory=metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            //Date date=directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            ExifThumbnailDirectory thumbnailDirectory = metadata.getFirstDirectoryOfType(ExifThumbnailDirectory.class);
            //System.out.println("Camera owner name = "+thumbnailDirectory.getString(ExifThumbnailDirectory.TAG_CAMERA_OWNER_NAME));
            //System.out.println("make = "+thumbnailDirectory.getString(ExifThumbnailDirectory.TAG_MAKE));



            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            GeoLocation geoLocation = gpsDirectory.getGeoLocation();
            if (geoLocation==null){
                this.latitude.setText("Latitude: null");
                this.longitude.setText("Longitude: null");
            }else{
                double latitude = geoLocation.getLatitude();
                double longitude = geoLocation.getLongitude();
                //if (latitude)
                this.latitude.setText("Latitude: " + String.format("%.2f",latitude));
                this.longitude.setText("Longitude: " + String.format("%.2f",longitude));
                System.out.println("Latitude = " + latitude);
                System.out.println("Longitude = " + longitude);
            }


            //assertEquals(54.989666666666665, geoLocation.getLatitude(),0.001);
            //assertEquals(-1.9141666666666666, geoLocation.getLongitude(),0.001);
        });
    }

 /*   public void readAndDisplayMetadata(File file) {
        try{
            ImageInputStream iis=ImageIO.createImageInputStream(file);
            Iterator<ImageReader> readers=ImageIO.getImageReaders(iis);
            if (readers.hasNext()){
                ImageReader reader=readers.next();
                reader.setInput(iis,true);
                IIOMetadata iioMetadata=reader.getImageMetadata(0);////////////
                String[] names=iioMetadata.getMetadataFormatNames();
                int length=names.length;
                for (int i=0;i<length;i++){
                    System.out.println("Format name:"+ names[i]);
                    displayMetadata(iioMetadata.getAsTree(names[i]));//Node root
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    void displayMetadata(Node root){
        displayMetadata(root,0);
    }
    void indent(int level) {
        for (int i = 0; i < level; i++)
            System.out.print("    ");
    }
    void displayMetadata(Node node, int level) {
        // print open tag of element
        indent(level);
        System.out.print("<" + node.getNodeName());
        NamedNodeMap map = node.getAttributes();
        if (map != null) {
            // print attribute values
            int length = map.getLength();
            for (int i = 0; i < length; i++) {
                Node attr = map.item(i);
                System.out.print(" " + attr.getNodeName() +
                        "=\"" + attr.getNodeValue() + "\"");
            }
        }
        Node child = node.getFirstChild();
        if (child == null) {
            // no children, so close element and return
            System.out.println("/>");
            return;
        }
        // children, so close current tag
        System.out.println(">");
        while (child != null) {
            // print children recursively
            displayMetadata(child, level + 1);
            child = child.getNextSibling();
        }

        // print close tag of element
        indent(level);
        System.out.println("</" + node.getNodeName() + ">");
    }*/
}

