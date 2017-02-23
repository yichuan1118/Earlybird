package com.metaisle.earlybird.twitter;

import twitter4j.IDs;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.integralblue.httpresponsecache.compat.java.util.Arrays;
import com.metaisle.earlybird.app.OAuthActivity;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.RelationshipTable;
import com.metaisle.util.Util;

public class GetFollowersTask extends AsyncTask<Void, Void, Void> {
	private Context mContext;
	private SharedPreferences mPrefs;

	private SharedPreferences followers_prefs;
	public static final String PREFS_FOLLOWERS_CURSOR = "PREFS_FOLLOWERS_CURSOR";
	public static final String KEY_FOLLOWERS_CURSOR_OF_ = "KEY_FOLLOWERS_CURSOR_OF_";

	private long mUserID = -1;
	private long mCursor = -1;

	private PullToRefreshListView mPtr;

	public GetFollowersTask setCursor(long cursor) {
		mCursor = cursor;
		return this;
	}

	public GetFollowersTask setUserID(long id) {
		mUserID = id;
		return this;
	}

	public GetFollowersTask setPtr(PullToRefreshListView ptr) {
		mPtr = ptr;
		return this;
	}

	public GetFollowersTask(Context context) {
		mContext = context;
		mPrefs = context.getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);

		followers_prefs = mContext.getSharedPreferences(PREFS_FOLLOWERS_CURSOR,
				Context.MODE_PRIVATE);
	}

	@Override
	protected Void doInBackground(Void... params) {
		String accessToken = mPrefs.getString(Prefs.KEY_OAUTH_TOKEN, null);
		String accessTokenSecret = mPrefs.getString(
				Prefs.KEY_OAUTH_TOKEN_SECRET, null);

		Configuration conf = new ConfigurationBuilder()
				.setOAuthConsumerKey(OAuthActivity.OAUTH_CONSUMER_KEY)
				.setOAuthConsumerSecret(OAuthActivity.OAUTH_CONSUMER_SECRET)
				.setOAuthAccessToken(accessToken).setJSONStoreEnabled(true)
				.setOAuthAccessTokenSecret(accessTokenSecret).build();
		Twitter t = new TwitterFactory(conf).getInstance();

		try {
			if (mUserID < 0) {
				mUserID = t.getId();
			}

			Util.log("mUserID " + mUserID);
			Util.log("mCursor " + mCursor);

			IDs ids = null;
			ids = t.getFollowersIDs(mUserID, mCursor);

			followers_prefs.edit()
					.putLong(String.valueOf(mUserID), ids.getNextCursor())
					.commit();

			if (mCursor < 0)
				mContext.getContentResolver().delete(
						Provider.FRIEND_CONTENT_URI,
						RelationshipTable.FRIEND + "=?",
						new String[] { String.valueOf(mUserID) });

			long[] ids_arr = ids.getIDs();
			if (ids_arr.length > 100) {
				ids_arr = Arrays.copyOfRange(ids_arr, 0, 100);
			}

			ResponseList<User> users = t.lookupUsers(ids_arr);
			for (User u : users) {
				Util.log(u.getName());
				TimelineTask.insertUser(mContext, u);

				ContentValues cvs = new ContentValues();
				cvs.put(RelationshipTable.FRIEND, mUserID);
				cvs.put(RelationshipTable.FOLLOWER, u.getId());
				mContext.getContentResolver().insert(
						Provider.FOLLOWER_CONTENT_URI, cvs);
			}

			// for (long id : ids.getIDs()) {
			// ContentValues cvs = new ContentValues();
			// cvs.put(RelationshipTable.FRIEND, mUserID);
			// cvs.put(RelationshipTable.FOLLOWER, id);
			// mContext.getContentResolver().insert(
			// Provider.FOLLOWER_CONTENT_URI, cvs);
			// }

		} catch (TwitterException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (mPtr != null) {
			mPtr.onRefreshComplete();
		}
	}
}
