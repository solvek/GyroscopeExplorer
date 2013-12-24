package com.kircherelectronics.gyroscopeexplorer.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.kircherelectronics.com.gyroscopeexplorer.R;
import com.kircherelectronics.gyroscopeexplorer.activity.prefs.HintsPreferences;
import com.kircherelectronics.gyroscopeexplorer.activity.prefs.PreferenceNames;

public class IntroductionActivity extends Activity implements OnClickListener
{

	private final static int INTRODUCTION_1 = 1;
	private final static int INTRODUCTION_2 = 2;
	private final static int INTRODUCTION_3 = 3;
	private final static int INTRODUCTION_FINISHED = 4;

	private boolean hasRun;

	private int introductionCount = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		readHintsPrefs();

		if (!hasRun)
		{
			setContentView(R.layout.introduction_layout);

			Button button = (Button) this.findViewById(R.id.button_confirm);
			button.setOnClickListener(this);
		}
		else
		{
			startGyroscopeActivity();
		}
	}

	private void startGyroscopeActivity()
	{
		hasRun = true;
		
		writeHintsPrefs();

		Intent intent = new Intent();
		intent.setClass(this, GyroscopeActivity.class);
		startActivity(intent);
	}

	private void readHintsPrefs()
	{
		SharedPreferences prefs = getSharedPreferences(PreferenceNames.HINTS,
				Activity.MODE_PRIVATE);

		hasRun = prefs.getBoolean(HintsPreferences.FIRST_RUN_HINTS_ENABLED,
				false);
	}

	/**
	 * Write the user preferences.
	 */
	private void writeHintsPrefs()
	{
		SharedPreferences.Editor editor = getSharedPreferences(
				PreferenceNames.HINTS, Activity.MODE_PRIVATE).edit();
		editor.putBoolean(HintsPreferences.FIRST_RUN_HINTS_ENABLED, hasRun);
		editor.commit();
	}

	@Override
	public void onClick(View arg0)
	{
		introductionCount++;
		ImageView introView = (ImageView) this
				.findViewById(R.id.introduction_image);

		switch (introductionCount)
		{
		case INTRODUCTION_1:
			introView
					.setImageResource(R.drawable.gyroscope_explorer_introduction_1);
			break;
		case INTRODUCTION_2:
			introView
					.setImageResource(R.drawable.gyroscope_explorer_introduction_2);
			break;
		case INTRODUCTION_3:
			introView
					.setImageResource(R.drawable.gyroscope_explorer_introduction_3);
			break;
		case INTRODUCTION_FINISHED:
			startGyroscopeActivity();
			break;
		}
	}
}
