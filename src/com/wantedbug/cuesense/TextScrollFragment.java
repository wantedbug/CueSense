/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;


import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
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
	private ImageView mImage;
	private TextView mContextHeader;
	private TextView mScrollText;
	// Animations for mScrollText
	Animation mSlideInLeftAnim;
	Animation mSlideOutRightAnim;
	Animation mBlinkingAnim;
	// Animation listeners to control mScrollText's animation and to get
	// new item at the end of the animation
	AnimationListener mSlideInLeftAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
			// Do nothing
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mScrollText.startAnimation(mSlideOutRightAnim);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// Do nothing
		}
	};
	
	AnimationListener mSlideOutRightAnimListener = new AnimationListener() {
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
			getNextText();
		}
	};
	
	AnimationListener mBlinkingAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
			// Do nothing
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			getNextText();
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// Do nothing
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
        
        // UI elements
        mImage = (ImageView) dialogView.findViewById(R.id.contextImage1);
        mContextHeader = (TextView) dialogView.findViewById(R.id.contextHeader1);
        mScrollText = (TextView) dialogView.findViewById(R.id.scrollText1);
        
        /** Set up animations */
        // Slide in from left
        mSlideInLeftAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
        mSlideInLeftAnim.setDuration(PUSH_INTERVAL_MS / 2);
        mSlideInLeftAnim.setAnimationListener(mSlideInLeftAnimListener);
        // Slide out to right
        mSlideOutRightAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);
        mSlideOutRightAnim.setDuration(PUSH_INTERVAL_MS / 2);
        mSlideOutRightAnim.setAnimationListener(mSlideOutRightAnimListener);
        // Blink 4 times
        mBlinkingAnim = new AlphaAnimation(0, 1);
        mBlinkingAnim.setDuration(PUSH_INTERVAL_MS / 8);
        mBlinkingAnim.setRepeatMode(Animation.REVERSE);
        mBlinkingAnim.setRepeatCount(4);
        mBlinkingAnim.setAnimationListener(mBlinkingAnimListener);
        
        return builder.create();
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
        super.onResume();
        
        // New item
        getNextText();
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
	private void getNextText() {
		final String spacer = "      ";
		// Get the next item
		CueItem item = InfoPool.INSTANCE.getNext();
		
		// Set text size
		if(item.data().length() <= 25) {
			Log.d(TAG, "font size large");
			mScrollText.setTextSize(getResources().getDimension(R.dimen.scroll_text_size_large));
		} else {
			Log.d(TAG, "font size medium");
			mScrollText.setTextSize(getResources().getDimension(R.dimen.scroll_text_size_medium));
		}
		
		// Set text properties and animation
		switch(item.type()) {
		case INFO_CUESENSE:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			mImage.setBackgroundColor(0x00000000);
			mContextHeader.setText(spacer + "What I did last summer..");
			mScrollText.setTextColor(Color.parseColor("#FFA500")); // CueSense orange
			mScrollText.setText(item.data());
			mScrollText.startAnimation(mBlinkingAnim);
			break;
		case INFO_FACEBOOK:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.com_facebook_inverse_icon));
			mImage.setBackgroundColor(getResources().getColor(R.color.com_facebook_blue));
			mContextHeader.setText(spacer + "I like..");
			mScrollText.setTextColor(Color.parseColor("#627AAD")); // Facebook blue
			mScrollText.setText(item.data());
			mScrollText.startAnimation(mSlideInLeftAnim);
			break;
		case INFO_TWITTER:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.twitter_logo_blue));
			mImage.setBackgroundColor(0x00000000);
			mContextHeader.setText(spacer + "On Twitter..");
			mScrollText.setTextColor(Color.parseColor("#1dcaff")); // Twitter cyan
			mScrollText.setText(item.data());
			mScrollText.startAnimation(mSlideInLeftAnim);
			break;
		default:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			mImage.setBackgroundColor(0x00000000);
			mContextHeader.setText(spacer + "CueSense");
			mScrollText.setTextColor(Color.parseColor("#FFA500")); // CueSense orange
			mScrollText.setText("Hello!");
			mScrollText.startAnimation(mBlinkingAnim);
			break;
		}
	}
}
