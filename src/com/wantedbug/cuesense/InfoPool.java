/**
 * Copyright (C) 2014 Tampere University of Technology
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wantedbug.cuesense.MainActivity.InfoType;

/**
 * This class holds display-able data in a structure.
 * @author vikasprabhu
 * This class is a simple thread-safe singleton.
 */
public class InfoPool {
	// Debugging
	private static final String TAG = "InfoPool";
	
	/**
	 * Constants
	 */
	private static final int INIT_SIZE = 50;
	
	// Name identifiers for the JSONArray
	public static final String JSON_DISTANCE_NAME = "dist";
	public static final String JSON_ARRAY_NAME = "d";
	public static final String JSON_TWITTERSCREENNAME_NAME = "tSN";
	
	/**
	 * Members
	 */
	// Static singleton instance
	public static final InfoPool INSTANCE = new InfoPool();
	
	// List for newly added CueSense cues - highest priority
	private ArrayList<CueItem> mNewCuesList = new ArrayList<CueItem>();
	
	// List for cues matched with another user - next highest priority
	private List<CueItem> mMatchedCuesList = new ArrayList<CueItem>(INIT_SIZE);
	
	// Global list for everything else - lowest priority
	private ArrayList<CueItem> mGlobalList = new ArrayList<CueItem>(INIT_SIZE);
	
	// Lists that contain appropriate CueItems for the respective distance levels
	// See MainActivity.BT_RSSI_NEAR etc.
	// Note 1: These lists are converted to JSONArrays for transmitting.
	// Note 2: Instead of creating lists while transmitting, we instead go for creating
	// them earlier (i.e. as and when Cues are added/deleted/modified) to make the
	// transmission quick.
	// Note 3: The decision about which kind of information goes to which
	// list (i.e. which information is transmitted at which range)
	// has to be implemented across the package. Currently, this is how it's done:
	// 1. User-added Cues are transmitted at the far range
	// 2. Facebook and Twitter Cues are transmitted at the near range
	private List<CueItem> mNearList = new ArrayList<CueItem>();
	private List<CueItem> mFarList = new ArrayList<CueItem>();
	
	// Thread to perform matching
	private MatchThread mMatchThread = null;
	
	private CueItem mPrevItem = new CueItem(-1, InfoType.INFO_SENTINEL, "", false);
	
	/**
	 * Private c'tor to defeat instantiation
	 */
	private InfoPool() { }
	
	/**
	 * Adds a new Cue to the pool
	 * @param item
	 * @return
	 */
	public synchronized void addCueItem(CueItem item) {
		Log.d(TAG, "add: " + item.type() + "," + item.data());
		// Add to appropriate list
		if(item.type() == InfoType.INFO_CUESENSE) {
			mNewCuesList.add(0, item);
		} else {
			mGlobalList.add(item);
		}
		// Add to data package
		onCueAdded(item);
	}
	
	/**
	 * Clears the InfoPool
	 */
	public void clear() {
		Log.d(TAG, "clear()");
		// Clear CueItem lists
		mGlobalList.clear();
		mNewCuesList.clear();
		clearMatchedCues();
		// Clear distance level lists
		mNearList.clear();
		mFarList.clear();
		// Cancel any ongoing matching operation, if any
		stopMatchThread();
	}
	
	/**
	 * Deletes matched cues
	 */
	public synchronized void clearMatchedCues() {
		Log.d(TAG, "clearMatchedCues()");
		mMatchedCuesList.clear();
	}

	/**
	 * Adds a list of InfoItems to the InfoPool
	 * @param items
	 * Called on app start
	 */
	public synchronized void addCueItems(List<CueItem> items) {
		Log.d(TAG, "addCueItems(): " + items.size());
		if(items.isEmpty()) {
			return;
		}
		// Add to the global list
		mGlobalList.addAll(items);
		// Add to the data package
		onCuesAdded(items);
	}
	
	/**
	 * Adds a list of InfoItems to the InfoPool at the top of other items
	 * of the same type.
	 * @param items
	 * @param type
	 */
	public synchronized void addCueItemsToTop(List<CueItem> items, InfoType type) {
		Log.d(TAG, "addCueItems(): " + items.size() + "," + type);
		
		if(items.isEmpty()) {
			return;
		}
		
		// Add to the global list
		int pos = 0;
		for(; pos < mGlobalList.size(); ++pos) {
			if(mGlobalList.get(pos).type().equals(type)) {
				break;
			}
		}
		mGlobalList.addAll(pos, items);
		// Add to the data package
		onCuesAdded(items);
	}
	
