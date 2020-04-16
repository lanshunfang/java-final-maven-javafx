package org.openjfx.core;

public interface MessageObject {
    enum SenderEnum {
        Anonymous
    }

    enum SubjectEnum {
        ImageIdToShow,
        ImageConvertingInProgress,
        OnImageFileListChanged,
        OnImageFileListCleared,
        OnAnImageLoaded,
        ProgressUpdate,
        EditConvertStateUpdate,
        OnCloseNotification
    }
}
