/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
		"public_profile", // id, name, gender, age range, locale
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
	List<Map<String, String>> mGroupData = new ArrayList<Map<String, String>>();
	List<List<Map<String, String>>> mChildData = new ArrayList<List<Map<String, String>>>();
		
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
	    if(mGroupData.isEmpty()) getData();
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
	 * Make the /me request to the Facebook Graph API and get user info on
	 * positive response
	 * @param session
	 * This can turn out to be a time-consuming call and is therefore performed
	 * in a RequestAsyncTask on the UI thread
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
    			Log.i(TAG, "getUserInfo() " + fbPermissions[i] + " granted");
    			switch(i) {
    			case 0: // public_profile
    				break;
    			case 1: // user_actions.books
    				break;
    			case 2: // user.actions_movies
    				break;
    			case 3: // user_actions.music
    				break;
    			case 4: // user_tagged_places
    				break;
    			case 5: { // user_birthday
    				if(!user.getBirthday().isEmpty()) {
    					/** 1. Add the list item header to the list view */
    					Map<String, String> bdayGroupMap = new HashMap<String, String>();
    					bdayGroupMap.put(ITEM_DATA, fbPermissionStrings[i]);
    					mGroupData.add(bdayGroupMap);
    					/** 2. Get the children from the JSON response */
    					List<Map<String, String>> bdayChildrenList = new ArrayList<Map<String, String>>();
    					// Child 1: the actual birthday as returned by Facebook
    					Map<String, String> birthdayString = new HashMap<String, String>();
    					birthdayString.put(ITEM_DATA, user.getBirthday());
    					bdayChildrenList.add(birthdayString);
    					// Child 2: the birthday month
    					int month = Integer.parseInt(user.getBirthday().substring(0, 2));
    					String bdayMonth = "";
    					switch(month) {
    					case 1: bdayMonth = "January"; break;
    					case 2: bdayMonth = "February"; break;
    					case 3: bdayMonth = "March"; break;
    					case 4: bdayMonth = "April"; break;
    					case 5: bdayMonth = "May"; break;
    					case 6: bdayMonth = "June"; break;
    					case 7: bdayMonth = "July"; break;
    					case 8: bdayMonth = "August"; break;
    					case 9: bdayMonth = "September"; break;
    					case 10: bdayMonth = "October"; break;
    					case 11: bdayMonth = "November"; break;
    					case 12: bdayMonth = "December"; break;
    					}
    					Map<String, String> birthdayMonth = new HashMap<String, String>();
    					birthdayMonth.put(ITEM_DATA, "Child of " + bdayMonth);
    					bdayChildrenList.add(birthdayMonth);
    					/** 3. Add the list item's children to the list view */
    					mChildData.add(bdayChildrenList);
    				} else {
    					Log.e(TAG, "getUserInfo() Birthday field empty");
    				}
    			}
    				break;
    			case 6: // user_about_me
    				break;
    			case 7: { // user_education_history
    				JSONArray schoolsJSON = (JSONArray)user.getProperty("education");
    				if(schoolsJSON.length() > 0) {
    					/** 1. Add the list item header to the list view */
    					Map<String, String> educationGroupMap = new HashMap<String, String>();
    					educationGroupMap.put(ITEM_DATA, fbPermissionStrings[i]);
    					/** 2. Get the children from the JSON response */
    					List<Map<String, String>> schoolsList = new ArrayList<Map<String, String>>();
    					int numChildrenAdded = 0;
    					// Add all schools as children
    					for (int j = 0; j < schoolsJSON.length(); ++j) {
    						JSONObject schoolJSON = schoolsJSON.optJSONObject(j);
    						try {
    							JSONObject school = schoolJSON.getJSONObject("school");
    							Map<String, String> schoolChild = new HashMap<String, String>();
            					schoolChild.put(ITEM_DATA, school.optString("name"));
            					schoolsList.add(schoolChild);
            					++numChildrenAdded;
    						} catch(JSONException e) {
    							Log.e(TAG, "getUserInfo() school error");
    						}
    					}
    					/** 3. Add the list item's children to the list view */
    					if(numChildrenAdded > 0) {
    						mGroupData.add(educationGroupMap);
    						mChildData.add(schoolsList);
    					}
    				} else {
    					Log.e(TAG, "getUserInfo() Education list empty");
    				}
    			}
    				break;
    			case 8: // user_work_history
    				break;
    			case 9: // user_hometown
    				break;
    			case 10: // user_activities
    				break;
    			case 11: // user_interests
    				break;
    			case 12: // user_likes
    				break;
    			default: Log.e(TAG, "getUserInfo() - something's wrong with the index");
    				break;
    			}
	    	} else {
    			Log.i(TAG, "getUserInfo() " + fbPermissions[i] + " NOT granted");
    			switch(i) {
    			case 0: // public_profile
    				break;
    			case 1: // user_actions.books
    	        	Request.newGraphPathRequest(session, "/me/books.reads", new Request.Callback() {
    					@Override
    					public void onCompleted(Response response) {
    						if(response.getError() == null) {
    							Log.i(TAG, response.toString());
    							// response.getRequestForPagedResults(PagingDirection.NEXT);
    						} else {
    							Log.e(TAG, "PATH error " + response.getError());
    						}
    					}
    				}).executeAsync();
    				break;
    			case 2: // user.actions_movies
    			case 3: // user_actions.music
    			case 4: // user_tagged_places
    			case 5: // user_birthday
    			case 6: // user_about_me
    			case 7: // user_education_history
    			case 8: // user_work_history
    			case 9: // user_hometown
    			case 10: // user_activities
    			case 11: // user_interests
    			case 12: // user_likes
    				break;
    			default: Log.e(TAG, "getUserInfo() - something's wrong with the index");
    				break;
    			}
    		}
		}
	    
	    // Notify that the list contents have changed
	    mAdapter.notifyDataSetChanged();
	}
}
