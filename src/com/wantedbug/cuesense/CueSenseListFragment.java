/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.List;

import com.wantedbug.cuesense.DeleteCueSenseItemDialog.DeleteCueSenseItemListener;
import com.wantedbug.cuesense.MainActivity.InfoType;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

/**
 * List view of the user's CueSense profile data 
 * @author vikasprabhu
 */
public class CueSenseListFragment extends ListFragment {
	// Debugging
	private static final String TAG = "CueSenseListFragment";
	/**
	 * Interface for new/deleted/changed CueItems in the list view
	 */
	public interface CueSenseListener {
		/** Handle addition of a new cue */
		void onCueSenseCueAdded(CueItem item);
		
		/** Handle deletion of a cue */
		void onCueSenseCueDeleted(CueItem item);
		
		/** Handle modification of a cue */
		void onCueSenseCueChanged(CueItem item);
	}

	/**
	 * Members
	 */
	// Data model for the list view
	private CueSenseListAdapter mCSListAdapter;
	// List view
	private ListView mCSListView;
	// Listener to handle CueItem addition, deletion and modification
	CueSenseListener mListener;
	// Data for the list views
	private List<CueItem> mCSList;
	// Database handle
	private DBHelper mDBHelper;
	
	public CueSenseListFragment() {
		
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.tab_cuesense, container, false);
		mCSListView = (ListView) v.findViewById(android.R.id.list);
		mDBHelper = new DBHelper(getActivity());
		mCSList = mDBHelper.getItems(InfoType.INFO_CUESENSE);
		mCSListAdapter = new CueSenseListAdapter(getActivity(), mCSList, mListener);
		mCSListView.setAdapter(mCSListAdapter);
		
		mCSListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				DialogFragment dialog = new DeleteCueSenseItemDialog(position);
				dialog.show(getActivity().getSupportFragmentManager(), "delete_cuesense_item");
				return true;
			}
		});
		return v;
	}
	
	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        
        if(!(activity instanceof CueSenseListener)) {
            throw new RuntimeException("Activity must implement CueSenseListener interface!");
        }
        
        mListener = (CueSenseListener) activity;
    }
	
	/**
	 * Refreshes the list view
	 */
	public void refreshList() {
		mCSList.clear();
		mCSList.addAll(mDBHelper.getItems(InfoType.INFO_CUESENSE));
		mCSListAdapter.notifyDataSetChanged();
	}
	
	/**
	 * Deletes an item from the list view
	 * @param itemPosition
	 */
	public void onCueDeleted(int itemPosition) {
		CueItem item = mCSList.get(itemPosition);
		mCSList.remove(itemPosition);
		mCSListAdapter.notifyDataSetChanged();
		mListener.onCueSenseCueDeleted(item);
	}
}
