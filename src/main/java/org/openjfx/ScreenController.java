package org.openjfx;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import java.util.HashMap;

public class ScreenController {
    private HashMap<String, Parent> screenMap = new HashMap<>();
    private Scene main;

    public ScreenController(Scene main) {
        this.main = main;
    }

    protected void addScreen(String name, Parent parent){
        screenMap.put(name, parent);
    }

    protected void removeScreen(String name){
        screenMap.remove(name);
    }

    protected void activate(String name){
        main.setRoot( screenMap.get(name) );
    }
}
