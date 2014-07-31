/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.wantedbug.cuesense.MainActivity.InfoType;

/**
 * Encapsulates an item in the list views
 * @author vikasprabhu
 */
public class CueItem {
	// Debugging
	private static final String TAG = "CueItem";
	/**
	 * Members
	 */
	private int mId;
	private InfoType mType;
	private String mData;
	private boolean mChecked;
	
	/** c'tors */
	public CueItem() {
		mId = 0;
		mType = InfoType.INFO_SENTINEL;
		mData = "";
		mChecked = true;
	}
	
	public CueItem(int id, InfoType type, String data, boolean checked) {
		mId = id;
		mType = type;
		mData = data;
		mChecked = checked;
	}
	
	/** get/set methods */
	public int id() { return mId; }
	public void setId(int id) { mId = id; }
	public InfoType type() { return mType; }
	public void setType(InfoType type) { mType = type; }
	public String data() { return mData; }
    public void setData(String data) { this.mData = data; }
	public boolean isChecked() { return mChecked; }
	public void setChecked(boolean checked) { mChecked = checked; }
	
	/**
	 * Creates a JSONObject out of this CueItem
	 * @return
	 */
	public JSONObject toJSONObject() {
		JSONObject obj = new JSONObject();
        try {
            obj.put("id", mId);
            obj.put("type", mType.value());
            obj.put("data", mData);
            obj.put("isChecked", mChecked);
        } catch (JSONException e) {
            Log.e(TAG, "CueItem JSON creation error " + e);
            obj = null;
            return null;
        }
        return obj;
	}
}
