package com.kircherelectronics.gyroscopeexplorer.activity;

import java.text.DecimalFormat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.kircherelectronics.com.gyroscopeexplorer.R;
import com.kircherelectronics.gyroscopeexplorer.activity.filter.MeanFilter;
import com.kircherelectronics.gyroscopeexplorer.activity.gauge.GaugeBearing;
import com.kircherelectronics.gyroscopeexplorer.activity.gauge.GaugeRotation;
import com.kircherelectronics.gyroscopeexplorer.activity.prefs.HintsPreferences;
import com.kircherelectronics.gyroscopeexplorer.activity.prefs.PreferenceNames;
import com.kircherelectronics.gyroscopeexplorer.activity.utils.Utils;
import com.kircherelectronics.gyroscopeexplorer.sensor.FusedGyroscopeSensor;
import com.kircherelectronics.gyroscopeexplorer.sensor.listener.FusedGyroscopeSensorListener;

/*
 * Gyroscope Explorer
 * Copyright (C) 2013, Kaleb Kircher - Kircher Engineering, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public class GyroscopeActivity extends Activity implements SensorEventListener,
		FusedGyroscopeSensorListener
{

	public static final float EPSILON = 0.000000001f;

	private static final String tag = GyroscopeActivity.class.getSimpleName();
	private static final float NS2S = 1.0f / 1000000000.0f;
	private static final int MEAN_FILTER_WINDOW = 10;
	private static final int MIN_SAMPLE_COUNT = 30;

	private boolean hasInitialOrientation = false;
	private boolean stateInitializedCalibrated = false;
	private boolean stateInitializedRaw = false;

	private boolean useFusedEstimation = false;
	private boolean useRadianUnits = false;

	// The gauge views. Note that these are views and UI hogs since they run in
	// the UI thread, not ideal, but easy to use.
	private GaugeBearing gaugeBearingCalibrated;
	private GaugeBearing gaugeBearingRaw;
	private GaugeRotation gaugeTiltCalibrated;
	private GaugeRotation gaugeTiltRaw;

	private DecimalFormat df;

	// Calibrated maths.
	private float[] currentRotationMatrixCalibrated;
	private float[] deltaRotationMatrixCalibrated;
	private float[] deltaRotationVectorCalibrated;
	private float[] gyroscopeOrientationCalibrated;

	// Uncalibrated maths
	private float[] currentRotationMatrixRaw;
	private float[] deltaRotationMatrixRaw;
	private float[] deltaRotationVectorRaw;
	private float[] gyroscopeOrientationRaw;

	// accelerometer and magnetometer based rotation matrix
	private float[] initialRotationMatrix;

	// accelerometer vector
	private float[] acceleration;

	// magnetic field vector
	private float[] magnetic;

	private FusedGyroscopeSensor fusedGyroscopeSensor;

	private int accelerationSampleCount = 0;
	private int magneticSampleCount = 0;

	private long timestampOldCalibrated = 0;
	private long timestampOldRaw = 0;

	private MeanFilter accelerationFilter;
	private MeanFilter magneticFilter;

	// We need the SensorManager to register for Sensor Events.
	private SensorManager sensorManager;

	private TextView xAxisRaw;
	private TextView yAxisRaw;
	private TextView zAxisRaw;

	private TextView xAxisCalibrated;
	private TextView yAxisCalibrated;
	private TextView zAxisCalibrated;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_gyroscope);

		initUI();
		initMaths();
		initSensors();
		initFilters();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.gyroscope, menu);
		return true;
	}

	/**
	 * Event Handling for Individual menu item selected Identify single menu
	 * item by it's id
	 * */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{

		// Reset everything
		case R.id.action_reset:
			reset();
			restart();
			return true;

			// Reset everything
		case R.id.action_config:
			Intent intent = new Intent();
			intent.setClass(this, ConfigActivity.class);
			startActivity(intent);
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onResume()
	{
		super.onResume();

		readPrefs();

		restart();
	}

	public void onPause()
	{
		super.onPause();

		reset();
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			onAccelerationSensorChanged(event.values, event.timestamp);
		}

		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
		{
			onMagneticSensorChanged(event.values, event.timestamp);
		}

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
		{
			onGyroscopeSensorChanged(event.values, event.timestamp);
		}

		if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE_UNCALIBRATED)
		{
			onGyroscopeSensorUncalibratedChanged(event.values, event.timestamp);
		}
	}

	@Override
	public void onAngularVelocitySensorChanged(float[] angularVelocity,
			long timeStamp)
	{
		gaugeBearingCalibrated.updateBearing(angularVelocity[0]);
		gaugeTiltCalibrated.updateRotation(angularVelocity);

		TextView status = (TextView) this
				.findViewById(R.id.label_calibrated_status);
		status.setText(R.string.sensor_active);

		int color = getResources().getColor(R.color.light_green);

		status.setTextColor(color);

		if (useRadianUnits)
		{
			xAxisCalibrated.setText(df.format(angularVelocity[0]));
			yAxisCalibrated.setText(df.format(angularVelocity[1]));
			zAxisCalibrated.setText(df.format(angularVelocity[2]));
		}
		else
		{
			xAxisCalibrated.setText(df.format(Math
					.toDegrees(angularVelocity[0])));
			yAxisCalibrated.setText(df.format(Math
					.toDegrees(angularVelocity[1])));
			zAxisCalibrated.setText(df.format(Math
					.toDegrees(angularVelocity[2])));
		}
	}

	public void onAccelerationSensorChanged(float[] acceleration, long timeStamp)
	{
		// Get a local copy of the raw magnetic values from the device sensor.
		System.arraycopy(acceleration, 0, this.acceleration, 0,
				acceleration.length);

		// Use a mean filter to smooth the sensor inputs
		this.acceleration = accelerationFilter.filterFloat(this.acceleration);

		// Count the number of samples received.
		accelerationSampleCount++;

		// Only determine the initial orientation after the acceleration sensor
		// and magnetic sensor have had enough time to be smoothed by the mean
		// filters. Also, only do this if the orientation hasn't already been
		// determined since we only need it once.
		if (accelerationSampleCount > MIN_SAMPLE_COUNT
				&& magneticSampleCount > MIN_SAMPLE_COUNT
				&& !hasInitialOrientation)
		{
			calculateOrientation();
		}
	}

	public void onGyroscopeSensorChanged(float[] gyroscope, long timestamp)
	{
		// don't start until first accelerometer/magnetometer orientation has
		// been acquired
		if (!hasInitialOrientation)
		{
			return;
		}

		// Initialization of the gyroscope based rotation matrix
		if (!stateInitializedCalibrated)
		{
			currentRotationMatrixCalibrated = matrixMultiplication(
					currentRotationMatrixCalibrated, initialRotationMatrix);

			stateInitializedCalibrated = true;

			TextView status = (TextView) this
					.findViewById(R.id.label_calibrated_status);
			status.setText(R.string.sensor_active);

			int color = getResources().getColor(R.color.light_green);

			status.setTextColor(color);
		}

		// This timestep's delta rotation to be multiplied by the current
		// rotation after computing it from the gyro sample data.
		if (timestampOldCalibrated != 0 && stateInitializedCalibrated)
		{
			final float dT = (timestamp - timestampOldCalibrated) * NS2S;

			// Axis of the rotation sample, not normalized yet.
			float axisX = gyroscope[0];
			float axisY = gyroscope[1];
			float axisZ = gyroscope[2];

			// Calculate the angular speed of the sample
			float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY
					* axisY + axisZ * axisZ);

			// Normalize the rotation vector if it's big enough to get the axis
			if (omegaMagnitude > EPSILON)
			{
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the
			// timestep. We will convert this axis-angle representation of the
			// delta rotation into a quaternion before turning it into the
			// rotation matrix.
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;

			float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

			deltaRotationVectorCalibrated[0] = sinThetaOverTwo * axisX;
			deltaRotationVectorCalibrated[1] = sinThetaOverTwo * axisY;
			deltaRotationVectorCalibrated[2] = sinThetaOverTwo * axisZ;
			deltaRotationVectorCalibrated[3] = cosThetaOverTwo;

			SensorManager.getRotationMatrixFromVector(
					deltaRotationMatrixCalibrated,
					deltaRotationVectorCalibrated);

			currentRotationMatrixCalibrated = matrixMultiplication(
					currentRotationMatrixCalibrated,
					deltaRotationMatrixCalibrated);

			SensorManager.getOrientation(currentRotationMatrixCalibrated,
					gyroscopeOrientationCalibrated);
		}

		timestampOldCalibrated = timestamp;

		gaugeBearingCalibrated.updateBearing(gyroscopeOrientationCalibrated[0]);
		gaugeTiltCalibrated.updateRotation(gyroscopeOrientationCalibrated);

		if (useRadianUnits)
		{
			xAxisCalibrated.setText(df
					.format(gyroscopeOrientationCalibrated[0]));
			yAxisCalibrated.setText(df
					.format(gyroscopeOrientationCalibrated[1]));
			zAxisCalibrated.setText(df
					.format(gyroscopeOrientationCalibrated[2]));
		}
		else
		{
			xAxisCalibrated.setText(df.format(Math
					.toDegrees(gyroscopeOrientationCalibrated[0])));
			yAxisCalibrated.setText(df.format(Math
					.toDegrees(gyroscopeOrientationCalibrated[1])));
			zAxisCalibrated.setText(df.format(Math
					.toDegrees(gyroscopeOrientationCalibrated[2])));
		}
	}

	public void onGyroscopeSensorUncalibratedChanged(float[] gyroscope,
			long timestamp)
	{
		// don't start until first accelerometer/magnetometer orientation has
		// been acquired
		if (!hasInitialOrientation)
		{
			return;
		}

		// Initialization of the gyroscope based rotation matrix
		if (!stateInitializedRaw)
		{
			currentRotationMatrixRaw = matrixMultiplication(
					currentRotationMatrixRaw, initialRotationMatrix);

			stateInitializedRaw = true;

			TextView status = (TextView) this
					.findViewById(R.id.label_uncalibrated_status);
			status.setText(R.string.sensor_active);

			int color = getResources().getColor(R.color.light_green);

			status.setTextColor(color);
		}

		// This timestep's delta rotation to be multiplied by the current
		// rotation after computing it from the gyro sample data.
		if (timestampOldRaw != 0 && stateInitializedRaw)
		{
			final float dT = (timestamp - timestampOldRaw) * NS2S;

			// Axis of the rotation sample, not normalized yet.
			float axisX = gyroscope[0];
			float axisY = gyroscope[1];
			float axisZ = gyroscope[2];

			// Calculate the angular speed of the sample
			float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY
					* axisY + axisZ * axisZ);

			// Normalize the rotation vector if it's big enough to get the axis
			if (omegaMagnitude > EPSILON)
			{
				axisX /= omegaMagnitude;
				axisY /= omegaMagnitude;
				axisZ /= omegaMagnitude;
			}

			// Integrate around this axis with the angular speed by the timestep
			// in order to get a delta rotation from this sample over the
			// timestep. We will convert this axis-angle representation of the
			// delta rotation into a quaternion before turning it into the
			// rotation matrix.
			float thetaOverTwo = omegaMagnitude * dT / 2.0f;

			float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
			float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

			deltaRotationVectorRaw[0] = sinThetaOverTwo * axisX;
			deltaRotationVectorRaw[1] = sinThetaOverTwo * axisY;
			deltaRotationVectorRaw[2] = sinThetaOverTwo * axisZ;
			deltaRotationVectorRaw[3] = cosThetaOverTwo;

			SensorManager.getRotationMatrixFromVector(deltaRotationMatrixRaw,
					deltaRotationVectorRaw);

			currentRotationMatrixRaw = matrixMultiplication(
					currentRotationMatrixRaw, deltaRotationMatrixRaw);

			SensorManager.getOrientation(currentRotationMatrixRaw,
					gyroscopeOrientationRaw);
		}

		timestampOldRaw = timestamp;

		gaugeBearingRaw.updateBearing(gyroscopeOrientationRaw[0]);
		gaugeTiltRaw.updateRotation(gyroscopeOrientationRaw);

		if (useRadianUnits)
		{
			xAxisRaw.setText(df.format(gyroscopeOrientationRaw[0]));
			yAxisRaw.setText(df.format(gyroscopeOrientationRaw[1]));
			zAxisRaw.setText(df.format(gyroscopeOrientationRaw[2]));
		}
		else
		{
			xAxisRaw.setText(df.format(Math
					.toDegrees(gyroscopeOrientationRaw[0])));
			yAxisRaw.setText(df.format(Math
					.toDegrees(gyroscopeOrientationRaw[1])));
			zAxisRaw.setText(df.format(Math
					.toDegrees(gyroscopeOrientationRaw[2])));
		}
	}

	public void onMagneticSensorChanged(float[] magnetic, long timeStamp)
	{
		// Get a local copy of the raw magnetic values from the device sensor.
		System.arraycopy(magnetic, 0, this.magnetic, 0, magnetic.length);

		// Use a mean filter to smooth the sensor inputs
		this.magnetic = magneticFilter.filterFloat(this.magnetic);

		// Count the number of samples received.
		magneticSampleCount++;
	}

	/**
	 * Calculates orientation angles from accelerometer and magnetometer output.
	 * Note that we only use this *once* at the beginning to orient the
	 * gyroscope to earth frame. If you do not call this, the gyroscope will
	 * orient itself to whatever the relative orientation the device is in at
	 * the time of initialization.
	 */
	private void calculateOrientation()
	{
		hasInitialOrientation = SensorManager.getRotationMatrix(
				initialRotationMatrix, null, acceleration, magnetic);

		// Remove the sensor observers since they are no longer required.
		if (hasInitialOrientation)
		{
			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));
		}
	}

	/**
	 * Initialize the mean filters.
	 */
	private void initFilters()
	{
		accelerationFilter = new MeanFilter();
		accelerationFilter.setWindowSize(MEAN_FILTER_WINDOW);

		magneticFilter = new MeanFilter();
		magneticFilter.setWindowSize(MEAN_FILTER_WINDOW);
	}

	/**
	 * Initialize the data structures required for the maths.
	 */
	private void initMaths()
	{
		acceleration = new float[3];
		magnetic = new float[3];

		initialRotationMatrix = new float[9];

		deltaRotationVectorCalibrated = new float[4];
		deltaRotationMatrixCalibrated = new float[9];
		currentRotationMatrixCalibrated = new float[9];
		gyroscopeOrientationCalibrated = new float[3];

		// Initialize the current rotation matrix as an identity matrix...
		currentRotationMatrixCalibrated[0] = 1.0f;
		currentRotationMatrixCalibrated[4] = 1.0f;
		currentRotationMatrixCalibrated[8] = 1.0f;

		deltaRotationVectorRaw = new float[4];
		deltaRotationMatrixRaw = new float[9];
		currentRotationMatrixRaw = new float[9];
		gyroscopeOrientationRaw = new float[3];

		// Initialize the current rotation matrix as an identity matrix...
		currentRotationMatrixRaw[0] = 1.0f;
		currentRotationMatrixRaw[4] = 1.0f;
		currentRotationMatrixRaw[8] = 1.0f;
	}

	/**
	 * Initialize the sensors.
	 */
	private void initSensors()
	{
		sensorManager = (SensorManager) this
				.getSystemService(Context.SENSOR_SERVICE);

		fusedGyroscopeSensor = new FusedGyroscopeSensor();
	}

	/**
	 * Initialize the UI.
	 */
	private void initUI()
	{
		// Get a decimal formatter for the text views
		df = new DecimalFormat("#.##");

		// Initialize the raw (uncalibrated) text views
		xAxisRaw = (TextView) this.findViewById(R.id.value_x_axis_raw);
		yAxisRaw = (TextView) this.findViewById(R.id.value_y_axis_raw);
		zAxisRaw = (TextView) this.findViewById(R.id.value_z_axis_raw);

		// Initialize the calibrated text views
		xAxisCalibrated = (TextView) this
				.findViewById(R.id.value_x_axis_calibrated);
		yAxisCalibrated = (TextView) this
				.findViewById(R.id.value_y_axis_calibrated);
		zAxisCalibrated = (TextView) this
				.findViewById(R.id.value_z_axis_calibrated);

		// Initialize the raw (uncalibrated) gauge views
		gaugeBearingRaw = (GaugeBearing) findViewById(R.id.gauge_bearing_raw);
		gaugeTiltRaw = (GaugeRotation) findViewById(R.id.gauge_tilt_raw);

		// Initialize the calibrated gauges views
		gaugeBearingCalibrated = (GaugeBearing) findViewById(R.id.gauge_bearing_calibrated);
		gaugeTiltCalibrated = (GaugeRotation) findViewById(R.id.gauge_tilt_calibrated);
	}

	/**
	 * Multiply matrix a by b. Android gives us matrices results in
	 * one-dimensional arrays instead of two, so instead of using some (O)2 to
	 * transfer to a two-dimensional array and then an (O)3 algorithm to
	 * multiply, we just use a static linear time method.
	 * 
	 * @param a
	 * @param b
	 * @return a*b
	 */
	private float[] matrixMultiplication(float[] a, float[] b)
	{
		float[] result = new float[9];

		result[0] = a[0] * b[0] + a[1] * b[3] + a[2] * b[6];
		result[1] = a[0] * b[1] + a[1] * b[4] + a[2] * b[7];
		result[2] = a[0] * b[2] + a[1] * b[5] + a[2] * b[8];

		result[3] = a[3] * b[0] + a[4] * b[3] + a[5] * b[6];
		result[4] = a[3] * b[1] + a[4] * b[4] + a[5] * b[7];
		result[5] = a[3] * b[2] + a[4] * b[5] + a[5] * b[8];

		result[6] = a[6] * b[0] + a[7] * b[3] + a[8] * b[6];
		result[7] = a[6] * b[1] + a[7] * b[4] + a[8] * b[7];
		result[8] = a[6] * b[2] + a[7] * b[5] + a[8] * b[8];

		return result;
	}

	/**
	 * Restarts all of the sensor observers and resets the activity to the
	 * initial state. This should only be called *after* a call to reset().
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void restart()
	{
		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);

		sensorManager.registerListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
				SensorManager.SENSOR_DELAY_FASTEST);

		// Do not register for gyroscope updates if we are going to use the
		// fused version of the sensor...
		if (!useFusedEstimation)
		{
			boolean enabled = sensorManager.registerListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					SensorManager.SENSOR_DELAY_FASTEST);

			if (!enabled)
			{
				showGyroscopeNotAvailableAlert();
			}
		}

		if (Utils.hasKitKat())
		{
			sensorManager.registerListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED),
					SensorManager.SENSOR_DELAY_FASTEST);
		}

		// If we want to use the fused version of the gyroscope sensor.
		if (useFusedEstimation)
		{
			boolean hasGravity = sensorManager.registerListener(
					fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
					SensorManager.SENSOR_DELAY_FASTEST);

			// If for some reason the gravity sensor does not exist, fall back
			// onto the acceleration sensor.
			if (!hasGravity)
			{
				sensorManager.registerListener(fusedGyroscopeSensor,
						sensorManager
								.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
						SensorManager.SENSOR_DELAY_FASTEST);
			}

			sensorManager.registerListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
					SensorManager.SENSOR_DELAY_FASTEST);

			boolean enabled = sensorManager.registerListener(
					fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
					SensorManager.SENSOR_DELAY_FASTEST);

			if (!enabled)
			{
				showGyroscopeNotAvailableAlert();
			}

			TextView label = (TextView) this
					.findViewById(R.id.label_calibrated_filter_name);
			label.setText("Fused"
					+ getResources().getString(R.string.sensor_calibrated_name));

			fusedGyroscopeSensor.registerObserver(this);
		}
		else
		{
			TextView label = (TextView) this
					.findViewById(R.id.label_calibrated_filter_name);
			label.setText(getResources().getString(
					R.string.sensor_calibrated_name));
		}
	}

	/**
	 * Removes all of the sensor observers and resets the activity to the
	 * initial state.
	 */
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
	private void reset()
	{
		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

		sensorManager.unregisterListener(this,
				sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

		if (!useFusedEstimation)
		{
			sensorManager.unregisterListener(this,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));
		}

		if (Utils.hasKitKat())
		{
			sensorManager.unregisterListener(this, sensorManager
					.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED));
		}

		if (useFusedEstimation)
		{
			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY));

			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));

			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD));

			sensorManager.unregisterListener(fusedGyroscopeSensor,
					sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE));

			fusedGyroscopeSensor.removeObserver(this);
		}

		initMaths();

		accelerationSampleCount = 0;
		magneticSampleCount = 0;

		hasInitialOrientation = false;
		stateInitializedCalibrated = false;
		stateInitializedRaw = false;
	}

	private void readPrefs()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		useFusedEstimation = prefs.getBoolean(ConfigActivity.FUSION_PREFERENCE,
				false);

		useRadianUnits = prefs
				.getBoolean(ConfigActivity.UNITS_PREFERENCE, true);

		Log.d(tag, "Fusion: " + String.valueOf(useFusedEstimation));

		Log.d(tag, "Units Radians: " + String.valueOf(useRadianUnits));
	}

	private void showGyroscopeNotAvailableAlert()
	{
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

		// set title
		alertDialogBuilder.setTitle("Gyroscope Not Available");

		// set dialog message
		alertDialogBuilder
				.setMessage(
						"Your device is not equipped with a gyroscope or it is not responding...")
				.setCancelable(false)
				.setNegativeButton("I'll look around...",
						new DialogInterface.OnClickListener()
						{
							public void onClick(DialogInterface dialog, int id)
							{
								// if this button is clicked, just close
								// the dialog box and do nothing
								dialog.cancel();
							}
						});

		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		// TODO Auto-generated method stub

	}
}
