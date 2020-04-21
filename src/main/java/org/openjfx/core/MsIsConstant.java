package org.openjfx.core;

import org.openjfx.App;

import java.net.URL;

public class MsIsConstant {

    public static String NAPlaceholder = "N/A";

    // All components loaded into Scene
    public enum ComponentEnum {
        ImageList("ImageList"),
        ImageDetail("ImageDetail"),
        ImageGeoMap("ImageGeoMap");

        public final String resourceName;

        ComponentEnum(String resourceName) {
            this.resourceName = resourceName;
        }

        @Override
        public String toString() {
            return this.resourceName;
        }

    }

    /**
     * All paths we used
     */
    public enum PathEnum {
        StyleCss("style.css"),
        ClosePng("images/close.png"),
        LoadingGif("images/loading.gif"),
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

    /**
     * Image converting options
     */
    public enum ImageConvertingFormatEnum {
        Jpg("JPG", new String[]{"jpg", "jpeg"}),
        Png("PNG", new String[]{"png"}),
        Gif("GIF", new String[]{"gif"});

        public final String displayValue;
        public final String[] formatValues;

        ImageConvertingFormatEnum(String displayValue, String[] formatValues) {
            this.displayValue = displayValue;
            this.formatValues = formatValues;
        }

        @Override
        public String toString() {
            return this.displayValue;
        }

    }

    /**
     * Extensible ImageMagic filtering
     *
     * @link https://imagemagick.org/script/convert.php
     */
    public enum ImageConvertingFilterEnum {
        MonoColor(
                "Grey Scale",
                "-grayscale average",
                false,
                "",
                "Grey Scale in black and white"
        ),
        Resize(
                "Resize",
                "-resize %s",
                true,
                "75%",
                "New dimension scale in percentage"
        ),
        Blur(
                "Blur",
                "-blur %s",
                true,
                "20x5",
                "Simple Gaussian blur with {radius}x{sigma}"
        ),
        Contrast(
                "Add Contrast",
                "-contrast",
                false,
                "",
                "Add contract to the photo"
        ),
        Rotate(
                "Rotate",
                "-rotate %s",
                true,
                "90",
                "Rotate the image by the giving degrees, say 90, -90."
        ),
        Custom(
                "Custom params",
                "%s",
                true,
                "",
                "Enter custom parameters from Image magick https://imagemagick.org/script/convert.php"
        ),
        ClearAll(
                "Clear filters",
                "ClearAll",
                false,
                "",
                "Clear all added filters"
        );

        public final String displayValue;
        public final String value;
        public final boolean isAccept1Param;
        public final String defaultParamValue;
        public final String hint;

        ImageConvertingFilterEnum(
                String displayValue,
                String value,
                boolean isAccept1Param,
                String defaultParamValue,
                String hint

        ) {
            this.displayValue = displayValue;
            this.value = value;
            this.isAccept1Param = isAccept1Param;
            this.defaultParamValue = defaultParamValue;
            this.hint = hint;
        }

        @Override
        public String toString() {
            return this.displayValue;
        }

        public String getValue() {
            return this.value;
        }


    }


}