	/**
	 * Deletes a CueItem from the lists
	 * @param item
	 * The CueItems are matched according to their id
	 */
	public synchronized void deleteCueItem(CueItem item) {
		Log.d(TAG, "deleteCueItem(): " + item.id());
		// Look in new list first
		boolean wasInNewList = false;
		Iterator<CueItem> it = mNewCuesList.iterator();
		while(it.hasNext()) {
			CueItem temp = it.next();
			if(item.id() == temp.id() ||
					(item.data().equals(temp.data()) && item.type().equals(temp.type())) ) {
				Log.i(TAG, "removing from new list " + item.type() + "," + item.data());
				// Remove from the list
				it.remove();
				// Remove from the data package
				onCueDeleted(item);
				wasInNewList = true;
				break;
			}
		}
		
		// Check in matched list as well
		it = mMatchedCuesList.iterator();
		while(it.hasNext()) {
			CueItem temp = it.next();
			if(item.id() == temp.id() ||
					(item.data().equals(temp.data()) && item.type().equals(temp.type())) ) {
				Log.i(TAG, "removing from matched list " + item.type() + "," + item.data());
				// Remove from the list
				it.remove();
				// Remove from the data package
				onCueDeleted(item);
				break;
			}
		}
		
		// No need to look in global list if found in new list
		if(wasInNewList) {
			return;
		}
		
		it = mGlobalList.iterator();
		while(it.hasNext()) {
			CueItem temp = it.next();
			if(item.id() == temp.id() ||
					(item.data().equals(temp.data()) && item.type().equals(temp.type())) ) {
				Log.i(TAG, "removing from global list " + item.type() + "," + item.data());
				// Remove from the list
				it.remove();
				// Remove from the data package
				onCueDeleted(item);
				break;
			}
		}
	}
	
	/**
	 * Updates a CueItem in the lists
	 * @param item
	 * The CueItems are matched according to their id
	 */
	public synchronized void updateCueItem(CueItem item) {
		Log.d(TAG, "updating " + item.type() + "," + item.data());
		
		// Search in new list, and return if found
		boolean wasInNewList = false;
		for(CueItem it : mNewCuesList) {
			if(it.id() == item.id() ||
					(item.data().equals(it.data()) && item.type().equals(it.type())) ) {
				it.setType(item.type());
				it.setData(item.data());
				it.setChecked(item.isChecked());
				if(!it.isChecked()) {
					mGlobalList.add(it);
					mNewCuesList.remove(it);
				}
				onCueUpdated(item);
				wasInNewList = true;
				break;
			}
		}

		// Search in matched list as well
		for(CueItem it : mMatchedCuesList) {
			if(it.id() == item.id() ||
					(item.data().equals(it.data()) && item.type().equals(it.type())) ) {
				it.setType(item.type());
				it.setData(item.data());
				it.setChecked(item.isChecked());
				onCueUpdated(item);
				break;
			}
		}
		
		// If not in new list, then search in global list
		if(wasInNewList) {
			return;
		}
		for(CueItem it : mGlobalList) {
			if(it.id() == item.id() ||
					(item.data().equals(it.data()) && item.type().equals(it.type())) ) {
				it.setType(item.type());
				it.setData(item.data());
				it.setChecked(item.isChecked());
				onCueUpdated(item);
				break;
			}
		}
	}
	
