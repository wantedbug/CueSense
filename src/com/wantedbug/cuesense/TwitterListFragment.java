/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wantedbug.cuesense.MainActivity.InfoType;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import android.app.Activity;
import android.app.ActivityManager.RecentTaskInfo;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

public class TwitterListFragment extends ListFragment {
	// Debugging
	private static final String TAG = "TwitterListFragment";
	
	/**
	 * Interfaces
	 */
	public interface TwitterCueListener {
		/** Handle addition of a new Twitter cue */
		void onTwitterCueAdded(CueItem item);
		
		/** Handle addition of priority Twitter cues */
//		void onTwitterPriorityCuesAdded(List<CueItem> items);
		
		/** Handle logging out of Twitter */
		void onTwitterLogout();
	}
	
	/**
	 * Constants
	 */
	private static final String ITEM_DATA = "CATEGORY";
	
	/**
	 * Members
	 */
	// Listener to handle cue addition, deletion and modification
	private TwitterCueListener mListener;

	// View for the fragment
	View mView;
	// Expandable list view
	ExpandableListView mListView;
	// Expandable list view
	private SimpleExpandableListAdapter mAdapter;
	// Expandable list data
	List<Map<String, String>> mGroupData = new ArrayList<Map<String, String>>();
	List<List<Map<String, String>>> mChildData = new ArrayList<List<Map<String, String>>>();
	// Handler to change UI elements after async network operations
	private Handler mTwitterUIHandler;
	
	// Shared Preferences
	private static SharedPreferences mSharedPreferences;
	
	// Twitter library handles
	private static Twitter mTwitter;
	
	// Flag to check if data request has already been submitted to avoid duplication
	private boolean mTWRequestSubmitted = false;
	
	ResponseList<Status> recentFavourites = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
	    super.onCreate(savedInstanceState);
	    
	    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
	    
	    mTwitterUIHandler = new Handler();
	    
	    TwitterFactory factory = new TwitterFactory();
		mTwitter = factory.getInstance();
		mTwitter.setOAuthConsumer(SettingsActivity.TWITTER_CONSUMER_KEY, SettingsActivity.TWITTER_CONSUMER_SECRET);
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
	    super.onResume();
	    
	    if(isTwitterLoggedIn()) {
	    	String token = mSharedPreferences.getString(SettingsActivity.PREF_KEY_OAUTH_TOKEN, "");
	    	String tokenSecret = mSharedPreferences.getString(SettingsActivity.PREF_KEY_OAUTH_SECRET, "");
	    	if(!token.isEmpty() && !tokenSecret.isEmpty()) {
	    		AccessToken accessToken = new AccessToken(token, tokenSecret);
	    		mTwitter.setOAuthAccessToken(accessToken);
	    	} else {
	    		Log.e(TAG, "Twitter token and secret empty");
	    	}
	    }
	    
	    getData();
	}
	
	@Override
	public void onSaveInstanceState(Bundle bundle) {
		Log.d(TAG, "onSaveInstanceState()");
	    super.onSaveInstanceState(bundle);
	}
	
	@Override
	public void onPause() {
		Log.d(TAG, "onPause()");
	    super.onPause();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
	    super.onDestroy();
	    
//	    logoutFromTwitter();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView()");
	    mView = inflater.inflate(R.layout.tab_twitter, container, false);
	    
	    // Set up list view and list adapter
	    mListView = (ExpandableListView) mView.findViewById(android.R.id.list);
	    mAdapter = new SimpleExpandableListAdapter(
	    		getActivity(),
	    		mGroupData,
	    		android.R.layout.simple_expandable_list_item_1,
	    		new String[] { ITEM_DATA },
	    		new int[] { android.R.id.text1 },
	    		mChildData,
	    		android.R.layout.simple_list_item_1,
	    		new String[] { ITEM_DATA },
	    		new int[] { android.R.id.text1 }
	    		);
	    mListView.setAdapter(mAdapter);
	    // Get Twitter data
	    getData();
	    
	    return mView;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if(!(activity instanceof TwitterCueListener)) {
            throw new RuntimeException("Activity must implement TwitterCueListener interface!");
        }
        
        mListener = (TwitterCueListener) activity;
    }
	
	/**
	 * Returns Twitter login boolean flag from SharedPreferences  
	 */
	private boolean isTwitterLoggedIn() {
		return mSharedPreferences.getBoolean(SettingsActivity.PREF_KEY_TWITTER_LOGIN, false);
	}
	
	/**
	 * Logs the user out from Twitter
	 * Clears SharedPreferences of all tokens 
	 */
	private void logoutFromTwitter() {
		Log.d(TAG, "logoutFromTwitter()");
		Editor e = mSharedPreferences.edit();
		e.remove(SettingsActivity.PREF_KEY_OAUTH_TOKEN);
		e.remove(SettingsActivity.PREF_KEY_OAUTH_SECRET);
		e.remove(SettingsActivity.PREF_KEY_TWITTER_LOGIN);
		e.commit();
	}
	
	/**
	 * Fetch user data on launch
	 */
	private void getData() {
		Log.d(TAG, "getData()");
		
		if(!isTwitterLoggedIn() || mTWRequestSubmitted) return;
		
		mTWRequestSubmitted = true;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					recentFavourites = mTwitter.getFavorites();
				} catch (TwitterException e) {
					Log.e(TAG, "Twitter query error " + e);
				}
				
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(!recentFavourites.isEmpty()) {
							/** 1. Add the list item header to the list view */
							Map<String, String> favouritesGroupMap = new HashMap<String, String>();
							favouritesGroupMap.put(ITEM_DATA, "Favourites");
							/** 2. Get the children from the JSON response */
							List<Map<String, String>> favouritesList = new ArrayList<Map<String, String>>();
							for(Status s : recentFavourites) {
								Map<String, String> favouritesChild = new HashMap<String, String>();
								favouritesChild.put(ITEM_DATA, s.getText());
								favouritesList.add(favouritesChild);
								CueItem teamItem = new CueItem(-1, InfoType.INFO_TWITTER, s.getText(), true);
								mListener.onTwitterCueAdded(teamItem);
							}
							/** 3. Add the list item's children to the list view */
							mGroupData.add(favouritesGroupMap);
							mChildData.add(favouritesList);
							// Notify that the list contents have changed
							mAdapter.notifyDataSetChanged();
						}
					}
				});
			}
		}).start();
	}
}