package org.openjfx.core;

import java.util.ArrayList;
import java.util.HashMap;

public class Messaging {
    private static Channel message;

    static Channel getInstance() {
        if (message == null) {
            message = new MyChannel();
        }
        return message;
    }
}

class MyChannel implements Channel {

    @Override
    public void postMessage(MessageObject.SubjectEnum subjectEnum, MessageObject messageObject) {
        this.lastMessage.put(subjectEnum, messageObject);

        ArrayList<LambdaInvoke> listeners = this.listeners.get(subjectEnum);

        if (listeners != null) {
            listeners.forEach(callback -> callback.invoke(messageObject));
        }

    }

    @Override
    public Unsub onMessage(MessageObject.SubjectEnum subjectEnum, LambdaInvoke callback, boolean isDiscardHistoryMessage) {
        if (!isDiscardHistoryMessage) {
            MessageObject lastMessageObject = this.lastMessage.get(subjectEnum);
            if (lastMessageObject != null) {
                callback.invoke(lastMessageObject);
            }
        }

        ArrayList<LambdaInvoke> listenersForTheSubject = this.listeners.get(subjectEnum);
        listenersForTheSubject.remove(callback);
        listenersForTheSubject.add(callback);
        this.listeners.put(subjectEnum, listenersForTheSubject);

        return (Unsub) () -> this.listeners.remove(subjectEnum, callback);

    }

}

interface Channel {

    HashMap<MessageObject.SubjectEnum, MessageObject> lastMessage = new HashMap<>();
    HashMap<MessageObject.SubjectEnum, ArrayList<LambdaInvoke>> listeners = new HashMap<>();

    void postMessage(MessageObject.SubjectEnum subjectEnum, MessageObject messageObject);

    Unsub onMessage(MessageObject.SubjectEnum subjectEnum, LambdaInvoke callback, boolean isDiscardHistoryMessage);

}


interface MessageObject {
    enum SenderEnum {
        Anonymous
    }

    enum SubjectEnum {
        ImageIdToShow
    }

    ;
}

interface LambdaInvoke {
    void invoke(Object obj);
}

interface Unsub {
    void unsub();
}