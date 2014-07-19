package com.wantedbug.cuesense;

import java.util.List;

import com.wantedbug.cuesense.CueSenseListFragment.CueSenseListener;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

public class CueSenseListAdapter extends ArrayAdapter<CueItem> {
	// Debugging
	protected static final String TAG = "CueSenseListAdapter";
	/**
	 * Members
	 */
	private final List<CueItem> list;
	private final Context context;
	private final CueSenseListener mListener;
	LayoutInflater inflater;

	public CueSenseListAdapter(Context context, List<CueItem> values, CueSenseListener listener) {
		super(context, R.layout.listitem_tab_cuesense, values);
		this.context = context;
		this.list = values;
		this.mListener = listener;
		this.inflater = LayoutInflater.from(context);
	}
	
	static class ViewHolder {
	    protected EditText text;
	    protected CheckBox checkbox;
	  }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		
		if(convertView == null) {
			view = inflater.inflate(R.layout.listitem_tab_cuesense, parent, false);
			final ViewHolder viewHolder = new ViewHolder();
			viewHolder.text = (EditText) view.findViewById(R.id.data);
			viewHolder.text.addTextChangedListener(new TextWatcher() {
				@Override
				public void afterTextChanged(Editable s) {
					CueItem item = (CueItem) viewHolder.text.getTag();
					item.setData(s.toString());
					mListener.onCueChanged(item);
				}
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) { /* Do nothing */}
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) { /* Do nothing */}
			});
			viewHolder.checkbox = (CheckBox) view.findViewById(R.id.isChecked);
			viewHolder.checkbox
			.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView,
						boolean isChecked) {
					CueItem item = (CueItem) viewHolder.checkbox.getTag();
					item.setChecked(buttonView.isChecked());
					mListener.onCueChanged(item);
				}
			});
			view.setTag(viewHolder);
			viewHolder.checkbox.setTag(list.get(position));
			viewHolder.text.setTag(list.get(position));
		} else {
			view = convertView;
			((ViewHolder) view.getTag()).checkbox.setTag(list.get(position));
			((ViewHolder) view.getTag()).text.setTag(list.get(position));
		}		
		ViewHolder holder = (ViewHolder) view.getTag();
	    holder.text.setText(list.get(position).data());
	    holder.checkbox.setChecked(list.get(position).isChecked());
		return view;
	}
}