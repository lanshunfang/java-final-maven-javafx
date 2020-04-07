package org.openjfx.core;

import com.drew.imaging.ImageProcessingException;

import java.io.IOException;

public interface LambdaInvoke {
    void invoke(Object obj) throws ImageProcessingException, IOException;
}