	/**
	 * Deletes all entries of a particular InfoType
	 * @param type
	 */
	public synchronized void deleteType(InfoType type) {
		Log.d(TAG, "deleteType(): " + type.toString());
		
		// Delete from new list
		Iterator<CueItem> it = mNewCuesList.iterator();
		while(it.hasNext()) {
			CueItem item = it.next();
			if(item.type().equals(type)) {
				Log.i(TAG, "removing from new list " + item.type() + "," + item.data());
				it.remove();
				break;
			}
		}
		
		// Delete from matched list
		it = mMatchedCuesList.iterator();
		while(it.hasNext()) {
			CueItem item = it.next();
			if(item.type().equals(type)) {
				Log.i(TAG, "removing from matched list " + item.type() + "," + item.data());
				it.remove();
			}
		}

		// Delete from global list
		it = mGlobalList.iterator();
		while(it.hasNext()) {
			CueItem item = it.next();
			if(item.type().equals(type)) {
				Log.i(TAG, "removing from global list " + item.type() + "," + item.data());
				it.remove();
			}
		}
		
		// Delete from the neighbour lists as well
		it = mNeighbourNearList.iterator();
		while(it.hasNext()) {
			CueItem item = it.next();
			if(item.type().equals(type)) {
				it.remove();
			}
		}
		it = mNeighbourFarList.iterator();
		while(it.hasNext()) {
			CueItem item = it.next();
			if(item.type().equals(type)) {
				it.remove();
			}
		}
	}
	
	/**
	 * Returns true if there is any data in the InfoPool; false if otherwise
	 * @return
	 */
	public boolean hasNext() {
		Log.d(TAG, "getNext()");
		if(mNewCuesList.isEmpty() && mGlobalList.isEmpty() && mMatchedCuesList.isEmpty()) {
			Log.i(TAG, "getNext() lists empty");
			return false;
		}
		return true;
	}
	
	private void updatePrevItem(CueItem ret) {
		mPrevItem = null;
		mPrevItem = ret;
	}
	
	/**
	 * Gets next Cue from the appropriate list
	 * @return
	 * Returns only checked Cues. Priority order for the search:
	 * 1. New cues list
	 * 2. Matched list
	 * 3. Global list
	 * If there is any data in the matched list, the global list will never
	 * be searched.
	 */
	public synchronized CueItem getNext() {
		Log.d(TAG, "getNext()");
		
		int count = 0;
		final int NUMTRIES = 100;
		
		// Return an item that the user entered
		// and add the same to the end of the global list
		if(!mNewCuesList.isEmpty()) {
			boolean found = false;
			CueItem ret = new CueItem(-1, InfoType.INFO_SENTINEL, "", false); // to make the silly Android compiler happy
			while(!found && count < NUMTRIES) {
				ret = mNewCuesList.get(new Random().nextInt(mNewCuesList.size()));
				if(ret.isChecked() && !ret.data().equals(mPrevItem.data())) {
					found = true;
					mGlobalList.add(ret);
					boolean removed = mNewCuesList.remove(ret);
					Log.i(TAG, "item removed = " + removed + " from mNewCuesList " + ret.data());
					Log.i(TAG, "getNext() from new list " + ret.data());
					updatePrevItem(ret);
					return ret;
				}
				++count;
			}
		}
		
		// If there's a matched Cue, return that
		count = 0;
		if(!mMatchedCuesList.isEmpty()) {
			boolean found = false;
			CueItem ret = new CueItem(-1, InfoType.INFO_SENTINEL, "", false); // to make the silly Android compiler happy
			while(!found && count < NUMTRIES) {
				ret = mMatchedCuesList.get(new Random().nextInt(mMatchedCuesList.size()));
				if(ret.isChecked() && !ret.data().equals(mPrevItem.data())) {
					found = true;
					Log.i(TAG, "getNext() from matched list " + ret.data());
					updatePrevItem(ret);
					return ret;
				}
				++count;
			}
		}
		
		// If not, then return an item from the global list
		boolean found = false;
		count = 0;
		CueItem ret = new CueItem(-1, InfoType.INFO_SENTINEL, "", false); // to make the silly Android compiler happy
		while(!found && count < NUMTRIES) {
			ret = mGlobalList.get(new Random().nextInt(mGlobalList.size()));
			if(ret.isChecked() && !ret.data().equals(mPrevItem.data())) {
				found = true;
				Log.i(TAG, "getNext() from global list " + ret.data());
				updatePrevItem(ret);
				return ret;
			}
			++count;
		}
		
		// If all the lists are empty
		Log.i(TAG, "getNext() lists empty");
		return new CueItem(-1, InfoType.INFO_SENTINEL, "CueSense", true);
	}
	
