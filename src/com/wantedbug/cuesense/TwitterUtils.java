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

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * Class to hold some Twitter functionality
 * @author vikasprabhu
 * The class is a singleton
 */
public class TwitterUtils {
	// Debugging
	private static final String TAG = "InfoPool";
	
	/**
	 * Constants
	 */
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
	
	/**
	 * Members
	 */
	// Static singleton instance
	public static final TwitterUtils INSTANCE = new TwitterUtils();
	//
	private Context mContext;
	// Shared Preferences
	private static SharedPreferences mSharedPreferences;
	
	// Twitter handle and details
	private static Twitter mTwitter;
	private static RequestToken mTwitterRequestToken;
	private static AccessToken mAccessToken;
	private static User mUser;
	private static long mUserId;
	private static String mUserName;
	
	/**
	 * Private c'tor to defeat instantiation
	 */
	private TwitterUtils() { }
	
	/**
	 * Initialize the singleton instance
	 * @param context
	 * MUST be called by the MainActivity
	 */
	public void init(Context context) {
		mContext = context;
		mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}
	
	/**
	 * Logs the user out from Twitter
	 * Clears SharedPreferences of all tokens 
	 */
	private void logoutFromTwitter() {
		Editor e = mSharedPreferences.edit();
		e.remove(PREF_KEY_OAUTH_TOKEN);
		e.remove(PREF_KEY_OAUTH_SECRET);
		e.remove(PREF_KEY_TWITTER_LOGIN);
		e.remove(PREF_KEY_TWITTER_USERID);
		e.remove(PREF_KEY_TWITTER_USERNAME);
		e.commit();
	}

	/**
	 * Returns Twitter login boolean flag from SharedPreferences  
	 */
	private boolean isTwitterLoggedIn() {
		return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}
	
	public Twitter getTwitter() {
		ConfigurationBuilder builder = new ConfigurationBuilder();
		builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
		builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
		Configuration configuration = builder.build();
		TwitterFactory factory = new TwitterFactory(configuration);
		mTwitter = factory.getInstance();
		return mTwitter;
	}
	
	public long getUserID() {
		return mUserId;
	}
	
	public String getUsername() {
		return mUserName;
	}
	
	public RequestToken getRequestToken() throws TwitterException {
		mTwitterRequestToken = mTwitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
		return mTwitterRequestToken;
	}
	
	public AccessToken getAccessToken(String verifier) throws TwitterException {
		mAccessToken = mTwitter.getOAuthAccessToken(mTwitterRequestToken, verifier);
		
		// Store some user details to be accessed later
		mUserId = mAccessToken.getUserId();
		mUser = mTwitter.showUser(mUserId);
		mUserName = mUser.getName();
		Editor e = mSharedPreferences.edit();
		e.putString(PREF_KEY_OAUTH_TOKEN, mAccessToken.getToken());
		e.putString(PREF_KEY_OAUTH_SECRET, mAccessToken.getTokenSecret());
		e.putBoolean(PREF_KEY_TWITTER_LOGIN, true);
		e.putLong(PREF_KEY_TWITTER_USERID, mUserId);
		e.putString(PREF_KEY_TWITTER_USERNAME, mUserName);
		e.commit();
		
		return mAccessToken;
	}
}
