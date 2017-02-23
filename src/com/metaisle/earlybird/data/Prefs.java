package com.metaisle.earlybird.data;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.SharedPreferences;

import com.metaisle.earlybird.app.OAuthActivity;

public class Prefs {

	public static final String PREFS_NAME = "earlybird.prefs";

	public static final String KEY_OAUTH_TOKEN = "oauth_token";
	public static final String KEY_OAUTH_TOKEN_SECRET = "oauth_token_secret";
	public static final String KEY_LAST_UPDATED_AT = "key_last_updated_at";
	public static final String KEY_ID = "KEY_ID";
	public static final String KEY_LAUNCHED_TIMES = "KEY_LAUNCHED_TIMES";
	public static final String KEY_DEBUG = "KEY_DEBUG";

	public static final int NOTIFICATION_TWEET_ID = 0x1;
	public static final int NOTIFICATION_RETWEET_ID = 0x2;

	public static final int conf_version = 1; // configure file version

	private static Twitter sTwitter;

	public static Twitter getTwitter(Context context) {

		SharedPreferences mPrefs = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);

		if (sTwitter == null) {
			String accessToken = mPrefs.getString(Prefs.KEY_OAUTH_TOKEN, null);
			String accessTokenSecret = mPrefs.getString(
					Prefs.KEY_OAUTH_TOKEN_SECRET, null);

			Configuration conf = new ConfigurationBuilder()
					.setOAuthConsumerKey(OAuthActivity.OAUTH_CONSUMER_KEY)
					.setOAuthConsumerSecret(OAuthActivity.OAUTH_CONSUMER_SECRET)
					.setOAuthAccessToken(accessToken)
					.setOAuthAccessTokenSecret(accessTokenSecret).build();
			sTwitter = new TwitterFactory(conf).getInstance();
		}
		return sTwitter;
	}

}