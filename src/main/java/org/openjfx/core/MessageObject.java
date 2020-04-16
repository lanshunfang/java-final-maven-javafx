package org.openjfx.core;

public interface MessageObject {
    enum SenderEnum {
        Anonymous
    }

    enum SubjectEnum {
        ImageIdToShow,
        ImageConvertingInProgress,
        OnPickImages,
        OnAnImageLoaded,
        ProgressUpdate,
        EditConvertStateUpdate,
        OnCloseNotification
    }
}
