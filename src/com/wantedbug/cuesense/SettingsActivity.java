/**
 * Copyright (C) 2014 Tampere University of Technology 
 */

package com.wantedbug.cuesense;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This class is for the Settings view 
 * @author vikasprabhu
 */
public class SettingsActivity extends FragmentActivity {
	// Debugging
	private static final String TAG = "SettingsActivity";

	/**
	 * Members
	 */
	// Fragment that holds Facebook login button
	FBFragment mFBFragment;
	// Twitter API keys
	static String TWITTER_CONSUMER_KEY = "P5HEJoRa6yUyWWatULzmBCpS2";
	static String TWITTER_CONSUMER_SECRET = "JBQo8dilVJLaFx9ReudSiNCntP1wx4DdpfSceJYccRoIAj8Gwx";
	// Preference Constants
	static String PREFERENCE_NAME = "twitter_oauth";
	static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
	static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
	static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";
	static final String PREF_KEY_TWITTER_USERID = "twitter_userid";
	static final String PREF_KEY_TWITTER_USERNAME = "twitter_username";
	static final String TWITTER_CALLBACK_URL = "oauth://cuesense";
	// Twitter oauth urls
	static final String URL_TWITTER_AUTH = "auth_url";
	static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
	static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";
	// Twitter UI elements
	Button mButtonLoginTwitter;
	Button mButtonLogoutTwitter;
	TextView mUserInfoTextView;
	// Handler to change UI elements after async network operations
	private Handler mTwitterUIHandler;
	// Twitter library handles
	private static Twitter mTwitter;
	private static RequestToken mTwitterRequestToken;
	private static AccessToken mAccessToken;
	private static long mUserId;
	private static String mUserName;
	// Shared Preferences
	private static SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(TAG, "onCreate()");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

		/** Facebook UI (handled by FBFragment */
		if (savedInstanceState == null) {
			// Add the fragment on initial activity setup
			mFBFragment = new FBFragment();
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, mFBFragment).commit();
		} else {
			// Or set the fragment from restored state info
			mFBFragment = (FBFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
		}

		/** Twitter UI (handled by SettingsActivity */
		mTwitterUIHandler = new Handler();
		mButtonLoginTwitter = (Button) findViewById(R.id.btnLoginTwitter);
		mButtonLoginTwitter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				loginToTwitter();
			}
		});
		mButtonLogoutTwitter = (Button) findViewById(R.id.btnLogoutTwitter);
		mButtonLogoutTwitter.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				logoutFromTwitter();
			}
		});
		mUserInfoTextView = (TextView) findViewById(R.id.lblUserName);

		// When control returns to SettingsActivity, check if the request token is
		// granted. If yes, store the access token in SharedPreferences and
		// appropriately update UI elements.
		if (!isTwitterLoggedIn()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Uri uri = getIntent().getData();
					if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
						// oAuth verifier
						String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

						try {
							// Get access token
							mAccessToken = mTwitter.getOAuthAccessToken(mTwitterRequestToken, verifier);
							
							// Update UI elements
							mTwitterUIHandler.post(new Runnable() {
								@Override
								public void run() {
									mButtonLoginTwitter.setVisibility(View.GONE);
									mButtonLogoutTwitter.setVisibility(View.VISIBLE);
								}
							});
							mUserId = mAccessToken.getUserId();
							User user = mTwitter.showUser(mUserId);
							mUserName = user.getName();
							mTwitterUIHandler.post(new Runnable() {
								@Override
								public void run() {
									mUserInfoTextView.setText(getResources().getString(R.string.fb_logged_in_as) + " " + mUserName);
								}
							});
							
							// Store some user details to be accessed later
							Editor e = mSharedPreferences.edit();
							e.putString(PREF_KEY_OAUTH_TOKEN, mAccessToken.getToken());
							e.putString(PREF_KEY_OAUTH_SECRET, mAccessToken.getTokenSecret());
							e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
							e.putLong(PREF_KEY_TWITTER_USERID, mUserId);
							e.putString(PREF_KEY_TWITTER_USERNAME, mUserName);
							e.commit();
						} catch (Exception e) {
							Log.e(TAG, "Twitter login error " + e);
						}
					}
				}
			}).start();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(isTwitterLoggedIn()) {
			mButtonLoginTwitter.setVisibility(View.GONE);
			mButtonLogoutTwitter.setVisibility(View.VISIBLE);
			mUserInfoTextView.setText(getResources().getString(R.string.fb_logged_in_as) + " " + mUserName);
		}
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause()");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy()");
		super.onDestroy();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
	}

	/**
	 * Logs the user in to Twitter
	 * Requests a RequestToken with which to obtain 
	 */
	private void loginToTwitter() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!isTwitterLoggedIn()) {
					ConfigurationBuilder builder = new ConfigurationBuilder();
					builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
					builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
					Configuration configuration = builder.build();
					TwitterFactory factory = new TwitterFactory(configuration);
					mTwitter = factory.getInstance();
					try {
						mTwitterRequestToken = mTwitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
						mTwitterUIHandler.post(new Runnable() {
							@Override
							public void run() {
								SettingsActivity.this.startActivityForResult(
										new Intent(Intent.ACTION_VIEW, Uri.parse(mTwitterRequestToken.getAuthenticationURL())), 101);
//											.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY),
//										101);
							}
						});
					} catch (TwitterException e) {
						e.printStackTrace();
					}
				} else {
					mTwitterUIHandler.post(new Runnable() {
						@Override
						public void run() {
							Toast.makeText(getApplicationContext(), "Already Logged into twitter", Toast.LENGTH_LONG).show();
						}
					});
				}
			}
		}).start();
	}

	/**
	 * Logs the user out from Twitter
	 * Clears SharedPreferences of all tokens 
	 */
	private void logoutFromTwitter() {
		// Clear the shared preferences
		Editor e = mSharedPreferences.edit();
		e.remove(PREF_KEY_OAUTH_TOKEN);
		e.remove(PREF_KEY_OAUTH_SECRET);
		e.remove(PREF_KEY_TWITTER_LOGIN);
		e.remove(PREF_KEY_TWITTER_USERID);
		e.remove(PREF_KEY_TWITTER_USERNAME);
		e.commit();
		// Update UI elements
		mButtonLogoutTwitter.setVisibility(View.GONE);
		mUserInfoTextView.setText(R.string.tw_login_explanation);
		mButtonLoginTwitter.setVisibility(View.VISIBLE);
	}

	/**
	 * Returns Twitter login boolean flag from SharedPreferences  
	 */
	private boolean isTwitterLoggedIn() {
		return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}
}
