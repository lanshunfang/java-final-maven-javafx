package org.openjfx.core;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class StyleClass {

    public enum FontStyleEnum{
        FontSmall("font-small"),
        FontItalic("font-italic"),
        FontWhite("font-white");

        private String label;
        FontStyleEnum(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }
    public enum PaddingEnum{
        Padding5("padding-5"),
        Padding10("padding-10");

        private String label;
        PaddingEnum(String label) {
            this.label = label;
        }


        @Override
        public String toString() {
            return this.label;
        }
    }


    public enum NodeClassEnum {
        DeleteButton("delete-image-btn");

        public final String value;

        NodeClassEnum(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

}
