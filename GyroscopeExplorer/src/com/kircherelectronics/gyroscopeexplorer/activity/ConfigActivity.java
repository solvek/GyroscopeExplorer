/*
 * TODO put header
 */
package com.kircherelectronics.gyroscopeexplorer.activity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;

import com.kircherelectronics.com.gyroscopeexplorer.R;

/**
 * Configuration activity.
 */
public class ConfigActivity extends PreferenceActivity implements
		OnPreferenceChangeListener
{

	public static final String FUSION_PREFERENCE = "fusion_preference";
	public static final String UNITS_PREFERENCE = "units_preference";

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		/*
		 * Read preferences resources available at res/xml/preferences.xml
		 */
		addPreferencesFromResource(R.xml.preferences);

	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue)
	{
		return false;
	}
}