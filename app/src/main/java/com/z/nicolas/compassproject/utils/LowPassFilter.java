package com.z.nicolas.compassproject.utils;

public class LowPassFilter {

    public static final float ALPHA = 0.2f;

    public static float[] filter(float[] input, float[] output) {
        if (null == output) {
            return input;
        }

        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }

        return output;
    }
}
