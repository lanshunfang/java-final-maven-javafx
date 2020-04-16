package org.openjfx.core;

/**
 * Message channel
 */
public class Messaging {
    private static Channel channel;

    public static Channel getInstance() {
        if (channel == null) {
            channel = new Channel();
        }
        return channel;
    }
}