	/**
	 * Adds a Cue to the appropriate data package
	 * @param item
	 * The Cue is not added if it's unchecked.
	 */
	private void onCueAdded(CueItem item) {
//		Log.d(TAG, "onCueAdded()");
		
		if(!item.isChecked()) return;
		
		switch(item.type()) {
		case INFO_FACEBOOK: 
			mNearList.add(item);
			break;
		case INFO_TWITTER: 
			mNearList.add(item);
			break;
		case INFO_CUESENSE:
			mFarList.add(item);
			break;
		case INFO_SENTINEL:
		default:
			Log.e(TAG, "onCueAdded: Something wrong with indexes");
			break;
		}
	}
	
	/**
	 * Adds the Cues to the appropriate data package
	 * @param items
	 * Only checked Cues are added to the data package
	 */
	private void onCuesAdded(List<CueItem> items) {
//		Log.d(TAG, "onCuesAdded()");
		for(CueItem item : items) {
			if(item.isChecked()) onCueAdded(item);
		}
	}
	
	/**
	 * Deletes a Cue from the appropriate data package
	 * @param item
	 */
	private void onCueDeleted(CueItem item) {
//		Log.d(TAG, "onCueDeleted()");
		switch(item.type()) {
		case INFO_CUESENSE: {
			Iterator<CueItem> it = mNearList.iterator();
			while(it.hasNext()) {
				CueItem temp = it.next();
				if(item.id() == temp.id() ||
						(item.data().equals(temp) && item.type().equals(temp.type())) ) {
					it.remove();
					break;
				}
			}
		}
			break;
		case INFO_FACEBOOK: {
			Iterator<CueItem> it = mNearList.iterator();
			while(it.hasNext()) {
				CueItem temp = it.next();
				if(item.id() == temp.id() ||
						(item.data().equals(temp) && item.type().equals(temp.type())) ) {
					it.remove();
					break;
				}
			}
		}
			break;
		case INFO_TWITTER: {
			Iterator<CueItem> it = mFarList.iterator();
			while(it.hasNext()) {
				CueItem temp = it.next();
				if(item.id() == temp.id() ||
						(item.data().equals(temp) && item.type().equals(temp.type())) ) {
					it.remove();
					break;
				}
			}
		}
			break;
		case INFO_SENTINEL:
		default:
			Log.e(TAG, "onCueDeleted: Something wrong with indexes");
			break;
		}
	}
	
	/**
	 * Modifies a Cue from the appropriate data package
	 * @param item
	 * A Cue may end up being deleted from the data package if the user has unchecked
	 * it in the app. On the same lines, it can also end up being added back if it
	 * was previously unchecked and gets checked again.
	 */
	private void onCueUpdated(CueItem item) {
		switch(item.type()) {
		case INFO_CUESENSE:
			for(CueItem it : mNearList) {
				if(it.id() == item.id() ||
						(item.data().equals(it.data()) && item.type().equals(it.type())) ) {
					it.setData(item.data());
					break;
				}
			}
			break;
		case INFO_FACEBOOK:
			for(CueItem it : mNearList) {
				if(it.id() == item.id() ||
						(item.data().equals(it.data()) && item.type().equals(it.type())) ) {
					it.setData(item.data());
					break;
				}
			}
			break;
		case INFO_TWITTER:
			for(CueItem it : mFarList) {
				if(it.id() == item.id() ||
						(item.data().equals(it.data()) && item.type().equals(it.type())) ) {
					it.setData(item.data());
					break;
				}
			}
			break;
		case INFO_SENTINEL:
		default:
			Log.e(TAG, "onCueUpdated: Something wrong with indexes");
			break;
		}
	}
	
