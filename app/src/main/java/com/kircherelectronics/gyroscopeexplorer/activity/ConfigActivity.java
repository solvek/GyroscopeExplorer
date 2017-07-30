package com.kircherelectronics.gyroscopeexplorer.activity;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.widget.Toast;

import com.kircherelectronics.gyroscopeexplorer.R;

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
 * Configuration activity.
 */
public class ConfigActivity extends PreferenceActivity implements
        OnSharedPreferenceChangeListener {

    public static final String CALIBRATED_GYROSCOPE_ENABLED_KEY = "calibrated_gyroscope_preference";
    public static final String IMUOCF_ORIENTATION_COEFF_KEY = "imuocf_orienation_coeff_preference";
    public static final String IMUOCF_ORIENTATION_ENABLED_KEY = "imuocf_orienation_enabled_preference";
    public static final String IMUOCF_QUATERNION_COEFF_KEY = "imuocf_quaternion_coeff_preference";
    public static final String IMUOCF_QUATERNION_ENABLED_KEY = "imuocf_quaternion_enabled_preference";
    public static final String IMUOCF_ROTATION_MATRIX_COEFF_KEY = "imuocf_rotation_matrix_coeff_preference";
    public static final String IMUOCF_ROTATION_MATRIX_ENABLED_KEY = "imuocf_rotation_matrix_enabled_preference";
    public static final String IMUOKF_QUATERNION_ENABLED_KEY = "imuokf_quaternion_enabled_preference";
    // Preference keys for smoothing filters
    public static final String MEAN_FILTER_SMOOTHING_ENABLED_KEY = "mean_filter_smoothing_enabled_preference";
    public static final String MEAN_FILTER_SMOOTHING_TIME_CONSTANT_KEY =
            "mean_filter_smoothing_time_constant_preference";
    private SwitchPreference spImuOCfOrientation;
    private SwitchPreference spImuOCfQuaternion;
    private SwitchPreference spImuOCfRotationMatrix;
    private SwitchPreference spImuOKfQuaternion;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		/*
         * Read preferences resources available at res/xml/preferences.xml
		 */
        addPreferencesFromResource(R.xml.preferences);

        spImuOCfOrientation = (SwitchPreference) findPreference(IMUOCF_ORIENTATION_ENABLED_KEY);

        spImuOCfRotationMatrix = (SwitchPreference) findPreference(IMUOCF_ROTATION_MATRIX_ENABLED_KEY);

        spImuOCfQuaternion = (SwitchPreference) findPreference(IMUOCF_QUATERNION_ENABLED_KEY);

        spImuOKfQuaternion = (SwitchPreference) findPreference(IMUOKF_QUATERNION_ENABLED_KEY);

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {

        if (key.equals(IMUOCF_ORIENTATION_ENABLED_KEY)) {
            if (sharedPreferences.getBoolean(key, false)) {
                Editor edit = sharedPreferences.edit();

                edit.putBoolean(IMUOCF_ROTATION_MATRIX_ENABLED_KEY, false);
                edit.putBoolean(IMUOCF_QUATERNION_ENABLED_KEY, false);
                edit.putBoolean(IMUOKF_QUATERNION_ENABLED_KEY, false);

                edit.apply();

                spImuOCfRotationMatrix.setChecked(false);
                spImuOCfQuaternion.setChecked(false);
                spImuOKfQuaternion.setChecked(false);
            }
        }

        if (key.equals(IMUOCF_ROTATION_MATRIX_ENABLED_KEY)) {
            if (sharedPreferences.getBoolean(key, false)) {
                Editor edit = sharedPreferences.edit();

                edit.putBoolean(IMUOCF_ORIENTATION_ENABLED_KEY, false);
                edit.putBoolean(IMUOCF_QUATERNION_ENABLED_KEY, false);
                edit.putBoolean(IMUOKF_QUATERNION_ENABLED_KEY, false);

                edit.apply();

                spImuOCfOrientation.setChecked(false);
                spImuOCfQuaternion.setChecked(false);
                spImuOKfQuaternion.setChecked(false);

            }
        }

        if (key.equals(IMUOKF_QUATERNION_ENABLED_KEY)) {
            if (sharedPreferences.getBoolean(key, false)) {
                Editor edit = sharedPreferences.edit();

                edit.putBoolean(IMUOCF_ORIENTATION_ENABLED_KEY, false);
                edit.putBoolean(IMUOCF_ROTATION_MATRIX_ENABLED_KEY, false);
                edit.putBoolean(IMUOCF_QUATERNION_ENABLED_KEY, false);

                edit.apply();

                spImuOCfOrientation.setChecked(false);
                spImuOCfRotationMatrix.setChecked(false);
                spImuOCfQuaternion.setChecked(false);
            }
        }

        if (key.equals(IMUOCF_QUATERNION_ENABLED_KEY)) {
            if (sharedPreferences.getBoolean(key, false)) {
                Editor edit = sharedPreferences.edit();

                edit.putBoolean(IMUOCF_ORIENTATION_ENABLED_KEY, false);
                edit.putBoolean(IMUOCF_ROTATION_MATRIX_ENABLED_KEY, false);
                edit.putBoolean(IMUOKF_QUATERNION_ENABLED_KEY, false);

                edit.apply();

                spImuOCfOrientation.setChecked(false);
                spImuOCfRotationMatrix.setChecked(false);
                spImuOKfQuaternion.setChecked(false);
            }
        }

        if (key.equals(IMUOCF_ORIENTATION_COEFF_KEY)) {
            if (Double.valueOf(sharedPreferences.getString(key, "0.5")) > 1) {
                sharedPreferences.edit().putString(key, "0.5").apply();

                ((EditTextPreference) findPreference(IMUOCF_ORIENTATION_COEFF_KEY))
                        .setText("0.5");

                Toast.makeText(
                        getApplicationContext(),
                        "Whoa! The filter constant must be less than or equal to 1",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (key.equals(IMUOCF_ROTATION_MATRIX_COEFF_KEY)) {
            if (Double.valueOf(sharedPreferences.getString(key, "0.5")) > 1) {
                sharedPreferences.edit().putString(key, "0.5").apply();

                ((EditTextPreference) findPreference(IMUOCF_ROTATION_MATRIX_COEFF_KEY))
                        .setText("0.5");

                Toast.makeText(
                        getApplicationContext(),
                        "Whoa! The filter constant must be less than or equal to 1",
                        Toast.LENGTH_LONG).show();
            }
        }

        if (key.equals(IMUOCF_QUATERNION_COEFF_KEY)) {
            if (Double.valueOf(sharedPreferences.getString(key, "0.5")) > 1) {
                sharedPreferences.edit().putString(key, "0.5").apply();

                ((EditTextPreference) findPreference(IMUOCF_QUATERNION_COEFF_KEY))
                        .setText("0.5");

                Toast.makeText(
                        getApplicationContext(),
                        "Whoa! The filter constant must be less than or equal to 1",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }
}