/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import com.wantedbug.cuesense.MainActivity.InfoType;

import android.R.color;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
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
	 * Constants
	 */
	// Time interval between successive data push attempts to the wearable device
	private static final int PUSH_INTERVAL_MS = 8000;
	
	/**
	 * Members
	 */
	// UI elements
	private TextView mScrollText;
	
	// A Handler and Runnable to periodically keep pushing messages to the "wearable device"
    Handler mSendCueHandler = new Handler();
    Runnable mSendCueRunnable = new Runnable() {
        @Override
        public void run() {
        	Log.d(TAG, "mSendCueRunnable::run()");
    		if(mScrollText != null) {
    			CueItem item = InfoPool.INSTANCE.getNext();
    			switch(item.type()) {
    			case INFO_CUESENSE:
    				mScrollText.setTextColor(Color.parseColor("#FFA500")); // orange
    				break;
    			case INFO_FACEBOOK:
    				mScrollText.setTextColor(Color.BLUE);
    				break;
    			case INFO_TWITTER:
    				mScrollText.setTextColor(Color.CYAN);
    				break;
    			}
    			mScrollText.setText(item.data());
    		}
        	mSendCueHandler.postDelayed(this, PUSH_INTERVAL_MS);
        }
    };
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
		Log.d(TAG, "onCreateDialog)()");
		// Stop display from timing out while this dialog is still active
		getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		// Create the dialog layout
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.fragment_textscroll, null);
        builder.setView(dialogView);
        
        mScrollText = (TextView) dialogView.findViewById(R.id.scrollText);
        
        return builder.create();
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
        super.onResume();
        
        // Set dialog dimensions
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        getDialog().getWindow().setLayout(width, height);
        
        mSendCueHandler.post(mSendCueRunnable);
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // Stop the send handler runnable
        mSendCueHandler.removeCallbacks(mSendCueRunnable);
        // Allow display to timeout
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}
