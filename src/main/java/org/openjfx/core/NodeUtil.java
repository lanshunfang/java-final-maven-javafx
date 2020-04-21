package org.openjfx.core;

import javafx.scene.Node;
import javafx.scene.control.Label;

public class NodeUtil {

    public static Node getPaddingNode() {
        Label label = new Label(" ");

        label.getStyleClass().addAll("gap-margin");

        return label;
    }

    /**
     * Show or hide a JavaFX node
     *
     * @param node
     * @param isShow
     */
    public static void setNodeVisibility(Node node, boolean isShow) {
        node.setVisible(isShow);
        node.setManaged(isShow);
    }


}
