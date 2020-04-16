package org.openjfx.core;

import com.drew.imaging.ImageProcessingException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Channel implements IChannel {

    HashMap<MessageObject.SubjectEnum, Object> lastMessage = new HashMap<>();
    HashMap<MessageObject.SubjectEnum, ArrayList<LambdaInvoke>> listeners = new HashMap<>();

    public static class ProgressData {
        public final double percentage;
        public final String prefix;

        public ProgressData(double percentage, String prefix) {
            this.prefix = prefix;
            this.percentage = percentage;
        }
    }

    @Override
    public void postMessage(MessageObject.SubjectEnum subjectEnum, Object data) {
        if (data == null) {
            this.lastMessage.remove(subjectEnum);
            return;
        }
        this.lastMessage.put(subjectEnum, data);

        ArrayList<LambdaInvoke> listeners = this.listeners.get(subjectEnum);

        if (listeners != null) {
            listeners.forEach(callback -> {
                try {
                    callback.invoke(data);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

    }

    @Override
    public Unsub onMessage(MessageObject.SubjectEnum subjectEnum, LambdaInvoke callback, boolean isDiscardHistoryMessage) {
        if (callback == null) {
            return null;
        }
        if (!isDiscardHistoryMessage) {
            Object lastMessageData = this.lastMessage.get(subjectEnum);
            if (lastMessageData != null) {
                try {
                    callback.invoke(lastMessageData);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        ArrayList<LambdaInvoke> listenersForTheSubject = this.listeners.get(subjectEnum);
        if (listenersForTheSubject == null) {
            listenersForTheSubject = new ArrayList<>();
        }
        listenersForTheSubject.remove(callback);
        listenersForTheSubject.add(callback);
        this.listeners.put(subjectEnum, listenersForTheSubject);

        return (Unsub) () -> this.listeners.remove(subjectEnum, callback);

    }

    public Unsub onMessage(MessageObject.SubjectEnum subjectEnum, LambdaInvoke callback) {
        return onMessage(subjectEnum, callback, false);

    }

}


interface IChannel {

    void postMessage(MessageObject.SubjectEnum subjectEnum, Object data);

    Unsub onMessage(MessageObject.SubjectEnum subjectEnum, LambdaInvoke callback, boolean isDiscardHistoryMessage);

}


interface Unsub {
    void unsub();
}