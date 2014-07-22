/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

/**
 * This class is for the Settings view 
 * @author vikasprabhu
 */
public class SettingsActivity extends FragmentActivity {

	// Debugging
	private static final String TAG = "SettingsActivity";
	
	/**
	 * Members
	 */
	// Fragment that holds Facebook login button
	FBFragment mFBFragment;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		if (savedInstanceState == null) {
	        // Add the fragment on initial activity setup
	        mFBFragment = new FBFragment();
	        getSupportFragmentManager().beginTransaction().add(android.R.id.content, mFBFragment).commit();
	    } else {
	        // Or set the fragment from restored state info
	        mFBFragment = (FBFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
	    }
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause()");
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy()");
		super.onDestroy();
	}
}
