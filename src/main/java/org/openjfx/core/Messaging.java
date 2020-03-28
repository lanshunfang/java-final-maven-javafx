package org.openjfx.core;

import java.util.ArrayList;
import java.util.HashMap;

public class Messaging {
    private static Channel channel;

    public static Channel getInstance() {
        if (channel == null) {
            channel = new Channel();
        }
        return channel;
    }
}


interface Unsub {
    void unsub();
}