/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.List;

import com.wantedbug.cuesense.MainActivity.InfoType;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * List view of the user's CueSense profile data 
 * @author vikasprabhu
 */
public class CueSenseListFragment extends ListFragment {
	/**
	 * Members
	 */
	// List of CueItems
	private List<CueItem> mCueSenseList;
	// DB helper
	private DBHelper mDBHelper;
	
	public class CueSenseListAdapter extends ArrayAdapter<CueItem> {
		private final Context context;
		private final List<CueItem> values;
		LayoutInflater inflater;

		public CueSenseListAdapter(Context context, List<CueItem> values) {
			super(context, R.layout.listitem_tab_cuesense, values);
			this.context = context;
			this.values = values;
			inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			
			if(convertView == null) {
				view = inflater.inflate(R.layout.listitem_tab_cuesense, parent, false);
			}
			
			TextView textView = (TextView) view.findViewById(R.id.data);
			textView.setText(values.get(position).getData());
			CheckBox checkbox = (CheckBox) view.findViewById(R.id.isChecked);
			checkbox.setChecked(values.get(position).isChecked());
			return view;
		}
	}
	
	public CueSenseListFragment() {
		mDBHelper = new DBHelper(getActivity());
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Read items from database when the view is first created
		mCueSenseList = mDBHelper.getItems(InfoType.INFO_CUESENSE);
		ListAdapter listAdapter = new CueSenseListAdapter(getActivity(), mCueSenseList);
		setListAdapter(listAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_cuesense, container, false);
	}

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		Toast.makeText(getActivity(), getListView().getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
	}
}
