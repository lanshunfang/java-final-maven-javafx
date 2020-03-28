package org.openjfx;

import java.io.File;

import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import org.openjfx.core.*;

public class ImageDetailController {

    private Channel messaging = Messaging.getInstance();

    @FXML
    public void switchToList() {
        Router.navigateToListView();
    }

    @FXML
    public HBox imageViewContainer;

    @FXML
    public void initialize() {
        messaging.onMessage(MessageObject.SubjectEnum.ImageIdToShow, (file) -> {

            this.imageViewContainer.getChildren().clear();
            this.imageViewContainer.getChildren().add(
                    ImageUtil.getImageViewByFile((File) file, "", 600, 400)
            );
        });
    }
}