package com.metaisle.earlybird.app;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.metaisle.earlybird.R;

public class PrefsActivity extends PreferenceActivity {
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		setForceThemeApply(true);
		super.onCreate(savedInstanceState); 
		addPreferencesFromResource(R.xml.prefs);
	}
}