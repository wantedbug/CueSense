/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.List;

import com.wantedbug.cuesense.MainActivity.InfoType;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * This class is a wrapper to SQLite functionality 
 * @author vikasprabhu
 * It implements CRUD for CueSense items that the user inputs
 */
public class DBHelper extends SQLiteOpenHelper {
	// Debugging
	private static final String TAG = "DBHelper";
	
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
    // All columns
    public static final String[] ALL_COLUMNS = {
        COLUMN_ID,
        COLUMN_TYPE,
        COLUMN_DATA,
        COLUMN_ISCHECKED
    };
    /** SQL queries */
    private static final String QUERY_TABLE_CREATE =
    		"CREATE TABLE IF NOT EXISTS " + TABLE_CUESENSE + " (" +
            		COLUMN_ID + " INT PRIMARY KEY, " +
            		COLUMN_TYPE + " INT, " +
            		COLUMN_DATA + " TEXT, " +
            		COLUMN_ISCHECKED + " BOOLEAN" + 
            		");";
    private static final String QUERY_NEXT_ID = 
    		"SELECT MAX(" + COLUMN_ID + ") FROM " + TABLE_CUESENSE + ";";
    
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
		Log.d(TAG, "onCreate()");
		// Create the database and table
		db.execSQL(QUERY_TABLE_CREATE);
	}

	/**
	 * Called when database version is upgraded, but we don't need it in our case
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "onUpgrade()");
		// Do nothing
	}
	
	/**
	 * Adds a CueItem to the database
	 * @param item
	 */
	public void addCueItem(CueItem item) {
		Log.d(TAG, "addCueItem()");
		// Create WHERE clause
		ContentValues values = new ContentValues();
		values.put(COLUMN_ID, getNextId());
		values.put(COLUMN_TYPE, item.getType().value());
		values.put(COLUMN_DATA, item.getData());
		values.put(COLUMN_ISCHECKED, true); // new item is checked by default
		
		// Write to the database
		SQLiteDatabase db = getWritableDatabase();
		long ret = db.insert(TABLE_CUESENSE, null, values);
		if(ret == -1) {
			Log.e(TAG, "addCueItem() failed");
		}
	}
	
	/**
	 * Returns the next valid id from the database
	 * @return
	 */
	private int getNextId() {
		Log.d(TAG, "getNextId()");
		SQLiteDatabase db = getReadableDatabase();
		Cursor c = db.rawQuery(QUERY_NEXT_ID, null);
		int id = 0;
		if(c.moveToFirst()) {
			id = c.getInt(0);
		}
		Log.d(TAG, "getNextId() " + id);
		return id + 1;
	}
	
	/**
	 * Updates an existing CueItem in the database
	 * @param item
	 */
	public void updateCueItem(CueItem item) {
		Log.d(TAG, "updateCueItem()");
		SQLiteDatabase db = getWritableDatabase();
	}
	
	/**
	 * Deletes a CueItem from the database
	 * @param item
	 */
	public void deleteCueItem(CueItem item) {
		Log.d(TAG, "deleteCueItem()");
		SQLiteDatabase db = getWritableDatabase();
	}
	
	/**
	 * Deletes all CueItems from the database
	 */
	public void deleteAll() {
		Log.d(TAG, "deleteAll()");
		SQLiteDatabase db = getWritableDatabase();
		db.delete(TABLE_CUESENSE, null, null);
	}
	
	/**
	 * Returns a list of CueItems of type InfoType
	 * @param type
	 * @return
	 */
	public List<CueItem> getItems(InfoType type) {
		Log.d(TAG, "getItems() " + type);
		SQLiteDatabase db = getReadableDatabase();
		
		// Execute query and get a cursor
		Cursor cursor = db.query(TABLE_CUESENSE,
                ALL_COLUMNS,
                "type = " + type.value(),
                null,
                null,
                null,
                COLUMN_ID);
		
		// Fill a list with data from the cursor
		final int idIdx = cursor.getColumnIndex(COLUMN_ID);
		final int typeIdx = cursor.getColumnIndex(COLUMN_TYPE);
        final int dataIdx = cursor.getColumnIndex(COLUMN_DATA);
        final int isCheckedIdx = cursor.getColumnIndex(COLUMN_ISCHECKED);
        
        List<CueItem> result = new ArrayList<CueItem>();
        while(cursor.moveToNext()) {
            CueItem item = new CueItem();
            item.setId(cursor.getInt(idIdx));
            item.setType(InfoType.toInfoType(cursor.getInt(typeIdx)));
            item.setData(cursor.getString(dataIdx));
            item.setChecked((cursor.getInt(isCheckedIdx) == 0 ? Boolean.FALSE : Boolean.TRUE));
            result.add(item);
        }
		Log.d(TAG, "getItems() " + result.size());
		return result;
	}
}
