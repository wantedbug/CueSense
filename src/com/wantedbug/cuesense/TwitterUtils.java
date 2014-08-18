/**
 * Copyright (C) 2014 Tampere University of Technology
 */

package com.wantedbug.cuesense;

import java.util.ArrayList;
import java.util.List;

import twitter4j.IDs;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Class to hold some Twitter functionality
 * @author vikasprabhu
 * The class is a singleton
 */
public class TwitterUtils {
	// Debugging
	private static final String TAG = "TwitterUtils";
	
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
	static final String PREF_KEY_TWITTER_NAME = "twitter_username";
	static final String PREF_KEY_TWITTER_SCREENNAME = "twitter_screenname";
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
	// Application context to build SharedPreferences
	private Context mContext;
	// Shared Preferences
	private static SharedPreferences mSharedPreferences;
	
	// Twitter handle and details
	private static Twitter mTwitter;
	private static AccessToken mAccessToken;
	private static long mUserId = -1;
	private static String mScreenName = "";
	
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
		if(isTwitterLoggedIn()) {
			getTwitter();
	    	String token = mSharedPreferences.getString(PREF_KEY_OAUTH_TOKEN, "");
	    	String tokenSecret = mSharedPreferences.getString(PREF_KEY_OAUTH_SECRET, "");
	    	if(!token.isEmpty() && !tokenSecret.isEmpty()) {
	    		mAccessToken = new AccessToken(token, tokenSecret);
	    		mTwitter.setOAuthAccessToken(mAccessToken);
	    	} else {
	    		Log.e(TAG, "Twitter token and secret empty");
	    	}
	    	
	    	mUserId = mSharedPreferences.getLong(PREF_KEY_TWITTER_USERID, -1);
	    	mScreenName  = mSharedPreferences.getString(PREF_KEY_TWITTER_SCREENNAME, "");
		}
	}
	
	/**
	 * Returns Twitter login boolean flag from SharedPreferences  
	 */
	private boolean isTwitterLoggedIn() {
		return mSharedPreferences.getBoolean(PREF_KEY_TWITTER_LOGIN, false);
	}
	
	public Twitter getTwitter() {
		if(mTwitter == null) {
			ConfigurationBuilder builder = new ConfigurationBuilder();
			builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
			builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
			Configuration configuration = builder.build();
			TwitterFactory factory = new TwitterFactory(configuration);
			mTwitter = factory.getInstance();
		}
		return mTwitter;
	}
	
	public long getUserID() {
		if(mUserId < 0)
			mUserId = mSharedPreferences.getLong(PREF_KEY_TWITTER_USERID, -1);
		return mUserId;
	}
	
	public String getScreenName() {
		if(mScreenName.isEmpty())
			mScreenName  = mSharedPreferences.getString(PREF_KEY_TWITTER_SCREENNAME, "");
		return mScreenName;
	}
	
	/**
	 * Logs user out of Twitter by clearing out all tokens
	 * from shared preferences
	 */
	public void logoutFromTwitter() {
		// Clear the shared preferences
		Editor e = mSharedPreferences.edit();
		e.remove(TwitterUtils.PREF_KEY_OAUTH_TOKEN);
		e.remove(TwitterUtils.PREF_KEY_OAUTH_SECRET);
		e.remove(TwitterUtils.PREF_KEY_TWITTER_LOGIN);
		e.remove(TwitterUtils.PREF_KEY_TWITTER_USERID);
		e.remove(TwitterUtils.PREF_KEY_TWITTER_NAME);
		e.remove(TwitterUtils.PREF_KEY_TWITTER_SCREENNAME);
		e.commit();
		
		mUserId = -1;
		mScreenName = "";
		mAccessToken = null;
		mTwitter = null;
	}
	
	/**
	 * Returns list of latest tweets of the common followings of the
	 * authenticated user and the specified targetUser
	 * @return
	 * IMPORTANT: MUST BE RUN ON NON-UI THREAD
	 */
	public List<String> getCommonFollowingsTweets(String targetUserScreenName) {
		IDs myFriendsIDs;
		IDs targetUserFriendsIDs;
		List<Status> statuses;
		List<String> commonFollowingsTweets = new ArrayList<String>();
		
		long cursor = -1;
		try {
			// Get authenticated user's friends
			myFriendsIDs = mTwitter.getFriendsIDs(cursor);
			for(long id : myFriendsIDs.getIDs()) {
				Log.i(TAG, "user " + mScreenName + " ~ " + id);
			}

			// Get targetUser's friends
			cursor = -1;
			targetUserFriendsIDs = mTwitter.getFriendsIDs(targetUserScreenName, cursor);
			for(long id : targetUserFriendsIDs.getIDs()) {
				Log.i(TAG, "user " + targetUserScreenName + " ~ " + id);
			}

			// Get common friends
			List<Long> commonIDs = new ArrayList<Long>();
			for(long id1 : myFriendsIDs.getIDs()) {
				for(long id2 : targetUserFriendsIDs.getIDs()) {
					if(id1 == id2) {
						commonIDs.add(Long.valueOf(id1));
					}
				}
			}

			// Get the common friends' recent tweets
			int count = 0;
			for(long id : commonIDs) {
				if(count == 5) break; // max 5 such tweets
				++count;
				statuses = mTwitter.getUserTimeline(id);
				if(!statuses.isEmpty()) {
					Status tweet = statuses.get(0); // latest tweet only
					Log.i(TAG, "@" + tweet.getUser().getName() + ": " + tweet.getText());
					commonFollowingsTweets.add("@" + tweet.getUser().getName() + ": " + tweet.getText());
				}
			}
		} catch(TwitterException e) {
			Log.e(TAG, "friends lookup error " + e);
		}

		return commonFollowingsTweets;
	}
}
