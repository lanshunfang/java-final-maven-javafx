package org.openjfx;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import org.openjfx.core.*;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Iterator;

//------------------------
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
    public void initialize() {
        messaging.onMessage(MessageObject.SubjectEnum.ImageIdToShow, (file) -> {
            //Metadata metadata=new Metadata();//ImageMetadataReader.readMetadata((File)file);
            readAndDisplayMetadata((File) file);
            this.imageViewContainer.getChildren().clear();
            this.imageViewContainer.getChildren().add(
                    ImageUtil.getImageViewByFile((File) file, "", 600, 400)
            );
        });
    }

    public void readAndDisplayMetadata(File file) {
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
    }
}

