/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.List;

import com.wantedbug.cuesense.CueSenseListFragment.CueSenseListener;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class CueSenseListAdapter extends ArrayAdapter<CueItem> {
	/**
	 * Members
	 */
	private final List<CueItem> mList;
	private final Context mContext;
	private final CueSenseListener mListener;
	private LayoutInflater mInflater;

	public CueSenseListAdapter(Context context, List<CueItem> values, CueSenseListener listener) {
		super(context, R.layout.listitem_tab_cuesense, values);
		this.mContext = context;
		this.mList = values;
		this.mListener = listener;
		this.mInflater = LayoutInflater.from(context);
	}
	
	static class ViewHolder {
	    protected EditText mEditText;
	    protected CheckBox mCheckbox;
	  }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if(convertView == null) {
			view = mInflater.inflate(R.layout.listitem_tab_cuesense, parent, false);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.mEditText = (EditText) view.findViewById(R.id.data);
			viewHolder.mEditText.addTextChangedListener(new TextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					CueItem item = (CueItem) viewHolder.mEditText.getTag();
					if(item.data() != s.toString()) {
						item.setData(s.toString());
						mListener.onCueChanged(item);
					}
				}
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing */}
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) { /* Do nothing */}
			});
			viewHolder.mCheckbox = (CheckBox) view.findViewById(R.id.isChecked);
			viewHolder.mCheckbox
			.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					CueItem item = (CueItem) viewHolder.mCheckbox.getTag();
					item.setChecked(buttonView.isChecked());
					mListener.onCueChanged(item);
				}
			});
			view.setTag(viewHolder);
			viewHolder.mCheckbox.setTag(mList.get(position));
			viewHolder.mEditText.setTag(mList.get(position));
		} else {
			view = convertView;
			((ViewHolder) view.getTag()).mCheckbox.setTag(mList.get(position));
			((ViewHolder) view.getTag()).mEditText.setTag(mList.get(position));
		}		
		ViewHolder holder = (ViewHolder) view.getTag();
	    holder.mEditText.setText(mList.get(position).data());
	    holder.mCheckbox.setChecked(mList.get(position).isChecked());
		return view;
	}
}