/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import com.wantedbug.cuesense.CueSenseListFragment.CueSenseListener;
import com.wantedbug.cuesense.DeleteCueSenseItemDialog.DeleteCueSenseItemListener;
import com.wantedbug.cuesense.NewCueSenseItemDialog.NewCueSenseItemListener;

import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.ActionBar;
import android.support.v4.app.DialogFragment;
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
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


/**
 * This class is the starting point for the CueSense application. 
 * @author vikasprabhu
 */
public class MainActivity extends FragmentActivity implements ActionBar.TabListener,
		CueSenseListener, NewCueSenseItemListener, DeleteCueSenseItemListener {
	// Debugging
	private static final String TAG = "MainActivity";

	/**
	 * Constants
	 */
	// Time interval to keep trying to push messages to the wearable device
	private static final int PUSH_INTERVAL_MS = 5000;
	// Tab content identifiers
	public enum InfoType {
		INFO_CUESENSE(0),
		INFO_FACEBOOK(1),
		INFO_TWITTER(2),
		INFO_SENTINEL(3);
		
		private final int value;
		
		private InfoType(int val) { this.value = val; }
		public int value() { return this.value; }
		
		public static InfoType toInfoType(int val) {
			InfoType ret = null;
		    for (InfoType temp : InfoType.values()) {
		        if(temp.value() == val)  {
		        	ret = temp;
		            break;
		        }
		    }
		    return ret;
		}
	}

	// Section number fragment argument for a particular fragment.
	private static final String ARG_TAB_NUMBER = "tab_number";
	
	// Types of messages that can be handled from the BTManager
	public static final int BT_MSG_TOAST = 1;
	// Key message names received from BTManager
	public static final String BT_MSG_ERROR = "error";
	// Error message values
	public static final int BT_ERR_CONN_LOST = 1;
	public static final int BT_ERR_CONN_FAILED = 2;

	/**
	 * Members
	 */
	// Intent request codes
	private static final int REQUEST_ENABLE_BT = 1;	

	// Members
	private BluetoothAdapter mBTAdapter = null;
	private BluetoothManager mBTManager = null;
	// A handler to deal with callbacks from BTManager
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BT_MSG_TOAST:
            	int msgData = msg.getData().getInt(BT_MSG_ERROR);
            	if(BT_ERR_CONN_FAILED == msgData) {
            		Toast.makeText(getApplicationContext(), R.string.bt_connection_failed, Toast.LENGTH_LONG).show();
            	} else if(BT_ERR_CONN_LOST == msgData) {
            		Toast.makeText(getApplicationContext(), R.string.bt_connection_lost, Toast.LENGTH_LONG).show();
            	}
            }
        }
    };

    // A Handler and Runnable to keep pushing messages to the wearable device
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
        	Log.d(TAG, "Runnable::run()");
        	if(mBTManager.getState() == BluetoothManager.STATE_CONNECTED &&
        			mBTManager.isDeviceReady()) {
        		Log.d(TAG, "BT is connected and ready");
        		sendToBT(InfoPool.INSTANCE.getNext());
        	}
            timerHandler.postDelayed(this, PUSH_INTERVAL_MS);
        }
    };

	// Pager adapter that provides fragments for each section
	private SectionsPagerAdapter mSectionsPagerAdapter;
	// ViewPager that hosts the section contents
	private ViewPager mViewPager;
	
	// Database helper class
	private DBHelper mDBHelper;
	// InfoPool instance
	InfoPool mPool = InfoPool.INSTANCE;
	
	// Contents of the CueSense profile tab
	private CueSenseListFragment mCSListFragment;
	// Contents of the Facebook tab
	private FBListFragment mFBListFragment;
	
	// Reference to Add Cue menu item to set its visibility when needed
	private MenuItem mAddMenuItem;
	


	/** MainActivity lifecycle methods*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
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
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOffscreenPageLimit(3);

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
			actionBar.addTab(newTab);
		}
		
		mDBHelper = new DBHelper(getApplication());
		mPool.addCueItems(mDBHelper.getItems(InfoType.INFO_CUESENSE));
	}
	
	@Override
	public void onStart() {
		super.onStart();
        Log.d(TAG, "onStart()");

        // If BT is not on, request that it be enabled.
        // setupBTLink() will then be called during onActivityResult
        if (!mBTAdapter.isEnabled()) {
        	Log.i(TAG, "BT not enabled. Enabling..");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the BT link
        } else {
        	Log.i(TAG, "BT enabled. Setting up link..");
            if (mBTManager == null) setupBTLink();
        }
	}
	
	@Override
	public synchronized void onPause() {
		super.onPause();
        Log.d(TAG, "onPause()");
//		timerHandler.removeCallbacks(timerRunnable);
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
              // Start the Bluetooth threads
              mBTManager.start();
            }
        }
    }
	
	@Override
    public void onDestroy() {
		Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // Stop the Bluetooth threads
        if (mBTManager != null) mBTManager.stop();
        timerHandler.removeCallbacks(timerRunnable);
    }
	/** End MainActivity lifecycle methods*/
	
	/**
	 * Connect to the Bluetooth device
	 */
	private void connectDevice() {
		Log.d(TAG, "connectDevice");
        // Get the BluetoothDevice object
        BluetoothDevice device = mBTAdapter.getRemoteDevice(BluetoothManager.DEVICE_MAC);
        // Attempt to connect to the device
        mBTManager.connect(device);
    }
	
	/**
	 * Setup BluetoothManager and UI element links
	 */
	private void setupBTLink() {
        Log.d(TAG, "setupBTLink()");

        // Initialize BluetoothManager
        mBTManager = new BluetoothManager(this, mHandler);

        // Connect to the Bluetooth device
        connectDevice();
        
        timerHandler.postDelayed(timerRunnable, PUSH_INTERVAL_MS);
    }
	
	/**
	 * Sends text from the TextView to the Bluetooth device
	 * @param text
	 */
	private void sendToBT(String text)
	{
		Log.d(TAG, "sendToBT() " + text);
        // Check that we're actually connected before trying anything
        if (mBTManager.getState() != BluetoothManager.STATE_CONNECTED) {
            Toast.makeText(this, R.string.bt_not_connected, Toast.LENGTH_LONG).show();
            return;
        }

        // Check that there's actually something to send
        if (text.length() > 0) {
            mBTManager.write(text);
        }
    }
	
	/**
	 *  This routine is called when an activity completes.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult()");
		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case REQUEST_ENABLE_BT:
            if (resultCode == Activity.RESULT_OK) {
                setupBTLink();
            } else {
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_LONG).show();
                finish();
            }
            break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		mAddMenuItem = menu.findItem(R.id.action_add);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			// Start Settings activity
			Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
			startActivity(settingsIntent);
			return true;
		} else if(id == R.id.action_add) {
			// New CueSense item dialog
			DialogFragment dialog = new NewCueSenseItemDialog();
			dialog.show(getSupportFragmentManager(), "new_cuesense_item");
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());
		// Enable Add menu item when CueSense tab is (re)selected
		InfoType tabType = InfoType.toInfoType(tab.getPosition());
		if(InfoType.INFO_CUESENSE == tabType) {
			if(mAddMenuItem != null) mAddMenuItem.setVisible(true);
		}
	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		// Disable Add menu item when CueSense tab is unselected
		InfoType tabType = InfoType.toInfoType(tab.getPosition());
		if(InfoType.INFO_CUESENSE == tabType) {
			mAddMenuItem.setVisible(false);
		}
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) { }

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
			Bundle args = new Bundle();
			// getItem is called to instantiate the fragment for the given page.
			switch(InfoType.toInfoType(position)) {
			case INFO_CUESENSE:
				if(mCSListFragment == null) {
					mCSListFragment = new CueSenseListFragment();
				}
				args.putInt(ARG_TAB_NUMBER, position + 1);
				mCSListFragment.setArguments(args);
				return mCSListFragment;
			case INFO_FACEBOOK:
				if(mFBListFragment == null) {
					mFBListFragment = new FBListFragment();
				}
				args.putInt(ARG_TAB_NUMBER, position + 1);
				mFBListFragment.setArguments(args);
				return mFBListFragment;
//			case INFO_TWITTER: // TODO - Twitter tab
			case INFO_SENTINEL: // falls through
			default: return PlaceholderFragment.newInstance(position + 1);
			}
		}

		@Override
		public int getCount() {
			return InfoType.INFO_SENTINEL.value();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			switch (InfoType.toInfoType(position)) {
			case INFO_CUESENSE:
				return getString(R.string.title_cuesense);
			case INFO_FACEBOOK:
				return getString(R.string.title_facebook);
			case INFO_TWITTER:
				return getString(R.string.title_twitter);
			case INFO_SENTINEL:
				return getString(R.string.title_foursquare);
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
			args.putInt(ARG_TAB_NUMBER, sectionNumber);
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

	/**
	 * Constructs a new CueItem and adds it to the database and InfoPool
	 */
	@Override
	public void onCueAdded(String itemData) {
		// Construct a new CueItem and push to database and InfoPool
		CueItem item = new CueItem(0, InfoType.INFO_CUESENSE, itemData, true);
		onCueSenseCueAdded(item);
	}

	/**
	 * Keep data model, database and InfoPool in sync when a CueItem is added
	 */
	@Override
	public void onCueSenseCueAdded(CueItem item) {
		Log.d(TAG, "onCueAdded()");
		// Push to database
		mDBHelper.addCueItem(item);
		// Push to InfoPool
		mPool.addCueItem(item);
		// Refresh CueSense list
		mCSListFragment.refreshList();
	}

	@Override
	public void onCueDeleted(int itemPosition) {
		mCSListFragment.onCueDeleted(itemPosition);
	}
	
	/**
	 * Keep data model, database and InfoPool in sync when a CueItem is deleted
	 */
	@Override
	public void onCueSenseCueDeleted(CueItem item) {
		Log.d(TAG, "onCueDeleted()");
		// Push to database
		mDBHelper.deleteCueItem(item);
		// Push to InfoPool
		mPool.deleteCueItem(item);
	}

	/**
	 * Keep data model, database and InfoPool in sync when a CueItem is modified
	 */
	@Override
	public void onCueSenseCueChanged(CueItem item) {
		Log.d(TAG, "onCueSenseCueChanged()");
		// Push to database
		mDBHelper.updateCueItem(item);
		// Push to InfoPool
		mPool.updateCueItem(item);
	}
}
