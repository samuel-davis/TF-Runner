package com.davis.tensorflow.inception;

import com.davis.tensorflow.utils.NativeUtils;

import java.io.IOException;

/**
 * This software was created for
 * rights to this software belong to
 * appropriate licenses and restrictions apply.
 *
 * @author Samuel Davis created on 8/7/17.
 */
public class InceptionRunner {
    static{
        try {
            NativeUtils.loadLibraryFromJar("/linux-x86_64/libtensorflow_jni.so");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
