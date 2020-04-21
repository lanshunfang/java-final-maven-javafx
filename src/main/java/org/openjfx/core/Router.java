package org.openjfx.core;

import org.openjfx.App;

/**
 * Main routing handler to switch scenes
 */
public class Router {
    public static void navigateToDetailView() {
        App.setRoot(MsIsConstant.ComponentEnum.ImageDetail);
    }
    public static void navigateToGeoMapView() {
        App.setRoot(MsIsConstant.ComponentEnum.ImageGeoMap);
    }
    public static void navigateToListView() {
        App.setRoot(MsIsConstant.ComponentEnum.ImageList);
    }
}
