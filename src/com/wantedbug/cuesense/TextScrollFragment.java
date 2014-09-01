/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;


import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
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
	// Animations for mScrollText
	Animation mIntroAnim;
	Animation mOutroAnim;
	// Animation listeners to control mScrollText's animation and to get
	// new item at the end of the animation
	AnimationListener mIntroAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
			// Do nothing
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mScrollText.startAnimation(mOutroAnim);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// Do nothing
		}
	};
	
	AnimationListener mOutroAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
			// Do nothing
		}
		
		@Override
		public void onAnimationRepeat(Animation animation) {
			// Do nothing
		}
		
		@Override
		public void onAnimationEnd(Animation animation) {
			setText();
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
        
        // Rotate text to make it look like the dialog is in landscape mode
        mScrollText.setRotation(90);
        
        // Set mScrollText's intro and outro animations
        mIntroAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
        mIntroAnim.setDuration(PUSH_INTERVAL_MS / 2);
        mIntroAnim.setAnimationListener(mIntroAnimListener);
        mOutroAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);
        mOutroAnim.setDuration(PUSH_INTERVAL_MS / 2);
        mOutroAnim.setAnimationListener(mOutroAnimListener);
        
        return builder.create();
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
        super.onResume();
        
        // Set dialog dimensions explicitly
        Point outSize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(outSize);
        int width = outSize.x;
        int height = outSize.y - 50;
        getDialog().getWindow().setLayout(width, height);
        
        // New item
        setText();
	}
	
	@Override
	public void onDestroy() {
		Log.d(TAG, "onDestroy()");
        super.onDestroy();
        mScrollText.clearAnimation();
        // Allow display to timeout
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
	
	/**
	 * Sets mScrollText by getting the next CueItem from the InfoPool
	 * and starting its intro animation
	 */
	private void setText() {
		// New item
		CueItem item = InfoPool.INSTANCE.getNext();
		if(mScrollText != null) {
			switch(item.type()) {
			case INFO_CUESENSE:
				mScrollText.setTextColor(Color.parseColor("#FFA500")); // CueSense orange
				break;
			case INFO_FACEBOOK:
				mScrollText.setTextColor(Color.parseColor("#627AAD")); // Facebook blue
				break;
			case INFO_TWITTER:
				mScrollText.setTextColor(Color.parseColor("#1dcaff")); // Twitter cyan
				break;
			}
		}
		mScrollText.setText(item.data());
		mScrollText.startAnimation(mIntroAnim);
	}
}
