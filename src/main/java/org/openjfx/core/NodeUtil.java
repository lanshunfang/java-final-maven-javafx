package org.openjfx.core;

import javafx.scene.Node;
import javafx.scene.control.Label;


public class NodeUtil {

    public static Node getPaddingNode() {
        Label label = new Label(" ");

        label.getStyleClass().addAll("gap-margin");

        return label;
    }

}