	/**
	 * Returns a JSONObject constructed from the appropriate distance level list
	 * @param distanceRange
	 * @return
	 */
	public JSONObject getData(int distanceRange) {
		Log.d(TAG, "getData()");
		
		// Construct a JSONArray from the appropriate Cues list. Then put that
		// into a JSONObject and return that.
		// If the respective list is empty, return null.
		JSONObject dataObject = null;
		JSONArray dataArray = null;
		switch(distanceRange) {
		case MainActivity.DISTANCE_NEAR:
			if(mNearList != null && mNearList.isEmpty()) {
				return null;
			}
			dataArray = new JSONArray();
			for(CueItem item : mNearList) {
				JSONObject itemJSON = item.toJSONObject();
				if(itemJSON != null) dataArray.put(itemJSON);
			}
			String twitterScreenName = TwitterUtils.INSTANCE.getScreenName();
			dataObject = new JSONObject();
			try {
				if(!twitterScreenName.isEmpty())
					dataObject.put(JSON_TWITTERSCREENNAME_NAME, twitterScreenName);
				dataObject.put(JSON_DISTANCE_NAME, distanceRange);
				dataObject.put(JSON_ARRAY_NAME, dataArray);
			} catch(JSONException e) {
				Log.e(TAG, "DISTANCE_NEAR JSON creation error " + e);
				return null;
			}
			return dataObject;
		case MainActivity.DISTANCE_FAR:
			if(mFarList != null && mFarList.isEmpty()) {
				return null;
			}
			dataArray = new JSONArray();
			for(CueItem item : mFarList) {
				JSONObject itemJSON = item.toJSONObject();
				if(itemJSON != null) dataArray.put(itemJSON);
			}
			if(dataArray.length() == 0) return null;
			dataObject = new JSONObject();
			try {
				dataObject.put(JSON_DISTANCE_NAME, distanceRange);
				if(dataArray.length() != 0) dataObject.put(JSON_ARRAY_NAME, dataArray);
			} catch(JSONException e) {
				Log.e(TAG, "DISTANCE_FAR JSON creation error " + e);
				return null;
			}
			return dataObject;
		case MainActivity.DISTANCE_OUTOFRANGE:
		default:
			Log.e(TAG, "getData() ruh-roh");
			break;
		}
		return null;
	}
	
	/**
	 * Stops ongoing matching operation
	 */
	private synchronized void stopMatchThread() {
		if(mMatchThread != null) {
			mMatchThread.cancel();
			mMatchThread = null;
		}
	}
	
	/**
	 * Extracts and matches received with what we currently have
	 * @param data
	 * Performs basic approximate string matching 
	 */
	public synchronized void matchData(String data) {
		Log.d(TAG, "matchData()");
		
		// Cancel any ongoing matching operation and start a new one
		stopMatchThread();
		mMatchThread = new MatchThread(data);
		mMatchThread.start();
	}
	
	/**
	 * Tries to fix problems with the raw data received, if any
	 * @param rawData
	 * @return
	 * Incoming raw JSON data may be incomplete due to problems in the transmissions
	 */
	public String fixRawData(String rawData) {
		JSONObject obj = null;
		boolean recovered = false;
		boolean triedAppend = false;
		String temp = rawData;
		String termCharSeq = "\"}]}"; // this is a valid termination char sequence
		
		// Try to recover the incomplete data by removing one char from the end at a time,
		// appending a valid termination character sequence and trying to construct a
		// JSONObject
		while(!recovered) {
			try {
				obj = null;
				obj = new JSONObject(temp);
				recovered = true;
				rawData = temp;
			} catch(JSONException e) {
				Log.d(TAG, "doesn't work: " + rawData);
				if(rawData.isEmpty()) {
					rawData = "Couldn't recover";
					break;
				}
				if(!triedAppend) {
					rawData = rawData + termCharSeq;
					temp = new String(rawData);
					triedAppend = true;
				} else {
					rawData = rawData.substring(0, rawData.length() - 1);
					temp = new String(rawData);
					temp = temp + termCharSeq;
				}
			}
		}
		
		return rawData;
	}
	
	private List<CueItem> mNeighbourNearList = new ArrayList<CueItem>();
	private List<CueItem> mNeighbourFarList = new ArrayList<CueItem>();
	
	/**
	 * Thread to perform matching
	 * @author vikasprabhu
	 */
	private class MatchThread extends Thread {
		// Debugging
		private static final String TAG = "MatchThread";
		
		/**
		 * Constants
		 */
		private static final double THRESHOLD_NEAR = 0.8;
		private static final double THRESHOLD_FAR = 0.5;
		
		/**
		 * Members
		 */
		//
		volatile boolean mRunning = true;
		// Raw JSON data
		private final String mRawData;
		// List of CueItems constructed from above JSON data
		private List<CueItem> mNeighbourItems;
		// Distance range received
		private int mDistance = MainActivity.DISTANCE_OUTOFRANGE;
		// Twitter screen name of the nearby user
		private String mTargetUserScreenName = "";
		//
		private boolean mHasContent = false;
		
