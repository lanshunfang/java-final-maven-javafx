package org.openjfx.core;

public class MsIsConstant {
    public enum ComponentEnum {
        ImageUpload("ImageUpload"),
        ImageDownload("ImageDownload");

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
        ImagePlaceholder("images/placeholder.css");

        public final String path;

        PathEnum(String path) {
            this.path = path;
        }

        @Override
        public String toString() {
            return this.path;
        }
    }

}
