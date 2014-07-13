package com.wantedbug.cuesense;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

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
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.settings, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
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
