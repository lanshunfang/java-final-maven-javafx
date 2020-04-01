package org.openjfx.core;

import org.openjfx.App;

import java.net.URL;

public class MsIsConstant {
    public enum ComponentEnum {
        ImageList("ImageList"),
        ImageDetail("ImageDetail");

        public final String resourceName;

        ComponentEnum(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public String toString() {
            return this.resourceName;
        }

    }

    public enum PathEnum {
        StyleCss("style.css"),
        ClosePng("images/close.png"),
        ImagePlaceholder("images/placeholder.png");

        public final String path;

        PathEnum(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return App.class.getResource(this.path).toString();
        }
        public URL toURL() {
            return App.class.getResource(this.path);
        }
    }

    public enum ImageConvertingFormatEnum {
        Jpg("JPG", "jpg"),
        Png("PNG", "png"),
        Gif("GIF", "gif");

        public final String displayValue;
        public final String formatValue;

        ImageConvertingFormatEnum(String displayValue, String formatValue) {
            this.displayValue = displayValue;
            this.formatValue = formatValue;
        }

        @Override
        public String toString() {
            return this.displayValue;
        }

        public String getFormatValue() {
            return this.formatValue;
        }


    }

}