		public MatchThread(String data) {
			Log.d(TAG, "create MatchThread " + data.length());
			mRawData = fixRawData(data);
			mNeighbourItems = new ArrayList<CueItem>();
		}
		
		public void run() {
			Log.d(TAG, "run()");
			if(!mRunning) return;
			// Extract data from received JSON
			try {
				JSONObject root = new JSONObject(mRawData);
				if(root.has(JSON_DISTANCE_NAME))
					mDistance = root.getInt(JSON_DISTANCE_NAME);
				if(root.has(JSON_TWITTERSCREENNAME_NAME))
					mTargetUserScreenName = root.getString(JSON_TWITTERSCREENNAME_NAME);
				JSONArray itemsArray = null;
				if(root.has(JSON_ARRAY_NAME)) {
					mHasContent = true;
					itemsArray = root.getJSONArray(JSON_ARRAY_NAME);
					for(int i = 0; mRunning && i < itemsArray.length(); ++i) {
						JSONObject itemJSON = itemsArray.getJSONObject(i);
						InfoType type = InfoType.toInfoType(itemJSON.optInt(CueItem.JSON_TAG_TYPE));
						String data = itemJSON.optString(CueItem.JSON_TAG_DATA);
						if(data != null && type != null) {
							CueItem item = new CueItem(-1, type, data, true);
							mNeighbourItems.add(item);
						}
					}
				} else {
					mHasContent = false;
				}
			} catch (JSONException e) {
				Log.e(TAG, "JSONObject creation/extraction error " + e);
				return;
			}
			
			// We just came out of a potentially long running JSON creation operation
			// Check if we're canceled and then proceed
			if(mRunning) {
				match();
			} else {
				return;
			}
		}
		
		/**
		 * Calls match-making function with appropriate threshold values w.r.t.
		 * distance range involved
		 */
		private void match() {
			Log.i(TAG, "match() mHasContent=" + mHasContent);
			// Clear any matches previously generated
			clearMatchedCues();

			if(mHasContent) {
				switch(mDistance) {
				case MainActivity.DISTANCE_NEAR:
					mNeighbourNearList.clear();
					mNeighbourNearList.addAll(mNeighbourItems);
					match(mNearList, mNeighbourItems, THRESHOLD_NEAR);
					// Get common followings tweets if we have the target user's screen name
					if(!mTargetUserScreenName.isEmpty())
						getCommonTweets();
					break;
				case MainActivity.DISTANCE_FAR:
					mNeighbourFarList.clear();
					mNeighbourFarList.addAll(mNeighbourItems);
					// Match data
					match(mFarList, mNeighbourItems, THRESHOLD_FAR);
					break;
				default: // If distance item is not received for some reason, fall back to matching with everything we have
					match(mNearList, mNeighbourItems, THRESHOLD_NEAR);
					// Get common followings tweets if we have the target user's screen name
					if(!mTargetUserScreenName.isEmpty())
						getCommonTweets();
					match(mFarList, mNeighbourItems, THRESHOLD_FAR);
					break;
				}
			} else {
				switch(mDistance) {
				case MainActivity.DISTANCE_NEAR:
					match(mNearList, mNeighbourNearList, THRESHOLD_NEAR);
					// Get common followings tweets if we have the target user's screen name
					if(!mTargetUserScreenName.isEmpty())
						getCommonTweets();
					break;
				case MainActivity.DISTANCE_FAR:
					// Match data
					match(mFarList, mNeighbourFarList, THRESHOLD_FAR);
					break;
				default: // If distance item is not received for some reason, fall back to matching with everything we have
					match(mNearList, mNeighbourNearList, THRESHOLD_NEAR);
					// Get common followings tweets if we have the target user's screen name
					if(!mTargetUserScreenName.isEmpty())
						getCommonTweets();
					match(mFarList, mNeighbourFarList, THRESHOLD_FAR);
					break;
				}
			}
		}
		
