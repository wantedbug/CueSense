/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.json.JSONObject;

import com.wantedbug.cuesense.CueSenseListFragment.CueSenseListener;
import com.wantedbug.cuesense.DeleteCueSenseItemDialog.DeleteCueSenseItemListener;
import com.wantedbug.cuesense.FBListFragment.FacebookCueListener;
import com.wantedbug.cuesense.NewCueSenseItemDialog.NewCueSenseItemListener;
import com.wantedbug.cuesense.TwitterListFragment.TwitterCueListener;

import android.annotation.SuppressLint;
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
//import android.support.v4.app.FragmentTransaction;
//import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentPagerAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;


/**
 * This class is the starting point for the CueSense application. 
 * @author vikasprabhu
 */
public class MainActivity extends FragmentActivity implements ActionBar.TabListener,
		CueSenseListener, NewCueSenseItemListener, DeleteCueSenseItemListener,
		FacebookCueListener,
		TwitterCueListener {
	// Debugging
	private static final String TAG = "MainActivity";
	public static final boolean DEBUG = true;

	/**
	 * Constants
	 */
	// Intent request codes
	private static final int REQUEST_ENABLE_BT = 1;	

	// Bluetooth RSSI range values
	// Note: calibrate these for every test environment since Bluetooth RSSI
	// values are dependent on the surroundings, surfaces, objects, obstacles, etc.
	private static final int BT_RSSI_NEAR = 50;
	private static final int BT_RSSI_INTERMEDIATE = 75;
	private static final int BT_RSSI_FAR = 100;

	// Time interval between successive data push attempts to the wearable device
	private static final int PUSH_INTERVAL_MS = 8000;
	// Time interval between successive Bluetooth discovery scans
	private static final int SCAN_INTERVAL_MS = 8000;
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
	public static final int BT_MSG_SENDRECV_DONE = 2;
	public static final int BT_MSG_PAIREDUSERCONNECTED = 3;
	// Key message names received from BTManager
	public static final String BT_MSG_ERROR = "error";
	public static final String BT_MSG_SENDRECV_DATA = "data";
	// Error message values
	public static final int BT_ERR_CONN_LOST = 1;
	public static final int BT_ERR_CONN_FAILED = 2;
	
	// Distance levels
	public static final int DISTANCE_OUTOFRANGE = 0;
	public static final int DISTANCE_NEAR = 1;
	public static final int DISTANCE_INTERMEDIATE = 2;
	public static final int DISTANCE_FAR = 3;
	
	/**
	 * Members
	 */
	private BluetoothAdapter mBTAdapter = BluetoothAdapter.getDefaultAdapter();
	private BluetoothManager mBTManager = null;
	// A handler to deal with callbacks from BTManager
    @SuppressLint("HandlerLeak")
	private final Handler mBTMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	Log.d(TAG, "handleMessage()");
            switch (msg.what) {
            case BT_MSG_TOAST:
            	int msgData = msg.getData().getInt(BT_MSG_ERROR);
            	if(BT_ERR_CONN_FAILED == msgData) {
            		Toast.makeText(getApplicationContext(), R.string.bt_connection_failed, Toast.LENGTH_LONG).show();
            	} else if(BT_ERR_CONN_LOST == msgData) {
            		Toast.makeText(getApplicationContext(), R.string.bt_connection_lost, Toast.LENGTH_LONG).show();
            	}
            	break;
            case BT_MSG_PAIREDUSERCONNECTED:
            	Log.i(TAG, "users connected");
            	if(mBTAdapter.getAddress().equals(USER1))
            		mBTManager.writeToPairedUser(getCuesData(mCurrDistance).toString().getBytes());
            	setDataChanged(mCurrDistance, false);
            	break;
            case BT_MSG_SENDRECV_DONE:
            	mCurrDevice = null;
            	// Send received data to InfoPool for matching
            	String data = msg.getData().getString(BT_MSG_SENDRECV_DATA);
            	if(!data.isEmpty()) mPool.matchData(data);
    			// Unpair the users' phones if they were bonded
            	// Note: we have to do this because the low level implementation may change between
            	// device manufacturers
            	Log.i(TAG, "Unpairing phones");
    			Set<BluetoothDevice> devices = mBTAdapter.getBondedDevices();
    			for(BluetoothDevice dev : devices) {
    				if(dev.getName().equals(USER1) || dev.getName().equals(USER2)) {
    					Log.i(TAG, dev.getName() + " bonded after BT_MSG_SENDRECV_DONE");
    					try {
    						Method method = dev.getClass().getMethod("removeBond", (Class[]) null);
    						method.invoke(dev, (Object[]) null);
    						Log.i(TAG, "unbonded");
    					} catch (Exception e) {
    						Log.e(TAG, "Could not unpair " + e);
    					}
    					break;
    				}
    			}
            	// Restart listening and discovery
            	Log.i(TAG, "Restarting user threads and discovery");
            	mBTManager.startPairedUserThreads();
            	if(mBTAdapter.getAddress().equals(USER1))
            		mBTScanHandler.postDelayed(mBTScanRunnable, SCAN_INTERVAL_MS);
            	break;
            }
        }
    };

    // A Handler and Runnable to periodically keep pushing messages to the wearable device
    Handler mSendCueHandler = new Handler();
    Runnable mSendCueRunnable = new Runnable() {
        @Override
        public void run() {
        	Log.d(TAG, "mSendCueRunnable::run()");
        	if(mBTManager.getWearableState() == BluetoothManager.STATE_CONNECTED &&
        			mBTManager.isDeviceReady()) {
        		Log.d(TAG, "BT is connected and ready");
        		sendToBT(InfoPool.INSTANCE.getNext());
        	}
        	mSendCueHandler.postDelayed(this, PUSH_INTERVAL_MS);
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
	// Contents of the Twitter tab
	private TwitterListFragment mTWListFragment;
	
	// TwitterUtils instance
	TwitterUtils mTwitterUtils = TwitterUtils.INSTANCE;
	
	// Reference to Add Cue menu item to set its visibility when needed
	private MenuItem mAddMenuItem;
	
	// Cues data wrapped in JSONObjects to be transmitted to the nearby user
	// Note: one object for each distance range
	private JSONObject mNearData;
	private JSONObject mIntermediateData;
	private JSONObject mFarData;
	// Flags set when above data changes
	private boolean mNearDataChanged = true;
	private boolean mIntermediateDataChanged = true;
	private boolean mFarDataChanged = true;
	
	// Distance levels to determine what data to send
	private int mPrevDistance = DISTANCE_OUTOFRANGE;
	private int mCurrDistance;
	
	// BluetoothDevice cache to avoid multiple discovery callbacks interfering with
	// an ongoing transmission
	private BluetoothDevice mCurrDevice = null;
	
	private static final String USER1 = "6C:F3:73:65:65:19"; // timo@s3mini, GT-I8190N
	private static final String USER2 = "6C:F3:73:65:66:A3"; // nikkis@s3mini, nikkis@s3mini
	
	// BroadcastReceiver to listen for another user's Bluetooth device
	private BroadcastReceiver mBTScanReceiver = new BroadcastReceiver() {
        @Override
        public synchronized void onReceive(Context context, Intent intent) {
        	Log.d(TAG, "BroadcastReceiver::onReceive()");
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
            	// Get signal strength and device details
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                Log.i(TAG, "onReceive() " + name + "," + rssi + "dBm" +
                		", pairedState=" + mBTManager.getPairedUserState());
                BluetoothDevice temp = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                BluetoothDevice device = mBTAdapter.getRemoteDevice(temp.getAddress());
                // Send if we find the right device and if we're ready to accept the connection
                if(device != null && device.getAddress().equals(USER2) &&
                		(mCurrDevice == null || !mCurrDevice.getAddress().equals(device.getAddress())) &&
                		mBTManager.getPairedUserState() >= BluetoothManager.STATE_LISTEN) {
                	mCurrDistance = getDistanceFromRSSI(rssi);
                	synchronized (this) {
                		// Send if in range or if data has changed
                		if(mCurrDistance != DISTANCE_OUTOFRANGE && isDataChanged(mCurrDistance)) {
                			Log.i(TAG, "Sending to " + device.getName() + "," + device.getAddress());
                			// Cache the BluetoothDevice and distance range
                			mCurrDevice = device;
                			mPrevDistance = mCurrDistance;
                			// Stop discovery
                        	mBTAdapter.cancelDiscovery();
                        	mBTScanHandler.removeCallbacks(mBTScanRunnable);
                        	// Get the data to be sent
                			JSONObject data = getCuesData(mCurrDistance);
                			if(null != data) {
                				// Connect and send
                				mBTManager.connectAndSend(device, data);
                				// Note: Devices are unbonded later after send/receive succeeds
                				if(device.getBondState() == BluetoothDevice.BOND_BONDED) {
                					Log.i(TAG, device.getName() + " bonded after connectAndSend()");
                				}
                			} else {
                				// Restart listening and discovery
                            	Log.i(TAG, "no data for distance " + mCurrDistance);
                            	mCurrDevice = null;
                            	setDataChanged(mCurrDistance, false);
                            	mBTManager.startPairedUserThreads();
                            	if(mBTAdapter.getAddress().equals(USER1))
                            		mBTScanHandler.postDelayed(mBTScanRunnable, SCAN_INTERVAL_MS);
                			}
                		} else {
                			Log.i(TAG, "not sending data " + mCurrDistance + mPrevDistance + isDataChanged(mCurrDistance));
                		}
                	}
                }
            }
        }
    };
    
    // A Handler and Runnable to continuously scan for another user's Bluetooth device
    // specifically for signal strength
    Handler mBTScanHandler = new Handler();
    Runnable mBTScanRunnable = new Runnable() {
        @Override
        public void run() {
        	if(mBTAdapter != null) {
        		Log.d(TAG, "performing BT scan");
        		if(!mBTAdapter.isDiscovering()) {
        			mBTAdapter.startDiscovery();
        		}
        	}
            mBTScanHandler.postDelayed(this, SCAN_INTERVAL_MS);
        }
    };

	/** MainActivity lifecycle methods*/
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		/** Bluetooth setup */
	    // If the adapter is null, then Bluetooth is not supported
        if (mBTAdapter == null) {
        	Log.e(TAG, "BT not available");
            Toast.makeText(this, R.string.bt_not_available, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        /** Set up TwitterUtils */
		mTwitterUtils.init(getApplicationContext());
		
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
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				actionBar.setSelectedNavigationItem(position);
			}
		});
		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			Tab newTab = actionBar.newTab()
					.setText(mSectionsPagerAdapter.getPageTitle(i))
					.setTabListener(this);
			actionBar.addTab(newTab);
		}
		
		/** Database and InfoPool setup */
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
            if (mBTManager.getWearableState() == BluetoothManager.STATE_NONE) {
              // Start the Bluetooth threads
              mBTManager.setup();
            }
            if (mBTManager.getPairedUserState() == BluetoothManager.STATE_NONE) {
            	mBTManager.startPairedUserThreads();
            }
        }
    }
	
	@Override
    public void onDestroy() {
		Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // Stop the Bluetooth threads
        if (mBTManager != null) {
        	mBTManager.stop();
        	mBTManager.stopPairedUserThreads();
        }
        // Stop the send and scan handler runnables
        mSendCueHandler.removeCallbacks(mSendCueRunnable);
        mBTScanHandler.removeCallbacks(mBTScanRunnable);
        if(mBTScanReceiver != null && mBTAdapter.getAddress().equals(USER1)) {
        	try {
        		unregisterReceiver(mBTScanReceiver);
        	} catch(IllegalArgumentException e) {
        		Log.d(TAG, "receiver not registered");
        		mBTScanReceiver = null;
        	}
        }
        mPool.clear();
    }
	/** End MainActivity lifecycle methods*/
	
	/**
	 * Connect to the Bluetooth device
	 */
	private void connectDevice() {
		Log.d(TAG, "connectDevice");
        // Get the BluetoothDevice object
        BluetoothDevice device = null;
        if(mBTAdapter.getAddress().equals(USER2)) {
        	device = mBTAdapter.getRemoteDevice(BluetoothManager.DEVICE1_MAC);
        } else {
        	device = mBTAdapter.getRemoteDevice(BluetoothManager.DEVICE2_MAC);
        }
        // Attempt to connect to the device
        mBTManager.connectWearable(device);
    }
	
	/**
	 * Setup BluetoothManager and UI element links
	 */
	private void setupBTLink() {
        Log.d(TAG, "setupBTLink()");

        // Initialize BluetoothManager
        mBTManager = new BluetoothManager(this, mBTMessageHandler);

        // Connect to the Bluetooth device
        connectDevice();
        
        // Start the runnable to periodically keep pushing data, if available, to the wearable
        mSendCueHandler.postDelayed(mSendCueRunnable, PUSH_INTERVAL_MS);
        
        // Start Bluetooth discovery to continuously monitor signal strength of
        // the nearby user
        // Note: This is being run on a GS3Mini with 4.1.2 JellyBean which does not
        // support Bluetooth LE. Therefore discovery being started is MANDATORY.
        // Register the BroadcastReceiver
        if(mBTAdapter.getAddress().equals(USER1)) {
	        registerReceiver(mBTScanReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
			mBTScanHandler.post(mBTScanRunnable);
        }
    }
	
	/**
	 * Sends text from the TextView to the Bluetooth device
	 * @param text
	 */
	private void sendToBT(String text) {
		Log.d(TAG, "sendToBT() " + text);
        // Check that we're actually connected before trying anything
        if (mBTManager.getWearableState() != BluetoothManager.STATE_CONNECTED) {
            Toast.makeText(this, R.string.bt_not_connected, Toast.LENGTH_LONG).show();
            return;
        }

        // Check that there's actually something to send
        if (text.length() > 0) {
            mBTManager.writeToWearable(text);
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
			case INFO_TWITTER:
				if(mTWListFragment == null) {
					mTWListFragment = new TwitterListFragment();
				}
				args.putInt(ARG_TAB_NUMBER, position + 1);
				mTWListFragment.setArguments(args);
				return mTWListFragment;
			case INFO_SENTINEL: // falls through
			default:
				Log.e(TAG, "Wrong tab ruh-roh");
				return null;
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
				break;
			default:
				break;
			}
			return null;
		}
	}

    /**
     * Converts RSSI to distance level
     * @param rssi
     * @return
     */
    private int getDistanceFromRSSI(int rssi) {
    	rssi = java.lang.Math.abs(rssi);
    	if(rssi <= BT_RSSI_NEAR) {
    		return DISTANCE_NEAR;
    	} else if(rssi >BT_RSSI_NEAR && rssi <= BT_RSSI_INTERMEDIATE) {
    		return DISTANCE_INTERMEDIATE;
    	} else if(rssi > BT_RSSI_INTERMEDIATE && rssi <= BT_RSSI_FAR) {
    		return DISTANCE_FAR;
    	} else {
    		return DISTANCE_OUTOFRANGE;
    	}
    }
    
    /**
     * Returns true if there are new Cues available for the specified distance
     * range
     * @param distanceRange
     * @return
     */
    private boolean isDataChanged(int distanceRange) {
    	switch(distanceRange) {
    	case DISTANCE_NEAR: return mNearDataChanged;
    	case DISTANCE_INTERMEDIATE: return mIntermediateDataChanged;
    	case DISTANCE_FAR: return mFarDataChanged;
    	case DISTANCE_OUTOFRANGE:
    	default: return false;
    	}
    }
    
    /**
     * Sets/resets the data changed flag for the specified distance range
     * @param distanceRange
     * @param dataChanged
     */
    private void setDataChanged(int distanceRange, boolean dataChanged) {
    	switch(mCurrDistance) {
    	case DISTANCE_NEAR: mNearDataChanged = dataChanged; break;
    	case DISTANCE_INTERMEDIATE: mIntermediateDataChanged = dataChanged; break;
    	case DISTANCE_FAR: mFarDataChanged = dataChanged; break;
    	case DISTANCE_OUTOFRANGE:
		default: break;
    	}
    }
    
    /**
	 * Returns the appropriate Cues JSONObject
	 * @param distanceRange
	 */
	private JSONObject getCuesData(int distanceRange) {
		switch(distanceRange) {
		case DISTANCE_NEAR:
			if(null == mNearData) refreshCuesData(distanceRange);
			return mNearData;
		case DISTANCE_INTERMEDIATE:
			if(null == mIntermediateData) refreshCuesData(distanceRange);
			return mIntermediateData;
		case DISTANCE_FAR:
			if(null == mFarData) refreshCuesData(distanceRange);
			return mFarData;
		case DISTANCE_OUTOFRANGE:
		default:
			Log.e(TAG, "refreshCuesData() ruh-roh");
			break;
		}
		return null;
	}
	
	/**
	 * Refreshes the appropriate Cues JSONObject
	 * @param distanceRange
	 */
	private void refreshCuesData(int distanceRange) {
		switch(distanceRange) {
		case DISTANCE_NEAR:
			mNearData = null;
			mNearData = mPool.getData(distanceRange);
			mNearDataChanged = true;
			break;
		case DISTANCE_INTERMEDIATE:
			mIntermediateData = null;
			mIntermediateData = mPool.getData(distanceRange);
			mIntermediateDataChanged = true;
			break;
		case DISTANCE_FAR:
			mFarData = null;
			mFarData = mPool.getData(distanceRange);
			mFarDataChanged = true;
			break;
		case DISTANCE_OUTOFRANGE:
		default:
			Log.e(TAG, "refreshCuesData() ruh-roh");
			break;
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
//		Log.d(TAG, "onCueSenseCueAdded()");
		// Push to database
		mDBHelper.addCueItem(item);
		// Push to InfoPool
		mPool.addCueItem(item);
		// Refresh CueSense list
		mCSListFragment.refreshList();
		// Refresh Cues data
		setDataChanged(DISTANCE_NEAR, true);
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
//		Log.d(TAG, "onCueSenseCueDeleted()");
		// Push to database
		mDBHelper.deleteCueItem(item);
		// Push to InfoPool
		mPool.deleteCueItem(item);
		// Refresh Cues data
		setDataChanged(DISTANCE_NEAR, true);
	}

	/**
	 * Keep data model, database and InfoPool in sync when a CueItem is modified
	 */
	@Override
	public void onCueSenseCueChanged(CueItem item) {
//		Log.d(TAG, "onCueSenseCueChanged()");
		// Push to database
		mDBHelper.updateCueItem(item);
		// Push to InfoPool
		mPool.updateCueItem(item);
		// Refresh Cues data
		setDataChanged(DISTANCE_NEAR, true);
	}

	/**
	 * Keep InfoPool updated when a Facebook cue is added
	 */
	@Override
	public void onFacebookCueAdded(CueItem item) {
//		Log.d(TAG, "onFacebookCueAdded()");
		// Push to InfoPool
		mPool.addCueItem(item);
		// Refresh Cues data
		setDataChanged(DISTANCE_INTERMEDIATE, true);
	}

	/**
	 * Keep InfoPool updated when the user logs out of Facebook
	 */
	@Override
	public void onFacebookLogout() {
//		Log.d(TAG, "onFacebookLogout()");
		// Remove Facebook items from InfoPool
		mPool.deleteType(InfoType.INFO_FACEBOOK);
		// Refresh Cues data
		setDataChanged(DISTANCE_INTERMEDIATE, false);
	}
	
	/**
	 * Keep InfoPool updated when a "special" Facebook cue is added
	 * This is so that some Facebook data gets a special preference in the
	 * InfoPool queue.
	 */
	@Override
	public void onFacebookPriorityCuesAdded(List<CueItem> items) {
//		Log.d(TAG, "onFacebookPriorityCuesAdded()");
		// Add Facebook items to InfoPool
		mPool.addCueItemsToTop(items, InfoType.INFO_FACEBOOK);
		// Refresh Cues data
		setDataChanged(DISTANCE_INTERMEDIATE, true);
	}

	/**
	 * Keep InfoPool updated when a Twitter cue is added
	 */
	@Override
	public void onTwitterCueAdded(CueItem item) {
//		Log.d(TAG, "onTwitterCueAdded()");
		// Push to InfoPool
		mPool.addCueItem(item);
		// Refresh Cues data
		setDataChanged(DISTANCE_FAR, true);
	}

	/**
	 * Keep InfoPool updated when the user logs out of Twitter
	 */
	@Override
	public void onTwitterLogout() {
//		Log.d(TAG, "onTwitterLogout()");
		// Remove Twitter items from InfoPool
		mPool.deleteType(InfoType.INFO_TWITTER);
		// Refresh Cues data
		setDataChanged(DISTANCE_FAR, false);
	}
}
