/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import android.content.Context;
import android.util.Log;

/**
 * Helper class to keep database and InfoPool in sync with list views
 * @author vikasprabhu
 */
public class CuesManager {
	// Debugging
	private static final String TAG = "CuesManager";
	/**
	 * Members
	 */
	// Handle to the database
	DBHelper mDBHelper;
	// Handle to the InfoPool instance
	InfoPool pool = InfoPool.INSTANCE;
	
	/**
	 * C'tor
	 * @param context
	 */
	public CuesManager(Context context) {
		mDBHelper = new DBHelper(context);
	}
	
	/**
	 * Adds the CueItem to the database and InfoPool
	 * @param item
	 */
	public void onCueAdded(CueItem item) {
		Log.d(TAG, "onCueAdded()");
		// Push to database
		mDBHelper.addCueItem(item);
		// Push to InfoPool
		pool.addCueItem(item);
	}

	/**
	 * Deletes a CueItem from the database and InfoPool
	 * @param item
	 */
	public void onCueDeleted(CueItem item) {
		Log.d(TAG, "onCueDeleted()");
		// Push to database
		mDBHelper.deleteCueItem(item);
		// Push to InfoPool
		pool.deleteCueItem(item);
	}

	/**
	 * Modifies a CueItem in the database and InfoPool
	 * @param item
	 */
	public void onCueChanged(CueItem item) {
		Log.d(TAG, "onCueChanged()");
		// Push to database
		mDBHelper.updateCueItem(item);
		// Push to InfoPool
	}
}