		/**
		 * Gets tweets of the users' common followings 
		 */
		public void getCommonTweets() {
			Log.d(TAG, "getCommonTweets()");
			
			List<String> commonFollowingsTweets = TwitterUtils.INSTANCE.getCommonFollowingsTweets(mTargetUserScreenName);
			if(commonFollowingsTweets == null)
				return;
			Log.i(TAG, commonFollowingsTweets.size() + " common tweets found " + mRunning);
			
			// We just came back from a potentially long running network operation
			// Check if we're canceled and then proceed
			if(!mRunning) return;
			
			// Add the tweets to the top of matched cues
			for(String tweet : commonFollowingsTweets) {
				mMatchedCuesList.add(0, new CueItem(-1, InfoType.INFO_TWITTER, tweet, true));
			}
		}
		
		/**
		 * Matches items in one list with another adding matched items to mMatchedCuesList
		 * @param myItems
		 * @param theirItems
		 * @param threshold
		 * Algorithm (for 1 string):
		 * 1. Match a string directly. If identical, add to the mMatchedCuesList
		 * 2. If not, compute Levenshtein distance. If it's above the specified threshold
		 *    value, add to the mMatchedCuesList
		 * 3. If not, then ignore the string
		 */
		private void match(List<CueItem> myItems, List<CueItem> theirItems, double threshold) {
			// Create trimmed lower case copies
			List<String> list1 = new ArrayList<String>();
			for(CueItem item : myItems) {
				list1.add(item.data().trim().toLowerCase(Locale.getDefault()));
			}
			List<String> list2 = new ArrayList<String>();
			for(CueItem item : theirItems) {
				list2.add(item.data().trim().toLowerCase(Locale.getDefault()));
			}
			
			// Match every string in one list against the other
			// Note: If there's a match found, the matching string is removed from
			// subsequent comparisons
			for(int i = 0; mRunning && i < list1.size(); ++i) {
				String s1 = list1.get(i);
				for(int j = 0; mRunning && j < list2.size(); ++j) {
					String s2 = list2.get(j);
					if(s2.equals(s1)) {
						Log.i(TAG, "match found: " + myItems.get(i).data());
						mMatchedCuesList.add(myItems.get(i));
						break;
					} else {
						int ld = computeLevenshteinDistance(s1, s2);
						double sim = (1 - (ld / (double)Math.max(s1.length(), s2.length())));
						if(sim >= threshold) {
								Log.i(TAG, "match found: " + myItems.get(i).data());
								mMatchedCuesList.add(myItems.get(i));
								break;
						}
					}
				}
			}
		}
		
		/**
		 * Returns the minimum of 3 numbers
		 * @param a
		 * @param b
		 * @param c
		 * @return
		 */
		private int minimum(int a, int b, int c) {                            
			return Math.min(Math.min(a, b), c);                                      
		}

		/**
		 * Returns Levenshtein edit distance between 2 strings
		 * @param s1
		 * @param s2
		 * @return
		 * Levenshtein distance is defined as the minimum number of edits
		 * (add/delete/change) required to make one string identical to the
		 * other
		 */
		public int computeLevenshteinDistance(String s1, String s2) {
			int len1 = s1.length() + 1;
			int len2 = s2.length() + 1;

			// Array of distances
			int[] cost = new int[len1];
			int[] newcost = new int[len1];

			// Initial cost of skipping prefix in String s1
			for (int i = 0; i < len1; i++)
				cost[i] = i;

			// Compute the array of distances
			// Transformation cost for each letter in s2
			for (int j = 1; j < len2; j++) {
				// Initial cost of skipping prefix in String s2
				newcost[0] = j;

				// Transformation cost for each letter in s1
				for(int i = 1; i < len1; i++) {
					// Matching current letters in both strings
					int match = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;

					// Cost for each type of operation
					int cost_replace = cost[i - 1] + match;
					int cost_insert  = cost[i] + 1;
					int cost_delete  = newcost[i - 1] + 1;

					// Keep minimum cost
					newcost[i] = minimum(cost_insert, cost_delete, cost_replace);
				}
				// Swap cost/newcost arrays
				int[] swap = cost; cost = newcost; newcost = swap;
			}

			// The distance is the cost for transforming all letters in both strings
			return cost[len1 - 1];
		}
		
		/**
		 * Resets the running flag of this thread
		 */
		public void cancel() {
			Log.d(TAG, "cancel()");
			mRunning = false;
		}
	}
}
