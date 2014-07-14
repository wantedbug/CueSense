/**
 * Copyright (C) 2014 Tampere University of Technology
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;

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
	 * Members
	 */
	// Static singleton instance
	public static final InfoPool INSTANCE = new InfoPool();
	
	public class InfoItem {
		private InfoType mType;
		private String mData;
		
		public InfoItem(InfoType type, String data) {
			mType = type;
			mData = data;
		}
		
		public InfoType type() { return mType; }
		public void setType(InfoType type) { mType = type; }
		public String data() { return mData; }
		public void setData(String data) { mData = data; }
	}
	
	// List for the data
	private ArrayList<InfoItem> mList = new ArrayList<InfoItem>();
	
	/**
	 * Private c'tor to defeat instantiation
	 */
	private InfoPool() { }
	
	/**
	 * Adds an InfoItem to the pool
	 * @param item
	 * @return
	 */
	public void add(InfoItem item) {
		Log.d(TAG, "add: " + item.type() + item.data());
		mList.add(item);
	}
}
