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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

public class FBListFragment extends ListFragment {
	// Debugging
	private static final String TAG = "FBListFragment";
	
	/**
	 * Constants
	 */
	private static final String NAME = "NAME";
	private static final String IS_EVEN = "IS_EVEN";
	// Activity request code to update Session info
	private static final int REAUTH_ACTIVITY_CODE = 100;
	/**
	 * Members
	 */
	// Expandable list view
	private ExpandableListAdapter mAdapter;
	// View for the fragment
	View v;
	
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
	    super.onCreate(savedInstanceState);
	    mUiLifecycleHelper = new UiLifecycleHelper(getActivity(), mFBCallback);
	    mUiLifecycleHelper.onCreate(savedInstanceState);
	}
	
	@Override
	public void onResume() {
	    super.onResume();
	    mUiLifecycleHelper.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle bundle) {
	    super.onSaveInstanceState(bundle);
	    mUiLifecycleHelper.onSaveInstanceState(bundle);
	}

	@Override
	public void onPause() {
	    super.onPause();
	    mUiLifecycleHelper.onPause();
	}

	@Override
	public void onDestroy() {
	    super.onDestroy();
	    mUiLifecycleHelper.onDestroy();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.tab_facebook, container, false);
		// Get Facebook data
		getData();
		return v;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
	    super.onActivityResult(requestCode, resultCode, data);
	    if (requestCode == REAUTH_ACTIVITY_CODE) {
	    	mUiLifecycleHelper.onActivityResult(requestCode, resultCode, data);
	    }
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// TODO remove dummy data
		List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
		List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
		for (int i = 0; i < 10; i++) {
			Map<String, String> curGroupMap = new HashMap<String, String>();
			groupData.add(curGroupMap);
			curGroupMap.put(NAME, "Group " + i);
			curGroupMap.put(IS_EVEN, (i % 2 == 0) ? "This group is even" : "This group is odd");

			List<Map<String, String>> children = new ArrayList<Map<String, String>>();
			for (int j = 0; j < 2; j++) {
				Map<String, String> curChildMap = new HashMap<String, String>();
				children.add(curChildMap);
				curChildMap.put(NAME, "Child " + j);
				curChildMap.put(IS_EVEN, (j % 2 == 0) ? "This child is even" : "This child is odd");
			}
			childData.add(children);
		}
		ExpandableListView lv = (ExpandableListView) v.findViewById(android.R.id.list);
		// Set up our adapter
		mAdapter = new SimpleExpandableListAdapter(
				getActivity(),
				groupData,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME, IS_EVEN },
				new int[] { android.R.id.text1, android.R.id.text2 },
				childData,
				android.R.layout.simple_expandable_list_item_2,
				new String[] { NAME, IS_EVEN },
				new int[] { android.R.id.text1, android.R.id.text2 }
				);
		lv.setAdapter(mAdapter);
	}
	
	/**
	 * Fetch user data on launch
	 */
	private void getData() {
		// Check for an open session
	    Session session = Session.getActiveSession();
	    if (session != null && session.isOpened()) {
	        // Get the user's data
	        makeMeRequest(session);
	    }
	}

	/**
	 * Make the /me request to the Facebook Graph API
	 * @param session
	 */
	private void makeMeRequest(final Session session) {
		Request request = Request.newMeRequest(session, 
	            new Request.GraphUserCallback() {
	        @Override
	        public void onCompleted(GraphUser user, Response response) {
	            // If the response is successful
	            if (session == Session.getActiveSession()) {
	                if (user != null) {
	                    // Set the id for the ProfilePictureView
	                    // view that in turn displays the profile picture.
//	                    profilePictureView.setProfileId(user.getId());
	                    // Set the Textview's text to the user's name.
//	                    userNameView.setText(user.getName());
	                	getUserInfo(user);
	                }
	            }
	            if (response.getError() != null) {
	                // Handle errors, will do so later.
	            }
	        }
	    });
	    request.executeAsync();
	}
	
	/**
	 * Handle Facebook Session state change
	 * @param session
	 * @param state
	 * @param exception
	 */
	private void onSessionStateChange(final Session session, SessionState state, Exception exception) {
	    if (session != null && session.isOpened()) {
	        // Get the user's data.
	        makeMeRequest(session);
	    }
	}
	
	/**
	 * Helper function to populate the list with user data
	 * @param user 
	 */
	protected void getUserInfo(GraphUser user) {
		
	}

}
