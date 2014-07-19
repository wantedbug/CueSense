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
	
	// TODO test stuff to send messages
	private int counter = 0;
	
	// List for the data
	// Note: using Vector for its inbuilt thread safety
	private Vector<CueItem> mList = new Vector<CueItem>(INIT_SIZE);
	
	/**
	 * Private c'tor to defeat instantiation
	 */
	private InfoPool() { }
	
	/**
	 * Adds an InfoItem to the pool
	 * @param item
	 * @return
	 */
	public synchronized void addCueItem(CueItem item) {
		Log.d(TAG, "add: " + item.type() + item.data());
		mList.add(item);
		Log.i(TAG, "adding " + item.type() + item.data());
	}
	
	/**
	 * Clears the InfoPool
	 */
	public void clear() {
		Log.d(TAG, "clear(): " + mList.size());
		mList.clear();
	}
	
	/**
	 * Adds a list of InfoItems to the InfoPool
	 * @param items
	 */
	public synchronized void addCueItems(List<CueItem> items) {
		Log.d(TAG, "addCueItems(): " + items.size());
		mList.addAll(items);
	}
	
	/**
	 * Deletes a CueItem from the list
	 * @param item
	 * The CueItems are matched according to their id
	 */
	public synchronized void deleteCueItem(CueItem item) {
		Log.d(TAG, "deleteCueItem(): " + item.id());
		Iterator<CueItem> it = mList.iterator();
		while(it.hasNext()) {
			CueItem temp = it.next();
			if(item.id() == temp.id()) {
				Log.i(TAG, "removing " + item.type() + item.data());
				it.remove();
				break;
			}
		}
	}
	
	/**
	 * Updates a CueItem in the list
	 * @param item
	 * The CueItems are matched according to their id
	 */
	public synchronized void updateCueItem(CueItem item) {
		Log.d(TAG, "updateCueItem(): " + item.id());
		for(CueItem it : mList) {
			if(it.id() == item.id()) {
				Log.i(TAG, "updating " + item.type() + item.data());
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
		
		// Nothing to do with an empty list
		if(mList.isEmpty()) {
			Log.i(TAG, "deleteType() - list empty");
			return;
		}
		// Iterate through the list weeding out items with type
		else {
			Iterator<CueItem> it = mList.iterator();
			while(it.hasNext()) {
				CueItem item = it.next();
				if(item.type().equals(type)) {
					Log.i(TAG, "removing " + item.type() + item.data());
					it.remove();
				}
			}
		}
	}
	
	/**
	 * Gets next message from the list
	 * @return
	 */
	public synchronized String getNext() {
		if(mList.size() == 0) {
			return "CueSense";
		}
		
		if(counter >= mList.size()) {
			counter = 0;
		}
		String temp = mList.elementAt(counter).data();
		++counter;
		return temp;
	}
}
