/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

public class FBListFragment extends ListFragment {
	// Debugging
	private static final String TAG = "FBListFragment";
	
	/**
	 * Constants
	 */
	private static final String NAME = "NAME";
	private static final String IS_EVEN = "IS_EVEN";

	/**
	 * Members
	 */
	// Expandable list view
	private ExpandableListAdapter mAdapter;
	
	// View for the fragment
	View v;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		v = inflater.inflate(R.layout.tab_facebook, container, false);
		return v;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		// TODO remove dummy data
		List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
		List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
		for (int i = 0; i < 10; i++) {
			Map<String, String> curGroupMap = new HashMap<String, String>();
			groupData.add(curGroupMap);
			curGroupMap.put(NAME, "Group " + i);
			curGroupMap.put(IS_EVEN, (i % 2 == 0) ? "This group is even" : "This group is odd");

			List<Map<String, String>> children = new ArrayList<Map<String, String>>();
			for (int j = 0; j < 2; j++) {
				Map<String, String> curChildMap = new HashMap<String, String>();
				children.add(curChildMap);
				curChildMap.put(NAME, "Child " + j);
				curChildMap.put(IS_EVEN, (j % 2 == 0) ? "This child is even" : "This child is odd");
			}
			childData.add(children);
		}
		ExpandableListView lv = (ExpandableListView) v.findViewById(android.R.id.list);
		// Set up our adapter
		mAdapter = new SimpleExpandableListAdapter(
				getActivity(),
				groupData,
				android.R.layout.simple_expandable_list_item_1,
				new String[] { NAME, IS_EVEN },
				new int[] { android.R.id.text1, android.R.id.text2 },
				childData,
				android.R.layout.simple_expandable_list_item_2,
				new String[] { NAME, IS_EVEN },
				new int[] { android.R.id.text1, android.R.id.text2 }
				);
		lv.setAdapter(mAdapter);
	}
	
}
