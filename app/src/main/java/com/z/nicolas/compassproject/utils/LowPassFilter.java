package com.z.nicolas.compassproject.utils;

public class LowPassFilter {

    public static final float ALPHA = 0.97f;

    public static float[] filter(float[] input, float[] output) {
        if (null == output) {
            return input;
        }

        for (int i = 0; i < input.length; i++) {
            output[i] = ALPHA * output[i] + (1f - ALPHA) * input[i];
        }

        return output;
    }
}
