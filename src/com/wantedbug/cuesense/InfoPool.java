/**
 * Copyright (C) 2014 Tampere University of Technology
 */

package com.wantedbug.cuesense;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

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
	private Vector<CueItem> mNewCuesList = new Vector<CueItem>();
	
	// List for cues matched with another user - next highest priority
	private Vector<CueItem> mMatchedCuesList = new Vector<CueItem>(INIT_SIZE);
	// Global list counter
	private int mMatchedCounter = 0;
	
	// Global list for everything else - lowest priority
	private Vector<CueItem> mGlobalList = new Vector<CueItem>(INIT_SIZE);
	// Global list counter
	private int mGlobalCounter = 0;
	
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
		if(item.type() == InfoType.INFO_CUESENSE) {
			mNewCuesList.add(0, item);
		} else {
			mGlobalList.add(item);
		}
	}
	
	/**
	 * Clears the InfoPool
	 */
	public void clear() {
		Log.d(TAG, "clear()");
		mGlobalList.clear();
		mNewCuesList.clear();
		mMatchedCuesList.clear();
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
		mGlobalList.addAll(items);
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
		
		int pos = 0;
		for(; pos < mGlobalList.size(); ++pos) {
			if(mGlobalList.elementAt(pos).type().equals(type)) {
				break;
			}
		}
		mGlobalList.addAll(pos, items);
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
			if(item.id() == temp.id()) {
				Log.i(TAG, "removing from new list " + item.type() + "," + item.data());
				it.remove();
				wasInNewList = true;
				break;
			}
		}
		
		// Check in matched list as well
		it = mMatchedCuesList.iterator();
		while(it.hasNext()) {
			CueItem temp = it.next();
			if(item.id() == temp.id()) {
				Log.i(TAG, "removing from matched list " + item.type() + "," + item.data());
				it.remove();
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
			if(item.id() == temp.id()) {
				Log.i(TAG, "removing from global list " + item.type() + "," + item.data());
				it.remove();
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
			if(it.id() == item.id()) {
				it.setType(item.type());
				it.setData(item.data());
				it.setChecked(item.isChecked());
				wasInNewList = true;
				break;
			}
		}

		// TODO What happens if item is in matched list and, due to the update,
		// it doesn't match so well any more?
		// Search in matched list as well
		for(CueItem it : mMatchedCuesList) {
			if(it.id() == item.id()) {
				it.setType(item.type());
				it.setData(item.data());
				it.setChecked(item.isChecked());
				break;
			}
		}
		
		// If not in new list, then search in global list
		if(wasInNewList) {
			return;
		}
		for(CueItem it : mGlobalList) {
			if(it.id() == item.id()) {
				it.setType(item.type());
				it.setData(item.data());
				it.setChecked(item.isChecked());
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
	 * Gets next message from the appropriate list
	 * @return
	 * Priority order for search:
	 * 1. New cues list
	 * 2. Matched list
	 * 3. Global list
	 */
	public synchronized String getNext() {
		Log.d(TAG, "getNext()");
		if(mNewCuesList.isEmpty() && mGlobalList.isEmpty() && mMatchedCuesList.isEmpty()) {
			Log.i(TAG, "getNext() lists empty");
			return "CueSense";
		}
		
		// If there's a new Cue that the user just entered, return that
		// and add the same to the end of the global list
		if(!mNewCuesList.isEmpty()) {
			String ret = mNewCuesList.elementAt(0).data();
			mGlobalList.add(mNewCuesList.elementAt(0));
			mNewCuesList.remove(0);
			Log.i(TAG, "getNext() from new list " + ret);
			return ret;
		}
		
		// If there's a matched Cue, return that
		if(!mMatchedCuesList.isEmpty()) {
			if(mMatchedCounter >= mMatchedCuesList.size()) {
				mMatchedCounter = 0;
			}
			String ret = mMatchedCuesList.elementAt(mMatchedCounter).data();
			++mMatchedCounter;
			Log.i(TAG, "getNext() from matched list " + ret);
			return ret;
		}
		
		// If not, then return one from the global list
		if(mGlobalCounter >= mGlobalList.size()) {
			mGlobalCounter = 0;
		}
		String ret = mGlobalList.elementAt(mGlobalCounter).data();
		++mGlobalCounter;
		Log.i(TAG, "getNext() from global list " + ret);
		return ret;
	}
}
