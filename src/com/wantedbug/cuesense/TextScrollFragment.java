/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Fragment that holds the scrolling text simulating a wearable display
 * @author vikasprabhu
 */
public class TextScrollFragment extends DialogFragment {
	// Debugging
	private static final String TAG = "TextScrollFragment";
	
	/**
	 * Members
	 */
	private View mView;
	private TextView mScrollText;
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.d(TAG, "onCreateDialog)()");
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.fragment_textscroll, null);
        builder.setView(dialogView);
        return builder.create();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
        super.onDestroy();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
