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
//	private static final int PUSH_INTERVAL_MS = 10000;
	private static final int DURATION_SHORT = 750;
	private static final int DURATION_MEDIUM = 1000;
	private static final int DURATION_LONG = 5000;
	
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
	Animation mSlideInTopAnim;
	Animation mSlideOutBottomAnim;
	Animation mSlideInRightAnim;
	Animation mSlideOutLeftAnim;
	Animation mFadeOutAnim;
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
	
	AnimationListener mSlideInTopAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
			// Do nothing
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mScrollText.startAnimation(mSlideOutBottomAnim);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// Do nothing
		}
	};
	
	AnimationListener mSlideOutBottomAnimListener = new AnimationListener() {
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
	
	AnimationListener mSlideInRightAnimListener = new AnimationListener() {
		@Override
		public void onAnimationStart(Animation animation) {
			// Do nothing
		}

		@Override
		public void onAnimationEnd(Animation animation) {
			mScrollText.startAnimation(mFadeOutAnim);
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
			// Do nothing
		}
	};
	
	AnimationListener mSlideOutLeftAnimListener = new AnimationListener() {
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
	
	AnimationListener mFadeOutAnimListener = new AnimationListener() {
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
        
        /** Animations */
        // Slide in from left
        mSlideInLeftAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_in_left);
        mSlideInLeftAnim.setDuration(DURATION_MEDIUM);
        mSlideInLeftAnim.setAnimationListener(mSlideInLeftAnimListener);
        // Slide out to right
        mSlideOutRightAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.slide_out_right);
        mSlideOutRightAnim.setDuration(DURATION_LONG);
        mSlideOutRightAnim.setAnimationListener(mSlideOutRightAnimListener);
        // Blink multiple times
        mBlinkingAnim = new AlphaAnimation(1, 0);
        mBlinkingAnim.setDuration(DURATION_SHORT);
        mBlinkingAnim.setRepeatCount(7);
        mBlinkingAnim.setAnimationListener(mBlinkingAnimListener);
        // Slide in from top
        mSlideInTopAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_top);
        mSlideInTopAnim.setDuration(DURATION_MEDIUM);
        mSlideInTopAnim.setAnimationListener(mSlideInTopAnimListener);
        // Slide out to bottom
        mSlideOutBottomAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_bottom);
        mSlideOutBottomAnim.setDuration(DURATION_LONG);
        mSlideOutBottomAnim.setAnimationListener(mSlideOutBottomAnimListener);
        // Slide in from right
        mSlideInRightAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_in_right);
        mSlideInRightAnim.setDuration(DURATION_MEDIUM);
        mSlideInRightAnim.setAnimationListener(mSlideInRightAnimListener);
        // Slide out to left
        mSlideOutLeftAnim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_out_left);
        mSlideOutLeftAnim.setDuration(DURATION_LONG);
        mSlideOutLeftAnim.setAnimationListener(mSlideOutLeftAnimListener);
        // Fade out
        mFadeOutAnim = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
        mFadeOutAnim.setDuration(DURATION_LONG);
        mFadeOutAnim.setAnimationListener(mFadeOutAnimListener);
        
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
		final String spacer = "       ";
		// Get the next item
		CueItem item = InfoPool.INSTANCE.getNext();
		
		final int TEXT_LEN_FONT_LARGE = 23;
		final int TEXT_LEN_FONT_MEDIUM = 30;
		
		// Set text size
		int strlen = item.data().length(); 
		if(strlen <= TEXT_LEN_FONT_LARGE) {
			Log.d(TAG, "font size large");
			mScrollText.setTextSize(getResources().getDimension(R.dimen.scroll_text_size_large));
		} else if(strlen > TEXT_LEN_FONT_LARGE && strlen <= TEXT_LEN_FONT_MEDIUM) {
			Log.d(TAG, "font size medium");
			mScrollText.setTextSize(getResources().getDimension(R.dimen.scroll_text_size_medium));
		} else {
			Log.d(TAG, "font size small");
			mScrollText.setTextSize(getResources().getDimension(R.dimen.scroll_text_size_small));
		}
		
		// Set text properties and animation
		switch(item.type()) {
		case INFO_CUESENSE:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			mImage.setBackgroundColor(0x00000000);
			mContextHeader.setText(spacer + "About me..");
			mScrollText.setTextColor(Color.parseColor("#FFA500")); // CueSense orange
			mScrollText.setText(item.data());
			mScrollText.startAnimation(mBlinkingAnim);
			break;
		case INFO_FACEBOOK:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.com_facebook_inverse_icon));
			mImage.setBackgroundColor(getResources().getColor(R.color.com_facebook_blue));
			mContextHeader.setText(spacer + "On my Facebook..");
			mScrollText.setTextColor(Color.parseColor("#627AAD")); // Facebook blue
			mScrollText.setText(item.data());
			mScrollText.startAnimation(mSlideInTopAnim);
			break;
		case INFO_TWITTER:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.twitter_logo_blue));
			mImage.setBackgroundColor(0x00000000);
			mContextHeader.setText(spacer + "On my Twitter..");
			mScrollText.setTextColor(Color.parseColor("#1dcaff")); // Twitter cyan
			mScrollText.setText(item.data());
			mScrollText.startAnimation(mSlideInRightAnim);
			break;
		default:
			mImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_launcher));
			mImage.setBackgroundColor(0x00000000);
			mContextHeader.setText(spacer + "CueSense");
			mScrollText.setTextColor(Color.parseColor("#FFA500")); // CueSense orange
			mScrollText.setText("Hello!");
			mScrollText.startAnimation(mSlideInTopAnim);
			break;
		}
	}
}
