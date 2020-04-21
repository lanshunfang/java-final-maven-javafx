package org.openjfx;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import org.openjfx.core.*;

import javafx.scene.web.*;

public class ImageGeoMapController {
    //receive messaging of Channel and pass the messaging to the ImageDetail scene
    private Channel messaging = Messaging.getInstance();

    @FXML
    VBox vBox;

    @FXML
    /**
     * switch to the ImageDetail scene
     */
    public void switchToDetailView() {
        // make the webview unload to reduce RAM usage
        getWebView().getEngine().load(null);
        Router.navigateToDetailView();
    }

    private WebView webView;


    @FXML
    /**
     * JavaFX lifecycle
     */
    public void initialize() {
        //incoming file by the Channel
        messaging.onMessage(MessageObject.SubjectEnum.GeoMapToShow, (data) -> {

            Double[] geo = (Double[]) data;
            Double longitudeValue = geo[0];
            Double latitudeValue = geo[1];

            final WebEngine webEngine = getWebView().getEngine();

            // load Google MAP with API
            webEngine.load(
                    "https://maps.googleapis.com/maps/api/staticmap?zoom=13&size=800x600&maptype=roadmap"
                            + "&markers=color:red%7Clabel:Photo%7C"
//                            + "40.711614,-74.012318"
                            + latitudeValue
                            + ","
                            + longitudeValue
                            + "&key=AIzaSyB33sJVcGznZsYXtgbtflyHLrPCBH-Xyag");

            // go back to list on click
            Button goBackBtn = new Button();
            goBackBtn.getStyleClass().addAll("btn", "btn-primary");
            goBackBtn.setText("Back");

            vBox.getChildren().clear();
            vBox.getChildren().addAll(
                    getWebView(),
                    NodeUtil.getPaddingNode(),
                    goBackBtn,
                    NodeUtil.getPaddingNode()
            );

            goBackBtn.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEvent -> {
                switchToDetailView();
            });

        });
    }

    /**
     * Generate webview if null
     * @return
     */
    private WebView getWebView() {
        if (webView == null) {
            webView = new WebView();
        }

        return webView;
    }

}