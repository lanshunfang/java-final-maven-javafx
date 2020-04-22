package org.openjfx;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.HashMap;

/**
 * Screen switcher
 */
public class ScreenController {
    private HashMap<String, Parent> screenMap = new HashMap<>();
    private Scene main;
    private Stage stage;

    public ScreenController(Scene main, Stage stage) {
        this.main = main;
        this.stage = stage;
    }

    protected void addScreen(String name, Parent parent){
        screenMap.put(name, parent);
    }

    protected void removeScreen(String name){
        screenMap.remove(name);
    }

    /**
     * Activate the scene
     * @param name
     */
    protected void activate(String name){
        main.setRoot( screenMap.get(name) );
        stage.sizeToScene();

    }
}
