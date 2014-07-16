/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import com.wantedbug.cuesense.MainActivity.InfoType;

/**
 * Encapsulates an item in the list views
 * @author vikasprabhu
 *
 */
public class CueItem {
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
	public int getId() { return mId; }
	public void setId(int id) { mId = id; }
	public InfoType getType() { return mType; }
	public void setType(InfoType type) { mType = type; }
	public String getData() { return mData; }
    public void setData(String data) { this.mData = data; }
	public boolean isChecked() { return mChecked; }
	public void setChecked(boolean checked) { mChecked = checked; }
}
