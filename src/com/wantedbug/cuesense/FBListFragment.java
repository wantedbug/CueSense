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
		// Go through all the data returned in the /me request
		/** BIRTHDAY */
		if(session.isPermissionGranted("user_birthday")) {
			if(!user.getBirthday().isEmpty()) {
				/** 1. Add the list item header to the list view */
				Map<String, String> bdayGroupMap = new HashMap<String, String>();
				bdayGroupMap.put(ITEM_DATA, "Birthday");
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
		} else {
			Log.i(TAG, "getUserInfo() Birthday permission NOT granted");
		}
		
		/** BOOKS */
		Request.newGraphPathRequest(session, "/me/books.reads", new Request.Callback() {
			@Override
			public void onCompleted(Response response) {
				if(response.getError() == null) {
					Log.i(TAG, response.toString());
					// response.getRequestForPagedResults(PagingDirection.NEXT);
					String rawResponse = response.getRawResponse();
					if(!rawResponse.isEmpty()) {
						
					} else {
						Log.i(TAG, "getUserInfo() Books path response empty");
					}
				} else {
					Log.e(TAG, "Books path error " + response.getError());
				}
			}
		}).executeAsync();
		
		/** MUSIC */
		JSONArray likesJSON = (JSONArray) user.getProperty("music");
		// Try with permissions
		if(session.isPermissionGranted("user_actions.music")) {
			
		} else {
			Log.i(TAG, "getUserInfo() Music permission NOT granted");
			// Check with a path request
			if(likesJSON.length() > 0) {
				Request.newGraphPathRequest(session, "/me/music", new Request.Callback() {
					@Override
					public void onCompleted(Response response) {
						if(response.getError() == null) {
							Log.i(TAG, response.toString());
							// response.getRequestForPagedResults(PagingDirection.NEXT);
							String rawResponse = response.getRawResponse();
							if(!rawResponse.isEmpty()) {
								
							} else {
								Log.i(TAG, "getUserInfo() Music path response empty");
								// Look in the list of likes
								
							}
						} else {
							Log.e(TAG, "Music path error " + response.getError());
						}
					}
				}).executeAsync();
			}
		}
		
		/** INSPIRATIONAL PEOPLE */
		
		/** ACTORS/DIRECTORS */
		
		/** EDUCATION */
		if(session.isPermissionGranted("user_education_history")) {
			JSONArray schoolsJSON = (JSONArray)user.getProperty("education");
			if(schoolsJSON.length() > 0) {
				/** 1. Add the list item header to the list view */
				Map<String, String> educationGroupMap = new HashMap<String, String>();
				educationGroupMap.put(ITEM_DATA, "Education");
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
		} else {
			Log.i(TAG, "getUserInfo() Education permission NOT granted");
		}
		
		/** WORK HISTORY */
		JSONArray companiesJSON = (JSONArray)user.getProperty("work");
		if(companiesJSON.length() > 0) {
			/** 1. Add the list item header to the list view */
			Map<String, String> workGroupMap = new HashMap<String, String>();
			workGroupMap.put(ITEM_DATA, "Work history");
			/** 2. Get the children from the JSON response */
			List<Map<String, String>> companiesList = new ArrayList<Map<String, String>>();
			int numChildrenAdded = 0;
			// Add all companies as children
			for (int j = 0; j < companiesJSON.length(); ++j) {
				JSONObject companyJSON = companiesJSON.optJSONObject(j);
				try {
					JSONObject company = companyJSON.getJSONObject("employer");
					Map<String, String> companyChild = new HashMap<String, String>();
					companyChild.put(ITEM_DATA, company.optString("name"));
					companiesList.add(companyChild);
					++numChildrenAdded;
				} catch(JSONException e) {
					Log.e(TAG, "getUserInfo() work error");
				}
			}
			/** 3. Add the list item's children to the list view */
			if(numChildrenAdded > 0) {
				mGroupData.add(workGroupMap);
				mChildData.add(companiesList);
			}
		} else {
			Log.e(TAG, "getUserInfo() Education list empty");
		}
		
		/** INTERESTS */
		Request.newGraphPathRequest(session, "/me/interests", new Request.Callback() {
			@Override
			public void onCompleted(Response response) {
				if(response.getError() == null) {
					Log.i(TAG, response.toString());
					// response.getRequestForPagedResults(PagingDirection.NEXT);
					String rawResponse = response.getRawResponse();
					if(!rawResponse.isEmpty()) {
						
					}
				} else {
					Log.e(TAG, "PATH error " + response.getError());
				}
			}
		}).executeAsync();
		
		/** PERSONAL DETAILS */
		JSONObject hometownJSON = (JSONObject)user.getProperty("hometown");
		if(hometownJSON.has("name")) {
			/** 1. Add the list item header to the list view */
			Map<String, String> hometownGroupMap = new HashMap<String, String>();
			hometownGroupMap.put(ITEM_DATA, "Hometown");
			mGroupData.add(hometownGroupMap);
			/** 2. Get the children from the JSON response */
			List<Map<String, String>> hometownList = new ArrayList<Map<String, String>>();
			Map<String, String> hometownChild = new HashMap<String, String>();
			hometownChild.put(ITEM_DATA, hometownJSON.optString("name"));
			hometownList.add(hometownChild);
			/** 3. Add the list item's children to the list view */
			mChildData.add(hometownList);
		}
		
		/** LIKES */
		if(likesJSON.length() > 0) {
			/** Check for Musician/band likes */

			/** Check for Professional sports team likes */

			/** Check for Actor and Actor/director likes */
		}
		
		/** LANGUAGES */
		JSONArray languages = (JSONArray) user.getProperty("languages");
		if (languages.length() > 0) {
			/** 1. Add the list item header to the list view */
			Map<String, String> languageGroupMap = new HashMap<String, String>();
			languageGroupMap.put(ITEM_DATA, "Languages");
			/** 2. Get the children from the JSON response */
			List<Map<String, String>> languagesList = new ArrayList<Map<String, String>>();
			int numChildrenAdded = 0;
			for (int i=0; i < languages.length(); i++) {
				JSONObject languageJSON = languages.optJSONObject(i);
				Map<String, String> languageChild = new HashMap<String, String>();
				languageChild.put(ITEM_DATA, languageJSON.optString("name"));
				languagesList.add(languageChild);
				++numChildrenAdded;
			}
			/** 3. Add the list item's children to the list view */
			if(numChildrenAdded > 0) {
				mGroupData.add(languageGroupMap);
				mChildData.add(languagesList);
			}
		}
		
	    // Notify that the list contents have changed
	    mAdapter.notifyDataSetChanged();
	}
}
