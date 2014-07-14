/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.Locale;
import java.util.UUID;

import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.ActionBar;
//import android.app.Fragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
//import android.app.FragmentManager;
import android.support.v4.app.FragmentManager;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
//import android.support.v4.app.FragmentTransaction;
//import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


/**
 * This class is the starting point for the application. 
 * @author vikasprabhu
 */
public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
	
	// Debugging
	private static final String TAG = "MainActivity";

	/**
	 * Constants
	 */
	// Tab content identifiers
	public enum Tabs {
		TAB_CUESENSE(0),
		TAB_FACEBOOK(1),
		TAB_TWITTER(2),
		TAB_SENTINEL(3);
		
		private final int value;
		
		private Tabs(int val) { this.value = val; }
		public int value() { return this.value; }
		
		public static Tabs toTabs(int val) {
			Tabs ret = null;
		    for (Tabs temp : Tabs.values()) {
		        if(temp.value() == val)  {
		        	ret = temp;
		            break;
		        }
		    }
		    return ret;
		}
	}

	// Section number fragment argument for a particular fragment.
	private static final String ARG_SECTION_NUMBER = "section_number";

	/**
	 * Members
	 */
	// Intent request codes
	private static final int REQUEST_ENABLE_BT = 1;	

	// Members
	private BluetoothAdapter mBTAdapter = null;
	private BluetoothManager mBTManager = null;

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a {@link FragmentPagerAdapter}
	 * derivative, which will keep every loaded fragment in memory. If this
	 * becomes too memory intensive, it may be best to switch to a
	 * {@link android.support.v13.app.FragmentStatePagerAdapter}.
	 */
	private SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/** InfoPool instantiation */
//		InfoPool pool = InfoPool.INSTANCE;

		/** Bluetooth setup */
		// Get the default Bluetooth adapter
	    mBTAdapter = BluetoothAdapter.getDefaultAdapter();
	    
	    // If the adapter is null, then Bluetooth is not supported
        if (mBTAdapter == null) {
        	Log.e(TAG, "BT not available");
            Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
		/** Action bar and tabs setup */
		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the activity.
//		mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager
				.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
					@Override
					public void onPageSelected(int position) {
						actionBar.setSelectedNavigationItem(position);
					}
				});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			Tab newTab = actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this);
			switch(i) {
			case 0: newTab.setIcon(R.drawable.fb_29); break;
			case 1: newTab.setIcon(R.drawable.fb_29); break;
			}
			actionBar.addTab(newTab);
		}
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
        Log.d(TAG, "onStart()");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBTAdapter.isEnabled()) {
        	Log.i(TAG, "BT not enabled. Enabling..");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
        	Log.i(TAG, "BT enabled. Setting up link..");
            if (mBTManager == null) setupBTLink();
        }
	}
	
	@Override
    public synchronized void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBTManager != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBTManager.getState() == BluetoothManager.STATE_NONE) {
              // Start the Bluetooth chat services
              mBTManager.start();
            }
        }
    }
	
	/**
	 * Connect to the Bluetooth device
	 */
	private void connectDevice()
	{
		Log.d(TAG, "connectDevice");
        // Get the BluetoothDevice object
        BluetoothDevice device = mBTAdapter.getRemoteDevice(BluetoothManager.DEVICE_MAC);
        // Attempt to connect to the device
        mBTManager.connect(device);
    }
	
	/**
	 * Setup BluetoothManager and UI element links
	 */
	private void setupBTLink()
	{
        Log.d(TAG, "setupBTLink()");

//        // Initialize the compose field with a listener for the return key
//        mEditText = (EditText) findViewById(R.id.edit_text_out);
//
//        // Initialize the send button with a listener that for click events
//        mSendButton = (Button) findViewById(R.id.button_send);
//        mSendButton.setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//                // Send a message using content of the edit text widget
//                TextView view = (TextView) findViewById(R.id.edit_text_out);
//                String message = view.getText().toString();
//                sendToBT(message);
//			}
//		});

        // Initialize BluetoothManager
        mBTManager = new BluetoothManager(this);

        // Initialize the buffer for outgoing messages
//        mOutStringBuffer = new StringBuffer("");
        
        // Connect to the Bluetooth device
        connectDevice();
    }
	
//	/**
//	 * Sends text from the TextView to the Bluetooth device
//	 * @param text
//	 */
//	private void sendToBT(String text)
//	{
//        // Check that we're actually connected before trying anything
//        if (mBTManager.getState() != BluetoothManager.STATE_CONNECTED) {
//            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Check that there's actually something to send
//        if (text.length() > 0) {
//            mBTManager.write(text);
//
//            // Reset out string buffer to zero and clear the edit text field
//            mOutStringBuffer.setLength(0);
//            mEditText.setText(mOutStringBuffer);
//        }
//    }
	
	/**
	 *  This routine is called when an activity completes.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.d(TAG, "onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                setupBTLink();
            } else {
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
                finish();
            }
            break;
		}
	}
	
	@Override
    public void onDestroy()
    {
		Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mBTManager != null) mBTManager.stop();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(settingsIntent);
//			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
			FragmentTransaction fragmentTransaction) {
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			switch(position) {
//			case 0: // TODO - CueSense profile tab
//			case 1:
//				if(mFBFragment == null) {
//					mFBFragment = new FBFragment();
//					Bundle args = new Bundle();
//					args.putInt(ARG_SECTION_NUMBER, position + 1);
//					mFBFragment.setArguments(args);
//				}
//				return mFBFragment;
//			case 2: // TODO - Twitter tab
//			case 3: // TODO - Foursquare tab
			default: return PlaceholderFragment.newInstance(position + 1);
			}
		}

		@Override
		public int getCount() {
			return Tabs.TAB_SENTINEL.value();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (Tabs.toTabs(position)) {
			case TAB_CUESENSE:
				return getString(R.string.title_section1);
			case TAB_FACEBOOK:
				return getString(R.string.title_section2);
			case TAB_TWITTER:
				return getString(R.string.title_section3);
			case TAB_SENTINEL:
				return getString(R.string.title_section3);
			}
			return null;
		}
	}

	// TODO: remove this when tabs are done
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {
		/**
		 * Returns a new instance of this fragment for the given section number.
		 */
		public static PlaceholderFragment newInstance(int sectionNumber) {
			PlaceholderFragment fragment = new PlaceholderFragment();
			Bundle args = new Bundle();
			args.putInt(ARG_SECTION_NUMBER, sectionNumber);
			fragment.setArguments(args);
			return fragment;
		}

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
