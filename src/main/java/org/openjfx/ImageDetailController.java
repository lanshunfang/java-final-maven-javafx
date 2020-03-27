package org.openjfx;

import java.io.IOException;
import javafx.fxml.FXML;
import org.openjfx.core.MsIsConstant;

public class ImageDetailController {

    @FXML
    private void switchToUpload() throws IOException {
        App.setRoot(MsIsConstant.ComponentEnum.ImageList);
    }
}