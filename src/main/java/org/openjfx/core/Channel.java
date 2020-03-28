package org.openjfx.core;

import java.util.ArrayList;
import java.util.HashMap;

public class Channel implements IChannel {


    @Override
    public void postMessage(MessageObject.SubjectEnum subjectEnum, Object data) {
        if (data == null) {
            this.lastMessage.remove(subjectEnum);
            return;
        }
        this.lastMessage.put(subjectEnum, data);

        ArrayList<LambdaInvoke> listeners = this.listeners.get(subjectEnum);

        if (listeners != null) {
            listeners.forEach(callback -> callback.invoke(data));
        }

    }

    @Override
    public Unsub onMessage(MessageObject.SubjectEnum subjectEnum, LambdaInvoke callback, boolean isDiscardHistoryMessage) {
        if (!isDiscardHistoryMessage) {
            Object lastMessageData = this.lastMessage.get(subjectEnum);
            if (lastMessageData != null) {
                callback.invoke(lastMessageData);
            }
        }

        ArrayList<LambdaInvoke> listenersForTheSubject = this.listeners.get(subjectEnum);
        listenersForTheSubject.remove(callback);
        listenersForTheSubject.add(callback);
        this.listeners.put(subjectEnum, listenersForTheSubject);

        return (Unsub) () -> this.listeners.remove(subjectEnum, callback);

    }

}


interface IChannel {


    HashMap<MessageObject.SubjectEnum, Object> lastMessage = new HashMap<>();
    HashMap<MessageObject.SubjectEnum, ArrayList<LambdaInvoke>> listeners = new HashMap<>();

    void postMessage(MessageObject.SubjectEnum subjectEnum, Object data);

    Unsub onMessage(MessageObject.SubjectEnum subjectEnum, LambdaInvoke callback, boolean isDiscardHistoryMessage);

}