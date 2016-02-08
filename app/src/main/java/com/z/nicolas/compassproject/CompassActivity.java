package com.z.nicolas.compassproject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

import com.z.nicolas.compassproject.utils.LowPassFilter;

public class CompassActivity extends Activity implements SensorEventListener, LocationListener {

    private static final int ROTATION_ANIMATION_DURATION = 250;
    public static final int ACCESS_LOCATION_PERMISSIONS_REQUEST = 101;

    private ImageView compassRose;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor magnetometerSensor;

    private LocationManager locationManager;
    private GeomagneticField geomagneticField;

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

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case ACCESS_LOCATION_PERMISSIONS_REQUEST:
                if (0 < grantResults.length && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    listenForLocationUpdates();
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, accelerometerSensor,
                SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magnetometerSensor,
                SensorManager.SENSOR_DELAY_GAME);

        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            listenForLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, ACCESS_LOCATION_PERMISSIONS_REQUEST);
        }
    }

    private void listenForLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // added here to suppress Android Studio warnings, we access this method only if permissions was granted
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60 * 1000, 1000, this);
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (null == lastKnownLocation) {
            lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
        if (null == lastKnownLocation) {
            lastKnownLocation = new Location("DEFAULT");
            lastKnownLocation.setAltitude(100);
            lastKnownLocation.setLatitude(21.00);
            lastKnownLocation.setLongitude(52.13);
        }

        onLocationChanged(lastKnownLocation);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this, accelerometerSensor);
        sensorManager.unregisterListener(this, magnetometerSensor);
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                && PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationManager.removeUpdates(this);
        }
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
        double degrees = Math.toDegrees(orientation[0]);
        if (null != geomagneticField) {
            degrees += geomagneticField.getDeclination();
        }

        return (float) (degrees + 360) % 360;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onLocationChanged(Location location) {
        geomagneticField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
