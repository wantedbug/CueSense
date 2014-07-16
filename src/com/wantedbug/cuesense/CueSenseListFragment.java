/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
	private String sampleList[];
	
	public class CueSenseListAdapter extends ArrayAdapter<String> {
		private final Context context;
		private final String[] values;
		LayoutInflater inflater;

		public CueSenseListAdapter(Context context, String[] values) {
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
			textView.setText(values[position]);
			return view;
		}
	}
	
	public CueSenseListFragment() {
		// TODO remove dummy data
		sampleList = new String[] {
				"Apolitical",
				"Humanist",
				"Equalist",
				"Do or do not, there is no try",
				"Let's see how long a message can really fit into a simple list item"
		};
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		ListAdapter listAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_multiple_choice, sampleList);
		ListAdapter listAdapter = new CueSenseListAdapter(getActivity(), sampleList);
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
