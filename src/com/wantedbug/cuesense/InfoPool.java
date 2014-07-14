/**
 * Copyright (C) 2014 Tampere University of Technology
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.Iterator;
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
	
	/**
	 * This class represents an item in the InfoPool
	 * @author vikasprabhu
	 */
	public class InfoItem {
		/**
		 * Members
		 */
		// The type of information
		private InfoType mType;
		// The information
		private String mData;
		
		public InfoItem(InfoType type, String data) {
			mType = type;
			mData = data;
		}
		
		/**
		 * Get/set methods 
		 */
		public InfoType type() { return mType; }
		public void setType(InfoType type) { mType = type; }
		public String data() { return mData; }
		public void setData(String data) { mData = data; }
	}
	
	// List for the data
	// Note: using Vector for its inbuilt thread safety
	private Vector<InfoItem> mList = new Vector<InfoItem>(INIT_SIZE);
	
	/**
	 * Private c'tor to defeat instantiation
	 */
	private InfoPool() { }
	
	/**
	 * Adds an InfoItem to the pool
	 * @param item
	 * @return
	 */
	public synchronized void add(InfoItem item) {
		Log.d(TAG, "add: " + item.type() + item.data());
		mList.add(item);
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
	public synchronized void addItems(ArrayList<InfoItem> items) {
		Log.d(TAG, "addItems(): " + items.size());
		mList.addAll(items);
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
			Iterator<InfoItem> it = mList.iterator();
			while(it.hasNext()) {
				InfoItem item = it.next();
				if(item.type().equals(type)) {
					Log.i(TAG, "removing " + item.type() + item.data());
					it.remove();
				}
			}
		}
	}
}
