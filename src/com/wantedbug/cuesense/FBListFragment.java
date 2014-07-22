/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.wantedbug.cuesense.MainActivity.InfoType;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

public class FBListFragment extends ListFragment {
	// Debugging
	private static final String TAG = "FBListFragment";
	
	/**
	 * Constants
	 */
	private static final String ITEM_DATA = "CATEGORY";
	// Activity request code to update Session info
	private static final int REAUTH_ACTIVITY_CODE = 100;
	// List of Facebook permissions
	private static final int NUM_PERMISSIONS = 13;
	private static final String[] fbPermissions = {
		"public_profile", // Facebook public profile
		"user_actions.books",
		"user_actions.movies",
		"user_actions.music",
		"user_tagged_places",
		"user_birthday",
		"user_about_me",
		"user_education_history",
		"user_work_history",
		"user_hometown",
		"user_activities", 
		"user_interests",
		"user_likes"
		};
	private static final String[] fbPermissionStrings = {
		"Profile",
		"Books",
		"Movies",
		"Music",
		"Places",
		"Birthday",
		"About me",
		"Education",
		"Work history",
		"Hometown",
		"Activities",
		"Interests",
		"Likes"
	};
	
	/**
	 * Members
	 */
	// Expandable list view
	private SimpleExpandableListAdapter mAdapter;
	// View for the fragment
	View v;
	
	// Handle to database
	private DBHelper mDBHelper;
	// Data for the list views
	private List<CueItem> mFBList;
	// Expandable list view
	ExpandableListView mListView;
	// Expandable list data
	List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
	List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
		
	// Facebook UI lifecycle helper to complete Activity lifecycle methods
	UiLifecycleHelper mUiLifecycleHelper;
	// Callback to handle Session state change
	private Session.StatusCallback mFBCallback = new Session.StatusCallback() {
	    @Override
	    public void call(final Session session, final SessionState state, final Exception exception) {
	        onSessionStateChange(session, state, exception);
	    }
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
	    super.onCreate(savedInstanceState);
	    mDBHelper = new DBHelper(getActivity());
	    mFBList = mDBHelper.getItems(InfoType.INFO_FACEBOOK);
	    mUiLifecycleHelper = new UiLifecycleHelper(getActivity(), mFBCallback);
	    mUiLifecycleHelper.onCreate(savedInstanceState);
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    mUiLifecycleHelper.onResume();
	    // Get Facebook data
	    if(groupData.isEmpty()) getData();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
		Log.d(TAG, "onSaveInstanceState()");
	    super.onSaveInstanceState(bundle);
	    mUiLifecycleHelper.onSaveInstanceState(bundle);
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause()");
	    super.onPause();
	    mUiLifecycleHelper.onPause();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
	    super.onDestroy();
	    mUiLifecycleHelper.onDestroy();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView()");
	    v = inflater.inflate(R.layout.tab_facebook, container, false);
		// Set up list view and list adapter
		mListView = (ExpandableListView) v.findViewById(android.R.id.list);
		mAdapter = new SimpleExpandableListAdapter(
				getActivity(),
				groupData,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { ITEM_DATA },
				new int[] { android.R.id.text1 },
				childData,
				android.R.layout.simple_list_item_1,
				new String[] { ITEM_DATA },
				new int[] { android.R.id.text1 }
				);
		mListView.setAdapter(mAdapter);
		// Get Facebook data
		getData();
		return v;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult()");
	    super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode == REAUTH_ACTIVITY_CODE) {
	    	mUiLifecycleHelper.onActivityResult(requestCode, resultCode, data);
	    }
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		Log.d(TAG, "onActivityCreated()");
	    super.onActivityCreated(savedInstanceState);
	}
	
	/**
	 * Fetch user data on launch
	 */
	private void getData() {
		Log.d(TAG, "getData()");
	    // Check for an open session
	    Session session = Session.getActiveSession();
	    if (session != null && session.isOpened()) {
	        // Get the user's data
	        makeMeRequest(session);
	    }
	}

	/**
	 * Handle Facebook Session state change
	 * @param session
	 * @param state
	 * @param exception
	 */
	private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
		Log.d(TAG, "onSessionStateChange()");
	    if (session != null && session.isOpened()) {
	        // Get the user's data.
	        makeMeRequest(session);
	    }
	}
	
	/**
	 * Make the /me request to the Facebook Graph API
	 * @param session
	 */
	private void makeMeRequest(final Session session) {
		Log.d(TAG, "makeMeRequest()");
	    Request request = Request.newMeRequest(session, 
	            new Request.GraphUserCallback() {
	        @Override
	        public void onCompleted(GraphUser user, Response response) {
	            // If the response is successful
	            if (session == Session.getActiveSession()) {
	                if (user != null) {
	                	getUserInfo(session, user);
	                }
	                TextView emptyMessage = (TextView) v.findViewById(android.R.id.empty);
	                emptyMessage.setText(R.string.fb_no_data);
	            }
	            if (response.getError() != null) {
	                TextView emptyMessage = (TextView) v.findViewById(android.R.id.empty);
	                emptyMessage.setText(R.string.fb_data_error);
	            }
	        }
	    });
	    request.executeAsync();
	}
	
	/**
	 * Helper function to populate the list with user data
	 * @param session 
	 * @param user 
	 */
	protected void getUserInfo(Session session, GraphUser user) {
		Log.d(TAG, "getUserInfo()");
	    for(int i = 0; i < NUM_PERMISSIONS; ++i) {
			if(session.isPermissionGranted(fbPermissions[i])) {
				// Create the expandable header item
				Log.i(TAG, "getUserInfo() " + fbPermissions[i] + " granted");
				Map<String, String> curGroupMap = new HashMap<String, String>();
	            groupData.add(curGroupMap);
	            curGroupMap.put(ITEM_DATA, fbPermissionStrings[i]);
	            // Create its children
	            List<Map<String, String>> children = new ArrayList<Map<String, String>>();
	            Map<String, String> curChildMap = new HashMap<String, String>();
				children.add(curChildMap);
//				Object data = user.getProperty(fbPermissions[i]);
				curChildMap.put(ITEM_DATA, user.getFirstName() + " " + user.getBirthday());
				childData.add(children);
			} else {
				Log.i(TAG, "getUserInfo() " + fbPermissions[i] + " NOT granted");
			}
		}
	    mAdapter.notifyDataSetChanged();
	}
}
