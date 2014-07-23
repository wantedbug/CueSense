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
import com.facebook.model.GraphObject;
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

/**
 * Fragment that holds the list view of data from the user's Facebook profile 
 * @author vikasprabhu
 */
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
	
	// Facebook session cached
	private Session mSession;
	// Facebook UI lifecycle helper to complement Activity lifecycle methods
	UiLifecycleHelper mUiLifecycleHelper;
	// Callback to handle Session state change
	private Session.StatusCallback mFBCallback = new Session.StatusCallback() {
	    @Override
	    public void call(final Session session, final SessionState state, final Exception exception) {
	        onSessionStateChange(session, state, exception);
	    }
	};
	// Flag to check if data request has already been submitted to avoid duplication
	private boolean mFBRequestSubmitted = false;

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
		mSession = Session.getActiveSession();
	    if (mSession != null && mSession.isOpened() && !mFBRequestSubmitted) {
	        // Get the user's data
	        makeMeRequest(mSession);
	        mFBRequestSubmitted = true;
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
	    if (session != null && session.isOpened() && !mFBRequestSubmitted) {
	        // Get the user's data.
	        makeMeRequest(session);
	        mFBRequestSubmitted = true;
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
		/** PERSONAL DETAILS */
		/** 1. Add the list item header to the list view */
		Map<String, String> aboutMeGroupMap = new HashMap<String, String>();
		aboutMeGroupMap.put(ITEM_DATA, "About me");
		List<Map<String, String>> aboutMeList = new ArrayList<Map<String, String>>();
		int numChildrenAdded = 0;
		/** BIRTHDAY */
		if(session.isPermissionGranted("user_birthday")) {
			if(!user.getBirthday().isEmpty()) {
				/** 2. Get the children from the JSON response */
				// Child 1: the actual birthday as returned by Facebook
				Map<String, String> birthdayString = new HashMap<String, String>();
				birthdayString.put(ITEM_DATA, user.getBirthday());
				aboutMeList.add(birthdayString);
				++numChildrenAdded;
//				CueItem birthdayStringItem = new CueItem(0, InfoType.INFO_FACEBOOK, user.getBirthday(), true);
//				mFBList.add(birthdayStringItem);
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
				aboutMeList.add(birthdayMonth);
				++numChildrenAdded;
//				CueItem birthdayMonthItem = new CueItem(0, InfoType.INFO_FACEBOOK, "Child of " + bdayMonth, true);
//				mFBList.add(birthdayMonthItem);
			} else {
				Log.e(TAG, "getUserInfo() Birthday field empty");
			}
		} else {
			Log.i(TAG, "getUserInfo() Birthday permission NOT granted");
		}
		/** HOMETOWN */
		if(session.isPermissionGranted("user_hometown")) {
			JSONObject hometownJSON = (JSONObject)user.getProperty("hometown");
			if(hometownJSON.has("name")) {
				/** 2. Get the children from the JSON response */
				Map<String, String> hometownChild = new HashMap<String, String>();
				hometownChild.put(ITEM_DATA, hometownJSON.optString("name"));
				aboutMeList.add(hometownChild);
				++numChildrenAdded;
			} else {
				Log.e(TAG, "getUserInfo() Hometown field empty");
			}
		} else {
			Log.e(TAG, "getUserInfo() Hometown permission NOT granted");
		}
		/** 3. Add the list item's children to the list view */
		if(numChildrenAdded > 0) {
			mGroupData.add(aboutMeGroupMap);
			mChildData.add(aboutMeList);
		}
		
		/** BOOKS */
		if(session.isPermissionGranted("user_actions.books")) {
			// Life is going to easy if this permission is granted in the future
		} else {
			Log.i(TAG, "getUserInfo() Books permission NOT granted");
			// Try with a Graph path request
			Request.newGraphPathRequest(session, "/me/books.reads", mBooksResponseCallback).executeAsync();
		}
		
		/** MUSIC */
		// Try with permissions
		if(session.isPermissionGranted("user_actions.music")) {
			// Life is going to easy if this permission is granted in the future
		} else {
			Log.i(TAG, "getUserInfo() Music permission NOT granted");
			// Try with a Graph path request
			Request.newGraphPathRequest(session, "/me/music", mMusicResponseCallback).executeAsync();
		}
		
		/** INSPIRATIONAL PEOPLE */
		JSONArray peopleJSONArray = (JSONArray) user.getProperty("inspirational_people");
		if(peopleJSONArray.length() > 0 ) {
			/** 1. Add the list item header to the list view */
			Map<String, String> peopleGroupMap = new HashMap<String, String>();
			peopleGroupMap.put(ITEM_DATA, "Inspirational people");
			/** 2. Get the children from the JSON response */
			List<Map<String, String>> peopleList = new ArrayList<Map<String, String>>();
			numChildrenAdded = 0;
			for (int j = 0; j < peopleJSONArray.length(); ++j) {
				try {
					JSONObject personJSON = peopleJSONArray.getJSONObject(j);
					if(personJSON.has("name")) {
						Map<String, String> personChild = new HashMap<String, String>();
						personChild.put(ITEM_DATA, personJSON.getString("name"));
						peopleList.add(personChild);
						++numChildrenAdded;
					} else {
						Log.e(TAG, "getUserInfo() Inspirational people[" + j + "] no name");
					}
				} catch(JSONException e) {
					Log.e(TAG, "getUserInfo() Inspirational people[" + j + "] extraction error");
				}
			}
			/** 3. Add the list item's children to the list view */
			if(numChildrenAdded > 0) {
				mGroupData.add(peopleGroupMap);
				mChildData.add(peopleList);
			} else {
				Log.e(TAG, "getUserInfo() Inspirational people extraction error");
			}
		} else {
			Log.e(TAG, "getUserInfo() Inspirational people list empty");
		}
		
		/** FAVOURITE SPORTS TEAMS */
		JSONArray teamsJSONArray = (JSONArray) user.getProperty("favorite_teams");
		if(teamsJSONArray.length() > 0 ) {
			/** 1. Add the list item header to the list view */
			Map<String, String> teamsGroupMap = new HashMap<String, String>();
			teamsGroupMap.put(ITEM_DATA, "Sports teams");
			/** 2. Get the children from the JSON response */
			List<Map<String, String>> teamsList = new ArrayList<Map<String, String>>();
			numChildrenAdded = 0;
			for (int j = 0; j < teamsJSONArray.length(); ++j) {
				try {
					JSONObject teamJSON = teamsJSONArray.getJSONObject(j);
					if(teamJSON.has("name")) {
						Map<String, String> teamChild = new HashMap<String, String>();
						teamChild.put(ITEM_DATA, teamJSON.getString("name"));
						teamsList.add(teamChild);
						++numChildrenAdded;
					} else {
						Log.e(TAG, "getUserInfo() Sports teams[" + j + "] no name");
					}
				} catch(JSONException e) {
					Log.e(TAG, "getUserInfo() Sports teams[" + j + "] extraction error");
				}
			}
			/** 3. Add the list item's children to the list view */
			if(numChildrenAdded > 0) {
				mGroupData.add(teamsGroupMap);
				mChildData.add(teamsList);
			} else {
				Log.e(TAG, "getUserInfo() Sports teams extraction error");
			}
		} else {
			Log.e(TAG, "getUserInfo() Sports teams list empty");
		}
		
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
				numChildrenAdded = 0;
				// Add all schools as children
				for (int j = 0; j < schoolsJSON.length(); ++j) {
					try {
						JSONObject schoolJSON = schoolsJSON.getJSONObject(j);
						JSONObject school = schoolJSON.getJSONObject("school");
						Map<String, String> schoolChild = new HashMap<String, String>();
						schoolChild.put(ITEM_DATA, school.getString("name"));
						schoolsList.add(schoolChild);
						++numChildrenAdded;
					} catch(JSONException e) {
						Log.e(TAG, "getUserInfo() School[" + j + "] extraction error");
					}
				}
				/** 3. Add the list item's children to the list view */
				if(numChildrenAdded > 0) {
					mGroupData.add(educationGroupMap);
					mChildData.add(schoolsList);
				} else {
					Log.e(TAG, "getUserInfo() Education extraction error");
				}
			} else {
				Log.e(TAG, "getUserInfo() Education list empty");
			}
		} else {
			Log.i(TAG, "getUserInfo() Education permission NOT granted");
		}
		
		/** WORK HISTORY */
		if(session.isPermissionGranted("user_work_history")) {
			JSONArray companiesJSON = (JSONArray)user.getProperty("work");
			if(companiesJSON.length() > 0) {
				/** 1. Add the list item header to the list view */
				Map<String, String> workGroupMap = new HashMap<String, String>();
				workGroupMap.put(ITEM_DATA, "Work history");
				/** 2. Get the children from the JSON response */
				List<Map<String, String>> companiesList = new ArrayList<Map<String, String>>();
				numChildrenAdded = 0;
				// Add all companies as children
				for (int j = 0; j < companiesJSON.length(); ++j) {
					JSONObject companyJSON = companiesJSON.optJSONObject(j);
					try {
						JSONObject company = companyJSON.getJSONObject("employer");
						Map<String, String> companyChild = new HashMap<String, String>();
						companyChild.put(ITEM_DATA, company.getString("name"));
						companiesList.add(companyChild);
						++numChildrenAdded;
					} catch(JSONException e) {
						Log.e(TAG, "getUserInfo() Work [" + j + "] extraction error" + e);
					}
				}
				/** 3. Add the list item's children to the list view */
				if(numChildrenAdded > 0) {
					mGroupData.add(workGroupMap);
					mChildData.add(companiesList);
				} else {
					Log.e(TAG, "getUserInfo() Work extraction error");
				}
			} else {
				Log.e(TAG, "getUserInfo() Work list empty");
			}
		} else {
			Log.e(TAG, "getUserInfo() Work permission NOT granted");
		}
		
