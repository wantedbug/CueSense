/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.List;

import com.wantedbug.cuesense.MainActivity.InfoType;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * This class is a wrapper to SQLite functionality 
 * @author vikasprabhu
 * It implements CRUD for CueSense items that the user inputs
 */
public class DBHelper extends SQLiteOpenHelper {
	/**
	 * Constants
	 */
	// Database details
	private static final String DB_NAME = "cuesense_db";
    private static final int DB_VERSION = 1;
    // Table name
    public static final String TABLE_CUESENSE = "cuesense_table";
    // Table column names (match CueItem members)
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TYPE = "type";
    public static final String COLUMN_DATA = "data";
    public static final String COLUMN_ISCHECKED = "ischecked";
    // SQL queries
    private static final String CUESENSE_TABLE_CREATE =
    		"CREATE TABLE IF NOT EXISTS " + TABLE_CUESENSE + " (" +
            		COLUMN_ID + " INT, " +
            		COLUMN_TYPE + " INT, " +
            		COLUMN_DATA + " TEXT, " +
            		COLUMN_ISCHECKED + "INT" + 
            		");";
    
    /**
     * Members
     */
    private Context context;
    
    /** c'tor */
    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.context = context;
    }

    /**
     * Creates database and table when DBHelper is instantiated
     */
	@Override
	public void onCreate(SQLiteDatabase db) {
		// Create the database and table
		db.execSQL(CUESENSE_TABLE_CREATE);
	}

	/**
	 * Called when database version is upgraded, but we don't need it in our case
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Do nothing
	}
	
	/**
	 * Adds a fully formed CueItem to the database
	 * @param item
	 */
	public void addCueItem(CueItem item) {
		SQLiteDatabase db = getWritableDatabase();
	}
	
	/**
	 * Updates an existing CueItem in the database
	 * @param item
	 */
	public void updateCueItem(CueItem item) {
		SQLiteDatabase db = getWritableDatabase();
	}
	
	/**
	 * Deletes a CueItem from the database
	 * @param item
	 */
	public void deleteCueItem(CueItem item) {
		SQLiteDatabase db = getWritableDatabase();
	}
	
	/**
	 * Returns a list of CueItems of type InfoType
	 * @param type
	 * @return
	 */
	public List<CueItem> getItems(InfoType type) {
		SQLiteDatabase db = getReadableDatabase();
		
		// Execute query and get a cursor
		
		// Fill the list with data from the cursor
		List<CueItem> result = new ArrayList<CueItem>();
		
		return result;
	}

}
