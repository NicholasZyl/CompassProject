package com.z.nicolas.compassproject;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.z.nicolas.compassproject.utils.LowPassFilter;

public class CompassActivity extends Activity implements SensorEventListener {

    private static final int ROTATION_ANIMATION_DURATION = 250;

    private ImageView compassRose;
    
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magnetometerSensor;
    
    private boolean isGravitySet = false;
    private boolean isMagneticFieldSet = false;

    private float[] accelerometerData = new float[3];
    private float[] magnetometerData = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    private float roseRotationDegree = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        compassRose = (ImageView) findViewById(R.id.compass);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometerSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this, accelerometerSensor);
        sensorManager.unregisterListener(this, magnetometerSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                setAccelerometerData(event);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                setMagnetometerData(event);
                break;
        }

        if (isGravitySet && isMagneticFieldSet) {
            rotateRose();
        }
    }

    private void setAccelerometerData(SensorEvent event) {
        accelerometerData = LowPassFilter.filter(event.values.clone(), accelerometerData);
        isGravitySet = true;
    }

    private void setMagnetometerData(SensorEvent event) {
        magnetometerData = LowPassFilter.filter(event.values.clone(), magnetometerData);
        isMagneticFieldSet = true;
    }

    private void rotateRose() {
        float roseTargetRotationDegrees = calculateRoseTargetRotationDegrees();

        RotateAnimation compassRotation = new RotateAnimation(
                roseRotationDegree,
                -roseTargetRotationDegrees,
                Animation.RELATIVE_TO_SELF,
                0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        compassRotation.setDuration(ROTATION_ANIMATION_DURATION);
        compassRotation.setFillAfter(true);

        compassRose.startAnimation(compassRotation);
        roseRotationDegree = -roseTargetRotationDegrees;
    }

    private float calculateRoseTargetRotationDegrees() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerData, magnetometerData);
        SensorManager.getOrientation(rotationMatrix, orientation);

        return (float) (Math.toDegrees(orientation[0]) + 360) % 360;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
