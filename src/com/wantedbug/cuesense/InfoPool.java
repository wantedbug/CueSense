/**
 * Copyright (C) 2014 Tampere University of Technology
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;

import com.wantedbug.cuesense.MainActivity.InfoType;

import android.util.Log;

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
	
	/**
	 * Members
	 */
	// Static singleton instance
	public static final InfoPool INSTANCE = new InfoPool();
	
	// List for newly added CueSense cues - highest priority
	private ArrayList<CueItem> mNewCuesList = new ArrayList<CueItem>();
	
	// List for cues matched with another user - next highest priority
	private ArrayList<CueItem> mMatchedCuesList = new ArrayList<CueItem>(INIT_SIZE);
	// Global list counter
	private int mMatchedCounter = 0;
	
	// Global list for everything else - lowest priority
	private ArrayList<CueItem> mGlobalList = new ArrayList<CueItem>(INIT_SIZE);
	// Global list counter
	private int mGlobalCounter = 0;
	
	// Lists that contain appropriate CueItems for the respective distance levels
	// See MainActivity.BT_RSSI_NEAR etc.
	// Note 1: These lists are converted to JSONArrays for transmitting.
	// Note 2: Instead of creating lists while transmitting, we instead go for creating
	// them earlier (i.e. as and when Cues are added/deleted/modified) to make the
	// transmission quick.
	// Note 3: The decision about which kind of information goes to which
	// list (i.e. which information is transmitted at which range)
	// has to be made. Currently, this is how it's done:
	// 1. User-added Cues are treated as the most personal to be transmitted at near range
	// 2. Facebook Cues are transmitted at intermediate range
	// 3. Twitter Cues are transmitted at far range
	private ArrayList<CueItem> mNearList = new ArrayList<CueItem>();
	private ArrayList<CueItem> mIntermediateList = new ArrayList<CueItem>();
	private ArrayList<CueItem> mFarList = new ArrayList<CueItem>();
	
	// JSONArrays actually used for transmission based on the above lists
	private JSONArray mNearDataArray;
	private JSONArray mIntermediateDataArray;
	private JSONArray mFarDataArray;
	
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
		// Clear lists
		mGlobalList.clear();
		mNewCuesList.clear();
		mMatchedCuesList.clear();
		// Clear data package
	}
	
	/**
	 * Deletes matched cues
	 */
	public synchronized void clearMatchedCues() {
		Log.d(TAG, "clearMatchedCues()");
		mMatchedCuesList.clear();
		mMatchedCounter = 0;
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
				onCueUpdated(item);
				wasInNewList = true;
				break;
			}
		}

		// TODO What happens if item is in matched list and, due to the update,
		// it doesn't match so well any more?
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
	public synchronized String getNext() {
		Log.d(TAG, "getNext()");
		if(mNewCuesList.isEmpty() && mGlobalList.isEmpty() && mMatchedCuesList.isEmpty()) {
			Log.i(TAG, "getNext() lists empty");
			return "CueSense";
		}
		
		// Return the first encountered checked Cue that the user entered
		// and add the same to the end of the global list
		if(!mNewCuesList.isEmpty()) {
			boolean found = false;
			String ret = "";
			Iterator<CueItem> it = mNewCuesList.iterator();
			while(!found && it.hasNext()) {
				CueItem item = it.next();
				if(item.isChecked()) {
					ret = item.data();
					mGlobalList.add(item);
					it.remove();
					found = true;
				}
			}
			if(found) {
				Log.i(TAG, "getNext() from new list " + ret);
				return ret;
			}
		}
		
		// If there's a matched Cue, return that
		if(!mMatchedCuesList.isEmpty()) {
			if(mMatchedCounter >= mMatchedCuesList.size()) {
				mMatchedCounter = 0;
			}
			String ret = mMatchedCuesList.get(mMatchedCounter).data();
			++mMatchedCounter;
			Log.i(TAG, "getNext() from matched list " + ret);
			return ret;
		}
		
		// If not, then return the first checked item from the global list
		if(mGlobalCounter >= mGlobalList.size()) {
			mGlobalCounter = 0;
		}
		for(int i = 0; i < mGlobalCounter; ++i) {
			CueItem item = mGlobalList.get(i);
			if(item.isChecked()) {
				mGlobalCounter = i;
			}
		}
		String ret = mGlobalList.get(mGlobalCounter).data();
		++mGlobalCounter;
		Log.i(TAG, "getNext() from global list " + ret);
		return ret;
	}
	
	/**
	 * Adds a Cue to the appropriate data package
	 * @param item
	 * The Cue is not added if it's unchecked.
	 */
	private void onCueAdded(CueItem item) {
		Log.d(TAG, "onCueAdded()");
		
		if(!item.isChecked()) return;
		
		switch(item.type()) {
		case INFO_CUESENSE: 
			mNearList.add(item);
			break;
		case INFO_FACEBOOK: 
			mIntermediateList.add(item);
			break;
		case INFO_TWITTER:
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
		Log.d(TAG, "onCuesAdded()");
		for(CueItem item : items) {
			if(item.isChecked()) onCueAdded(item);
		}
	}
	
	/**
	 * Deletes a Cue from the appropriate data package
	 * @param item
	 */
	private void onCueDeleted(CueItem item) {
		Log.d(TAG, "onCueDeleted()");
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
			Iterator<CueItem> it = mIntermediateList.iterator();
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
		Log.d(TAG, "onCueUpdated()");
		if(!item.isChecked()) {
			onCueDeleted(item);
		} else if(item.isChecked()) {
			onCueAdded(item);
		} else {
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
				for(CueItem it : mIntermediateList) {
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
	}
}
