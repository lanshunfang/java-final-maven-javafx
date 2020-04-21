package org.openjfx.core;

public interface MessageObject {
    enum SenderEnum {
        Anonymous
    }

    // holding all message channel subjects
    enum SubjectEnum {
        // the image ID selected to show in detail controller
        ImageIdToShow,

        // The Geolocation found on the image to show in the geo map view
        GeoMapToShow,

        // fired when the converting is in progress
        ImageConvertingInProgress,

        // fired when the image file list changed with new file added
        OnImageFileListChanged,

        // fired when the image file list is cleared by clicking Clear button
        OnImageFileListCleared,

        // Fired when an image is loaded in parallel loading tasks
        OnAnImageLoaded,

        // fired on Loading or Converting progress update
        ProgressUpdate,

        // Edit or Convert mode updating
        EditConvertStateUpdate,

        // Fired on click the global notification close icon
        OnCloseNotification
    }
}
