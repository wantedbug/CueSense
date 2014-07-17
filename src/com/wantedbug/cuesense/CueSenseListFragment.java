/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.List;

import com.wantedbug.cuesense.MainActivity.InfoType;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

/**
 * List view of the user's CueSense profile data 
 * @author vikasprabhu
 */
public class CueSenseListFragment extends ListFragment {
	/**
	 * Interface
	 */
	public interface CueSenseListener {
		/** Handle addition of a new cue */
		void onCueAdded(CueItem item);
		
		/** Handle deletion of a cue */
		void onCueDeleted(CueItem item);
		
		/** Handle modification of a cue */
		void onCueChanged(CueItem item);
	}
	
	/**
	 * Members
	 */
	// List of CueItems
	private List<CueItem> mCueSenseList;
	// Listener to handle CueItem addition, deletion and modification
	CueSenseListener mListener;
	
	/**
	 * Custom list adapter for the list view 
	 * @author vikasprabhu
	 */
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
			
			EditText textView = (EditText) view.findViewById(R.id.data);
			textView.setText(values.get(position).data());
			CheckBox checkbox = (CheckBox) view.findViewById(R.id.isChecked);
			checkbox.setChecked(values.get(position).isChecked());
			return view;
		}
	}
	
	public CueSenseListFragment() {
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DBHelper dbHelper = new DBHelper(getActivity());

		// Read items from database when the view is first created
		mCueSenseList = dbHelper.getItems(InfoType.INFO_CUESENSE);
		ListAdapter listAdapter = new CueSenseListAdapter(getActivity(), mCueSenseList);
		setListAdapter(listAdapter);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.tab_cuesense, container, false);
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if(!(activity instanceof CueSenseListener)) {
            throw new RuntimeException("Activity must implement NewTaskDialogListener interface!");
        }
        
        mListener = (CueSenseListener) activity;
    }

	@Override
	public void onListItemClick(ListView list, View v, int position, long id) {
		Toast.makeText(getActivity(), getListView().getItemAtPosition(position).toString(), Toast.LENGTH_LONG).show();
	}
}
