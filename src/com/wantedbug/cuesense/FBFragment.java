/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
//import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.Request;
import com.facebook.Request.GraphUserCallback;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;
//import com.facebook.widget.UserSettingsFragment;

/**
 * This class holds the Facebook login button. 
 * @author vikasprabhu
 */
public class FBFragment extends Fragment {
	// Debugging
	private static final String TAG = "FBFragment";
	/**
	 * Constants
	 */
	// List of Facebook permissions
	public static final List<String> FB_PERMISSIONS =
			Collections.unmodifiableList(Arrays.asList(
					"public_profile",
					"user_about_me",
					"user_actions.books",
					"user_actions.music",
					"user_actions.news",
					"user_actions.video",
					"user_activities",
					"user_birthday",
					"user_education_history",
					"user_events",
					"user_friends",
					"user_games_activity",
					"user_groups",
					"user_hometown",
					"user_interests",
					"user_likes",
					"user_location",
					"user_photos",
					"user_relationship_details",
					"user_relationships",
					"user_religion_politics",
					"user_status",
					"user_tagged_places",
					"user_videos",
					"user_website",
					"user_work_history"
					));
	
	/**
	 * Members
	 */
	// UiLifecycleHelper class from Facebook SDK
	// Note: add this to any activity/fragment that needs to track and respond
	// to Facebook.Session state changes.
	private UiLifecycleHelper uiHelper;
	
	// TextView to display user's Facebook data
	private TextView userInfoTextView;
	
	// Facebook.Session cache
	private Session mSession;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate()");
	    super.onCreate(savedInstanceState);
	    uiHelper = new UiLifecycleHelper(getActivity(), callback);
	    uiHelper.onCreate(savedInstanceState);
	}
	
	/** BEGIN Fragment lifecycle methods*/
	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
	    super.onResume();
	    
	    // For scenarios where the main activity is launched and user
	    // session is not null, the session state change notification
	    // may not be triggered. Trigger it if it's open/closed.
	    Session session = Session.getActiveSession();
	    if (session != null &&
	           (session.isOpened() || session.isClosed()) ) {
	        onSessionStateChange(session, session.getState(), null);
	    }

	    uiHelper.onResume();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult()");
	    super.onActivityResult(requestCode, resultCode, data);
	    uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause()");
	    super.onPause();
	    uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
	    super.onDestroy();
	    uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.d(TAG, "onSaveInstanceState()");
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}
	/** END Fragment lifecycle methods*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, 
			Bundle savedInstanceState) {
		Log.d(TAG, "onCreateView()");
		View view = inflater.inflate(R.layout.fragment_fb, container, false);

		LoginButton authButton = (LoginButton) view.findViewById(R.id.authButton);
		authButton.setFragment(this);
		authButton.setReadPermissions(FB_PERMISSIONS);
		
		userInfoTextView = (TextView) view.findViewById(R.id.userInfoTextView);
		
		return view;
	}
	
	/**
	 * Constructs user data for TextView
	 * @param user
	 * @return
	 */
	private String buildUserInfoDisplay(GraphUser user) {
	    StringBuilder userInfo = new StringBuilder("");
	    userInfo.append(getResources().getString(R.string.fb_logged_in_as) + " ");
	    userInfo.append(user.getName());
	    return userInfo.toString();
	}
	
	// Function to respond to Facebook.Session state changes
	// For instance, change the UI
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	    if (state.isOpened()) {
	        if(mSession == null || isSessionChanged(session)) {
	        	mSession = session; // cache the session
	        	Log.i(TAG, "Access Token: " + session.getAccessToken());

	        	// Request user data and show the results
	        	Request.newMeRequest(session, new GraphUserCallback() {

	        		@Override
	        		public void onCompleted(GraphUser user, Response response) {
	        			if(user != null) {
	        				Log.i(TAG, "Logged in, /me complete");
	        				userInfoTextView.setText(buildUserInfoDisplay(user));
	        			}
	        		}
	        	}).executeAsync();
	        }
	    } else if (state.isClosed()) {
	        Log.i(TAG, "Facebook logged out");
	        userInfoTextView.setText(R.string.fb_login_explanation);
	    }
	}
	
	// Callback listener to listen for Facebook.Session state changes
	private Session.StatusCallback callback = new Session.StatusCallback() {
	    @Override
	    public void call(Session session, SessionState state, Exception exception) {
	        onSessionStateChange(session, state, exception);
	    }
	};
	
	// Check if Facebook.Session changed between calls
	// Note: required as onSessionStateChange() gets called multiple times
	private boolean isSessionChanged(Session session) {
	    // Check if session state changed
	    if (mSession.getState() != session.getState())
	        return true;

	    // Check if accessToken changed
	    if (mSession.getAccessToken() != null) {
	        if (!mSession.getAccessToken().equals(session.getAccessToken()))
	            return true;
	    }
	    else if (session.getAccessToken() != null) {
	        return true;
	    }

	    // Nothing changed
	    return false;
	}
}
