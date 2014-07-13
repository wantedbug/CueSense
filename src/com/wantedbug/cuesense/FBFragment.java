/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

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

public class FBFragment extends Fragment {

	// Debugging
	private static final String TAG = "FBFragment";
	
	// Members
	// UiLifecycleHelper class from Facebook SDK
	// Note: add this to any activity/fragment that needs to track and respond
	// to Facebook.Session state changes.
	private UiLifecycleHelper uiHelper;
	
	// List of Facebook permissions
	private static final String[] fbPermissions = {
		"public_profile", // Facebook public profile
		"user_about_me",
		"user_activities",
		"user_birthday",
		"user_education_history",
		"user_hometown",
		"user_interests",
		"user_likes", // User likes
		"user_tagged_places",
		"user_work_history"
		};
	
	// TextView to display user's Facebook data
	private TextView userInfoTextView;
	
	// GraphUser represents a Facebook user
//	private FBUser mFBUser;
	
	// Facebook.Session cache
	private Session mSession;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
	    super.onCreate(savedInstanceState);
	    uiHelper = new UiLifecycleHelper(getActivity(), callback);
	    uiHelper.onCreate(savedInstanceState);
	}
	
	/** BEGIN Fragment lifecycle methods*/
	@Override
	public void onResume() {
		Log.i(TAG, "onResume()");
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
		Log.i(TAG, "onActivityResult()");
	    super.onActivityResult(requestCode, resultCode, data);
	    uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
		Log.i(TAG, "onPause()");
	    super.onPause();
	    uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		Log.i(TAG, "onDestroy()");
	    super.onDestroy();
	    uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState()");
	    super.onSaveInstanceState(outState);
	    uiHelper.onSaveInstanceState(outState);
	}
	/** END Fragment lifecycle methods*/
	
	@Override
	public View onCreateView(LayoutInflater inflater, 
			ViewGroup container, 
			Bundle savedInstanceState) {
		Log.i(TAG, "onCreateView()");
//		View view = inflater.inflate(R.layout.activity_main, container, false);
		View view = inflater.inflate(R.layout.fragment_fb, container, false);

		LoginButton authButton = (LoginButton) view.findViewById(R.id.authButton);
		authButton.setFragment(this);
		authButton.setReadPermissions(fbPermissions);
		
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

	    // Example: typed access (name)
	    // - no special permissions required
	    userInfo.append(String.format("Name: %s\n\n", 
	        user.getName()));

	    // Example: typed access (birthday)
	    // - requires user_birthday permission
	    userInfo.append(String.format("Birthday: %s\n\n", 
	        user.getBirthday()));

	    // Example: partially typed access, to location field,
	    // name key (location)
	    // - requires user_location permission
//	    userInfo.append(String.format("Location: %s\n\n", 
//	        user.getLocation().getProperty("name")));

	    // Example: access via property name (locale)
	    // - no special permissions required
//	    userInfo.append(String.format("Locale: %s\n\n", 
//	        user.getProperty("locale")));

	    // Example: access via key for array (languages) 
	    // - requires user_likes permission
	    JSONArray languages = (JSONArray)user.getProperty("languages");
	    if (languages.length() > 0) {
	        ArrayList<String> languageNames = new ArrayList<String> ();
	        for (int i=0; i < languages.length(); i++) {
	            JSONObject language = languages.optJSONObject(i);
	            // Add the language name to a list. Use JSON
	            // methods to get access to the name field. 
	            languageNames.add(language.optString("name"));
	        }           
	        userInfo.append(String.format("Languages: %s\n\n", 
	        languageNames.toString()));
	    }

	    return userInfo.toString();
	}
	
	// Function to respond to Facebook.Session state changes
	// For instance, change the UI
	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
	    if (state.isOpened()) {
	        Log.i(TAG, "Logged in...");
	        
	        if(mSession == null || isSessionChanged(session)) {
	        	mSession = session; // cache the session
	        	Log.i(TAG, "Access Token: " + session.getAccessToken());

	        	userInfoTextView.setVisibility(View.VISIBLE);

	        	// Request user data and show the results
	        	Request.newMeRequest(session, new GraphUserCallback() {

	        		@Override
	        		public void onCompleted(GraphUser user, Response response) {
	        			if(user != null) {
	        				userInfoTextView.setText(buildUserInfoDisplay(user));
	        			}
	        		}
	        	}).executeAsync();
	        }
	    } else if (state.isClosed()) {
	        Log.i(TAG, "Logged out...");
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