//		/** INTERESTS */
//		Request.newGraphPathRequest(session, "/me/interests", new Request.Callback() {
//			@Override
//			public void onCompleted(Response response) {
//				if(response.getError() == null) {
//					Log.i(TAG, response.toString());
//					// response.getRequestForPagedResults(PagingDirection.NEXT);
//					String rawResponse = response.getRawResponse();
//					if(!rawResponse.isEmpty()) {
//						
//					}
//				} else {
//					Log.e(TAG, "PATH error " + response.getError());
//				}
//			}
//		}).executeAsync();
		
//		/** LIKES */
//		JSONArray likesJSON = (JSONArray) user.getProperty("music");
//		if(likesJSON.length() > 0) {
//			/** Check for Professional sports team likes */
//
//			/** Check for Actor and Actor/director likes */
//		}
		
		/** LANGUAGES */
		JSONArray languages = (JSONArray) user.getProperty("languages");
		if (languages.length() > 0) {
			/** 1. Add the list item header to the list view */
			Map<String, String> languageGroupMap = new HashMap<String, String>();
			languageGroupMap.put(ITEM_DATA, "Languages");
			/** 2. Get the children from the JSON response */
			List<Map<String, String>> languagesList = new ArrayList<Map<String, String>>();
			numChildrenAdded = 0;
			for (int i = 0; i < languages.length(); i++) {
				JSONObject languageJSON = null;
				try {
					languageJSON = languages.getJSONObject(i);
				} catch(JSONException e) {
					Log.e(TAG, "getUserInfo() Language[" + i + "] extraction error" + e);
					continue;
				}
				Map<String, String> languageChild = new HashMap<String, String>();
				languageChild.put(ITEM_DATA, languageJSON.optString("name"));
				languagesList.add(languageChild);
				++numChildrenAdded;
			}
			/** 3. Add the list item's children to the list view */
			if(numChildrenAdded > 0) {
				mGroupData.add(languageGroupMap);
				mChildData.add(languagesList);
			} else {
				Log.e(TAG, "getUserInfo() Languages extraction error");
			}
		} else {
			Log.e(TAG, "getUserInfo() Languages list empty");
		}

	    // Notify that the list contents have changed
	    mAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Callback to handle /me/books.reads response
	 */
	private Request.Callback mBooksResponseCallback =  new Request.Callback() {
		@Override
		public void onCompleted(Response response) {
			if(response.getError() == null) {
				// response.getRequestForPagedResults(PagingDirection.NEXT);
				String rawResponse = response.getRawResponse();
				if(!rawResponse.isEmpty()) {
					GraphObject responseGraphObject = response.getGraphObject();
					JSONObject json = responseGraphObject.getInnerJSONObject();
					if(json.has("data")) {
						JSONArray booksJSON = null;
						try {
							booksJSON = json.getJSONArray("data");
						} catch(JSONException e) {
							Log.e(TAG, "getUserInfo() Books array extraction error " + e);
							return;
						}
						/** 1. Add the list item header to the list view */
						Map<String, String> booksGroupMap = new HashMap<String, String>();
						booksGroupMap.put(ITEM_DATA, "Books");
						/** 2. Get the children from the JSON response */
						List<Map<String, String>> booksChildrenList = new ArrayList<Map<String, String>>();
						int numChildrenAdded = 0;
						for(int i = 0; i < booksJSON.length(); ++i) {
							JSONObject bookData = null;
							try {
								bookData = booksJSON.getJSONObject(i).getJSONObject("data").getJSONObject("book");
								if(bookData.has("title")) {
									Map<String, String> bookChild = new HashMap<String, String>();
									bookChild.put(ITEM_DATA, bookData.getString("title"));
									booksChildrenList.add(bookChild);
									++numChildrenAdded;
								} else {
									Log.e(TAG, "getUserInfo() Books[" + i + "] no title");
								}
							} catch(JSONException e) {
								Log.e(TAG, "getUserInfo() Books[" + i + "] extraction error " + e);
							}
						}
						/** 3. Add the list item's children to the list view */
						if(numChildrenAdded > 0) {
							mGroupData.add(booksGroupMap);
							mChildData.add(booksChildrenList);
							// Notify list adapter here since this is an async task
							mAdapter.notifyDataSetChanged();
						} else {
							Log.e(TAG, "getUserInfo() Books extraction error ");
						}
					} else {
						Log.e(TAG, "getUserInfo() Books data empty");
					}
				} else {
					Log.e(TAG, "getUserInfo() Books path response empty");
				}
			} else {
				Log.e(TAG, "getUserInfo() Books path error " + response.getError());
			}
		}
	};
	
	/**
	 * Callback to handle /me/music response
	 */
	private Request.Callback mMusicResponseCallback = new Request.Callback() {
		@Override
		public void onCompleted(Response response) {
			if(response.getError() == null) {
				Log.i(TAG, response.toString());
				// response.getRequestForPagedResults(PagingDirection.NEXT);
				String rawResponse = response.getRawResponse();
				if(!rawResponse.isEmpty()) {
					GraphObject responseGraphObject = response.getGraphObject();
					JSONObject json = responseGraphObject.getInnerJSONObject();
					if(json.has("data")) {
						JSONArray musicJSON = null;
						try {
							musicJSON = json.getJSONArray("data");
						} catch(JSONException e) {
							Log.e(TAG, "getUserInfo() Music array extraction error " + e);
							return;
						}
						/** 1. Add the list item header to the list view */
						Map<String, String> musicGroupMap = new HashMap<String, String>();
						musicGroupMap.put(ITEM_DATA, "Music");
						/** 2. Get the children from the JSON response */
						List<Map<String, String>> musicChildrenList = new ArrayList<Map<String, String>>();
						int numChildrenAdded = 0;
						for(int i = 0; i < musicJSON.length(); ++i) {
							JSONObject musicData = null;
							try {
								musicData = musicJSON.getJSONObject(i);
								if(musicData.has("name")) {
									Map<String, String> musicChild = new HashMap<String, String>();
									musicChild.put(ITEM_DATA, musicData.getString("name"));
									musicChildrenList.add(musicChild);
									++numChildrenAdded;
								} else {
									Log.e(TAG, "getUserInfo() Music[" + i + "] no name");
								}
							} catch(JSONException e) {
								Log.e(TAG, "getUserInfo() Music[" + i + "] extraction error" + e);
							}
						}
						/** 3. Add the list item's children to the list view */
						if(numChildrenAdded > 0) {
							mGroupData.add(musicGroupMap);
							mChildData.add(musicChildrenList);
							// Notify list adapter here since this is an async task
							mAdapter.notifyDataSetChanged();
						} else {
							Log.e(TAG, "getUserInfo() Music extraction error");
						}
					} else {
						Log.e(TAG, "getUserInfo() Music list null");
					}
				} else {
					Log.e(TAG, "getUserInfo() Music path response empty");
				}
			} else {
				Log.e(TAG, "Music path error " + response.getError());
			}
		}
	};
	
}
