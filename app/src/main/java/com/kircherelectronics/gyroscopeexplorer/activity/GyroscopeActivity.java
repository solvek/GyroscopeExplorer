package com.kircherelectronics.gyroscopeexplorer.activity;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.kircherelectronics.fsensor.filter.averaging.MeanFilter;
import com.kircherelectronics.fsensor.filter.fusion.OrientationComplimentaryFusion;
import com.kircherelectronics.fsensor.filter.fusion.OrientationFusion;
import com.kircherelectronics.fsensor.filter.fusion.OrientationKalmanFusion;
import com.kircherelectronics.gyroscopeexplorer.R;


import com.kircherelectronics.gyroscopeexplorer.datalogger.DataLoggerManager;
import  com.kircherelectronics.gyroscopeexplorer.gauge.*;
import com.kircherelectronics.gyroscopeexplorer.view.VectorDrawableButton;

/*
 * Copyright 2013-2017, Kaleb Kircher - Kircher Engineering, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The main activity displays the orientation estimated by the sensor(s) and
 * provides an interface for the user to modify settings, reset or view help.
 *
 * @author Kaleb
 */
public class GyroscopeActivity extends AppCompatActivity implements SensorEventListener {
    private static final String tag = GyroscopeActivity.class.getSimpleName();

    private final static int WRITE_EXTERNAL_STORAGE_REQUEST = 1000;

    // Indicate if the output should be logged to a .csv file
    private boolean logData = false;

    private boolean meanFilterEnabled;
    private boolean kalmanQuaternionEnabled;

    private float[] fusedOrientation = new float[3];
    private float[] acceleration = new float[4];
    private float[] magnetic = new float[3];
    private float[] rotation = new float[3];


    // The gauge views. Note that these are views and UI hogs since they run in
    // the UI thread, not ideal, but easy to use.
    private GaugeBearing gaugeBearingCalibrated;
    private GaugeRotation gaugeTiltCalibrated;

    // Handler for the UI plots so everything plots smoothly
    protected Handler handler;

    protected Runnable runable;

    private TextView tvXAxis;
    private TextView tvYAxis;
    private TextView tvZAxis;

    private OrientationFusion orientationFusion;
    private MeanFilter meanFilter;

    private SensorManager sensorManager;

    private DataLoggerManager dataLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gyroscope);
        dataLogger = new DataLoggerManager(this);
        meanFilter = new MeanFilter();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        initUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.gyroscope, menu);
        return true;
    }

    /**
     * Event Handling for Individual menu item selected Identify single menu
     * item by it's id
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Reset everything
            case R.id.action_reset:
                orientationFusion.reset();
                return true;

            // Reset everything
            case R.id.action_config:
                Intent intent = new Intent();
                intent.setClass(this, ConfigActivity.class);
                startActivity(intent);
                return true;

            // Reset everything
            case R.id.action_help:
                showHelpDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onResume() {
        super.onResume();

        requestPermissions();
        readPrefs();
        reset();

        sensorManager.registerListener(this, sensorManager
                        .getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        // Register for sensor updates.
        sensorManager.registerListener(this, sensorManager
                        .getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);

        // Register for sensor updates.
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);

        handler.post(runable);

        if(kalmanQuaternionEnabled) {
            orientationFusion.startFusion();
        }
    }

    public void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);
        handler.removeCallbacks(runable);

        if(kalmanQuaternionEnabled) {
            orientationFusion.stopFusion();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, acceleration, 0, event.values.length);
            orientationFusion.setAcceleration(acceleration);
        } else  if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, magnetic, 0, event.values.length);
            orientationFusion.setMagneticField(this.magnetic);
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            // Android reuses events, so you probably want a copy
            System.arraycopy(event.values, 0, rotation, 0, event.values.length);
            // Filter the rotation
            fusedOrientation = orientationFusion.filter(this.rotation);

            if(meanFilterEnabled) {
                fusedOrientation = meanFilter.filter(fusedOrientation);
            }

            dataLogger.setRotation(fusedOrientation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}


    private boolean getPrefMeanFilterEnabled() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return prefs.getBoolean(ConfigActivity.MEAN_FILTER_SMOOTHING_ENABLED_KEY,
                false);
    }

    private float getPrefMeanFilterTimeConstant() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return Float.valueOf(prefs.getString(ConfigActivity.MEAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY, "0.5"));
    }

    private boolean getPreKalmanQuaternionEnabled() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return prefs.getBoolean(ConfigActivity.KALMAN_QUATERNION_ENABLED_KEY,
                false);
    }

    private float getPrefImuOCfQuaternionCoeff() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        return Float.valueOf(prefs.getString(
                ConfigActivity.COMPLIMENTARY_QUATERNION_COEFF_KEY, "0.5"));
    }

    private void initStartButton() {
        final VectorDrawableButton button = findViewById(R.id.button_start);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!logData) {
                    button.setText("Stop Log");
                    startDataLog();
                } else {
                    button.setText("Start Log");
                    stopDataLog();
                }
            }
        });
    }

    /**
     * Initialize the UI.
     */
    private void initUI() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Initialize the calibrated text views
        tvXAxis = this.findViewById(R.id.value_x_axis_calibrated);
        tvYAxis = this.findViewById(R.id.value_y_axis_calibrated);
        tvZAxis = this.findViewById(R.id.value_z_axis_calibrated);

        // Initialize the calibrated gauges views
        gaugeBearingCalibrated = findViewById(R.id.gauge_bearing_calibrated);
        gaugeTiltCalibrated = findViewById(R.id.gauge_tilt_calibrated);

        initStartButton();
    }

    private void reset() {

        if (kalmanQuaternionEnabled) {
            orientationFusion = new OrientationKalmanFusion();
        } else {
            orientationFusion = new OrientationComplimentaryFusion();
            orientationFusion.setTimeConstant(getPrefImuOCfQuaternionCoeff());
        }

        handler = new Handler();

        runable = new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(this, 100);
                updateText();
                updateGauges();
            }
        };
    }

    private void readPrefs() {
        meanFilterEnabled = getPrefMeanFilterEnabled();
        kalmanQuaternionEnabled = getPreKalmanQuaternionEnabled();

        if(meanFilterEnabled) {
            meanFilter.setTimeConstant(getPrefMeanFilterTimeConstant());
        }
    }

    private void showHelpDialog() {
        Dialog helpDialog = new Dialog(this);

        helpDialog.setCancelable(true);
        helpDialog.setCanceledOnTouchOutside(true);
        helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = getLayoutInflater()
                .inflate(R.layout.layout_help_home, null);

        helpDialog.setContentView(view);

        helpDialog.show();
    }

    private void startDataLog() {
        logData = true;
        dataLogger.startDataLog();
    }

    private void stopDataLog() {
        logData = false;
        String path = dataLogger.stopDataLog();
        Toast.makeText(this, "File Written to: " + path, Toast.LENGTH_SHORT).show();
    }

    private void updateText() {
        tvXAxis.setText(String.format("%.2f", Math.toDegrees(fusedOrientation[0])));
        tvYAxis.setText(String.format("%.2f", Math.toDegrees(fusedOrientation[1])));
        tvZAxis.setText(String.format("%.2f", Math.toDegrees(fusedOrientation[2])));
    }

    private void updateGauges() {
        gaugeBearingCalibrated.updateBearing(fusedOrientation[0]);
        gaugeTiltCalibrated.updateRotation(fusedOrientation);
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST);
        }
    }

}
