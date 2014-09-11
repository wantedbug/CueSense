/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.wantedbug.cuesense.MainActivity.InfoType;

import twitter4j.PagableResponseList;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
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
	
	// Shared Preferences
	private static SharedPreferences mSharedPreferences;
	
	// Twitter library handles
	private static Twitter mTwitter;
	
	// Flag to check if data request has already been submitted to avoid duplication
	private boolean mTWRequestSubmitted = false;
	
	private long mUserId = 0;
	private String mUserName = null;
	private ResponseList<Status> mFavourites = null;
	private PagableResponseList<User> mFriends = null;
	private List<User> mFollowers = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
	    super.onCreate(savedInstanceState);
	    
	    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
	    
	    TwitterFactory factory = new TwitterFactory();
		mTwitter = factory.getInstance();
		mTwitter.setOAuthConsumer(TwitterUtils.TWITTER_CONSUMER_KEY, TwitterUtils.TWITTER_CONSUMER_SECRET);
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
	    super.onResume();
	    
	    // If we're logged in, refresh AccessToken and user details, and get
	    // user's Twitter data.
	    // If not, this means that the user has just logged out of
	    // Twitter from the Settings view, and we need to clear Twitter data.
	    if(isTwitterLoggedIn()) {
	    	String token = mSharedPreferences.getString(TwitterUtils.PREF_KEY_OAUTH_TOKEN, "");
	    	String tokenSecret = mSharedPreferences.getString(TwitterUtils.PREF_KEY_OAUTH_SECRET, "");
	    	if(!token.isEmpty() && !tokenSecret.isEmpty()) {
	    		AccessToken accessToken = new AccessToken(token, tokenSecret);
	    		mTwitter.setOAuthAccessToken(accessToken);
	    	} else {
	    		Log.e(TAG, "Twitter token and secret empty");
	    	}
	    	
	    	mUserId = mSharedPreferences.getLong(TwitterUtils.PREF_KEY_TWITTER_USERID, -1);
	    	mUserName  = mSharedPreferences.getString(TwitterUtils.PREF_KEY_TWITTER_NAME, "");
	    	
	    	// Get Twitter data from API queries
	    	getData();
	    } else {
	    	logoutFromTwitter();
	    }
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
		return mSharedPreferences.getBoolean(TwitterUtils.PREF_KEY_TWITTER_LOGIN, false);
	}
	
	/**
	 * Logs the user out from Twitter
	 * Clears SharedPreferences of all tokens and notifies the listener
	 * to clear all Twitter data from the InfoPool
	 */
	private void logoutFromTwitter() {
		Log.d(TAG, "logoutFromTwitter()");

		TwitterUtils.INSTANCE.logoutFromTwitter();
		mListener.onTwitterLogout();
	}
	
	/**
	 * Fetch user data on launch
	 */
	private void getData() {
		Log.d(TAG, "getData()");
		
		if(!isTwitterLoggedIn()) return;
		if(mTWRequestSubmitted) return;
		
		mTWRequestSubmitted = true;
		Log.i(TAG, "Twitter user " + mUserId + "," + mUserName);
		
		// Recent favourites
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					mFavourites = mTwitter.getFavorites();
				} catch (TwitterException e) {
					Log.e(TAG, "Twitter favourites query error " + e);
				}
				
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(!mFavourites.isEmpty()) {
							/** 1. Add the list item header to the list view */
							Map<String, String> favouritesGroupMap = new HashMap<String, String>();
							favouritesGroupMap.put(ITEM_DATA, "My favourite tweets");
							/** 2. Get the children from the response */
							List<Map<String, String>> favouritesList = new ArrayList<Map<String, String>>();
							int count = 0;
							for(Status s : mFavourites) {
								Map<String, String> favouritesChild = new HashMap<String, String>();
								String data = "@" + s.getUser().getName() + ": " + s.getText();
								favouritesChild.put(ITEM_DATA, data);
								favouritesList.add(favouritesChild);
								CueItem favouriteItem = new CueItem(-1, InfoType.INFO_TWITTER, data, true);
//								mListener.onTwitterCueAdded(favouriteItem);
								++count;
								if(count == 20) break;
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
		
		// Recent friends
		new Thread(new Runnable() {
			@Override
			public void run() {
				long cursor = -1;
				try {
					mFriends = mTwitter.getFriendsList(mUserId, cursor);
				} catch (TwitterException e) {
					Log.e(TAG, "Twitter friends query error " + e);
				}
				
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(!mFriends.isEmpty()) {
							/** 1. Add the list item header to the list view */
							Map<String, String> friendsGroupMap = new HashMap<String, String>();
							friendsGroupMap.put(ITEM_DATA, "My friends and followees");
							/** 2. Get the children from the response */
							List<Map<String, String>> friendsList = new ArrayList<Map<String, String>>();
							int count = 0;
							for(User u : mFriends) {
								Map<String, String> friendsChild = new HashMap<String, String>();
								String data = u.getName();
								friendsChild.put(ITEM_DATA, data);
								friendsList.add(friendsChild);
								CueItem friendItem = new CueItem(-1, InfoType.INFO_TWITTER, data, true);
								mListener.onTwitterCueAdded(friendItem);
								++count;
								if(count == 20) break;
							}
							/** 3. Add the list item's children to the list view */
							mGroupData.add(friendsGroupMap);
							mChildData.add(friendsList);
							// Notify that the list contents have changed
							mAdapter.notifyDataSetChanged();
						}
					}
				});
			}
		}).start();
		
		// Recent followers
		new Thread(new Runnable() {
			@Override
			public void run() {
				long cursor = -1;
				try {
					Log.i(TAG, "TWitter user " + mUserId + mUserName);
					mFollowers = mTwitter.getFollowersList(mUserId, cursor);
				} catch (TwitterException e) {
					Log.e(TAG, "Twitter followers query error " + e);
				}
				
				getActivity().runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if(!mFollowers.isEmpty()) {
							/** 1. Add the list item header to the list view */
							Map<String, String> followersGroupMap = new HashMap<String, String>();
							followersGroupMap.put(ITEM_DATA, "My followers");
							/** 2. Get the children from the response */
							List<Map<String, String>> followersList = new ArrayList<Map<String, String>>();
							int count = 0;
							for(User u : mFollowers) {
								Map<String, String> followersChild = new HashMap<String, String>();
								String data = u.getName();
								followersChild.put(ITEM_DATA, data);
								followersList.add(followersChild);
								CueItem followerItem = new CueItem(-1, InfoType.INFO_TWITTER, data, true);
								mListener.onTwitterCueAdded(followerItem);
								++count;
								if(count == 20) break;
							}
							/** 3. Add the list item's children to the list view */
							mGroupData.add(followersGroupMap);
							mChildData.add(followersList);
							// Notify that the list contents have changed
							mAdapter.notifyDataSetChanged();
						}
					}
				});
			}
		}).start();
	}
}
