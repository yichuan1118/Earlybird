package com.metaisle.earlybird.twitter;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.format.Time;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.metaisle.earlybird.app.OAuthActivity;
import com.metaisle.earlybird.caching.CachingTask;
import com.metaisle.earlybird.data.MessageTable;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.TimelineTable;
import com.metaisle.earlybird.data.UserTable;
import com.metaisle.util.Util;

/*
 * Timeline
 */
public class TimelineTask extends AsyncTask<Void, Void, Void> {
	private Context mContext;
	private SharedPreferences mPrefs;

	private Paging[] mPagings;
	private long mOtherUserId = 0;

	public static final int MAX_TWEETS_IN_DB = 1000;

	private PullToRefreshListView mPtr = null;

	public TimelineTask(Context context, Paging[] paging) {
		mContext = context;
		mPrefs = context.getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		mPagings = paging;
	}

	public TimelineTask(Context context, long user_id) {
		mContext = context;
		mPrefs = context.getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		mPagings = new Paging[] { null, null, null, null, new Paging() };
		mOtherUserId = user_id;
	}

	public TimelineTask(Context context, long user_id, Paging paging) {
		this(context, user_id);
		mPagings = new Paging[] { null, null, null, null, paging };
	}

	public TimelineTask(Context context, long user_id, Paging paging,
			PullToRefreshListView ptr) {
		this(context, user_id);
		mPagings = new Paging[] { null, null, null, null, paging };
		mPtr = ptr;
	}

	public TimelineTask(Context context, Paging[] paging,
			PullToRefreshListView ptr) {
		this(context, paging);
		mPtr = ptr;
	}

	@Override
	protected Void doInBackground(Void... nulls) {
		String accessToken = mPrefs.getString(Prefs.KEY_OAUTH_TOKEN, null);
		String accessTokenSecret = mPrefs.getString(
				Prefs.KEY_OAUTH_TOKEN_SECRET, null);

		Configuration conf = new ConfigurationBuilder()
				.setOAuthConsumerKey(OAuthActivity.OAUTH_CONSUMER_KEY)
				.setOAuthConsumerSecret(OAuthActivity.OAUTH_CONSUMER_SECRET)
				.setOAuthAccessToken(accessToken).setJSONStoreEnabled(true)
				.setOAuthAccessTokenSecret(accessTokenSecret).build();
		Twitter t = new TwitterFactory(conf).getInstance();
		List<twitter4j.Status> statuses = null;
		try {
			if (mPagings[0] != null) {
				Util.log("Downlaod home");
				statuses = t.getHomeTimeline(mPagings[0]);

				String json = DataObjectFactory.getRawJSON(statuses);

				Util.profile(mContext, "home_timeline.csv", json);

				for (twitter4j.Status s : statuses) {
					insertStatus(mContext, s, TimelineTable.IS_HOME, "1");
				}
			}

			if (mPagings[1] != null) {
				Util.log("Downlaod mention");
				statuses = t.getMentions(mPagings[1]);
				for (twitter4j.Status s : statuses) {
					insertStatus(mContext, s, TimelineTable.IS_MENTION, "1");
				}
			}

			if (mPagings[2] != null) {
				Util.log("Downlaod favorite");
				statuses = t.getFavorites(mPagings[2]);
				for (twitter4j.Status s : statuses) {
					insertStatus(mContext, s, TimelineTable.IS_FAVORITED, "1");
				}
			}

			if (mPagings[3] != null) {
				Util.log("Downlaod DM");
				List<twitter4j.DirectMessage> msgs = t
						.getDirectMessages(mPagings[3]);
				for (twitter4j.DirectMessage m : msgs) {
					insertMessage(mContext, m);
				}
				
			}

			if (mOtherUserId != 0) {
				Util.log("Downlaod User Timeline");
				statuses = t.getUserTimeline(mOtherUserId, mPagings[4]);
				for (twitter4j.Status s : statuses) {
					insertStatus(mContext, s, TimelineTable.USER_TIMELINE,
							String.valueOf(mOtherUserId));
				}
			}
		} catch (TwitterException e) {
			e.printStackTrace();
			Activity a = (Activity) mContext;
			a.runOnUiThread(mDownloadErrorToastRunnable);
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
		Time now = new Time();
		now.setToNow();
		mPrefs.edit().putLong(Prefs.KEY_LAST_UPDATED_AT, now.toMillis(true))
				.commit();
		new CachingTask(mContext).execute();
		if (mPtr != null) {
			mPtr.onRefreshComplete();
		}
	}

	/*
	 * Misc.
	 */
	public static void insertStatus(Context context, twitter4j.Status s,
			String key, String value) {
		// ----------------------------------------------------------
		ContentValues values = new ContentValues();
		values.put(key, value);
		values.put(TimelineTable.CREATED_AT, s.getCreatedAt().getTime());
		values.put(TimelineTable.IS_RETWEETED_BY_ME, s.isRetweetedByMe());
		values.put(TimelineTable.IS_FAVORITED, s.isFavorited());
		values.put(TimelineTable.IS_RETWEET, s.isRetweet());

		if (s.isRetweet()) {
			values.put(TimelineTable.STATUS_ID, s.getRetweetedStatus().getId());
			values.put(TimelineTable.STATUS_TEXT, s.getRetweetedStatus()
					.getText());
			values.put(TimelineTable.STATUS_TEXT_EXP, s.getRetweetedStatus()
					.getText());
			values.put(TimelineTable.FROM_ID, s.getRetweetedStatus().getUser()
					.getId());

			values.put(TimelineTable.RT_STATUS_ID, s.getId());
			values.put(TimelineTable.RT_USER_ID, s.getUser().getId());
			values.put(TimelineTable.RT_USER_NAME, s.getUser().getName());

		} else {
			values.put(TimelineTable.STATUS_ID, s.getId());
			values.put(TimelineTable.STATUS_TEXT, s.getText());
			values.put(TimelineTable.STATUS_TEXT_EXP, s.getText());
			values.put(TimelineTable.FROM_ID, s.getUser().getId());
		}

		context.getContentResolver().insert(Provider.TIMELINE_CONTENT_URI,
				values);

		// ----------------------------------------------------------
		if (s.isRetweet()) {
			insertUser(context, s.getRetweetedStatus().getUser());
		}

		// ----------------------------------------------------------
		insertUser(context, s.getUser());
	}

	public static void insertUser(Context context, User u) {
		ContentValues values = new ContentValues();
		String bigger_url = getBiggerImageUrl(u.getProfileImageURL().toString());
		values.put(UserTable.USER_ID, u.getId());
		values.put(UserTable.CREATED_AT, u.getCreatedAt().getTime());
		values.put(UserTable.DESCRIPTION, u.getDescription());
		values.put(UserTable.FOLLOWERS_COUNT, u.getFollowersCount());
		values.put(UserTable.FRIENDS_COUNT, u.getFriendsCount());
		values.put(UserTable.USER_NAME, u.getName());
		values.put(UserTable.SCREEN_NAME, "@" + u.getScreenName());
		values.put(UserTable.PROFILE_IMAGE_URL, bigger_url);
		values.put(UserTable.STATUS_COUNT, u.getStatusesCount());

		context.getContentResolver().insert(Provider.USER_CONTENT_URI, values);
	}

	public static void insertMessage(Context context, twitter4j.DirectMessage s) {
		ContentValues values = new ContentValues();
		values.put(MessageTable.STATUS_ID, s.getId());
		values.put(MessageTable.STATUS_TEXT, s.getText());
		values.put(MessageTable.CREATED_AT, s.getCreatedAt().getTime());
		values.put(MessageTable.RECIPIENT_NAME, s.getRecipient().getName());
		values.put(MessageTable.RECIPIENT_ID, s.getRecipientId());
		values.put(MessageTable.SENDER_NAME, s.getSender().getName());
		values.put(MessageTable.SENDER_ID, s.getSenderId());

		context.getContentResolver().insert(Provider.MESSAGE_CONTENT_URI,
				values);

		insertUser(context, s.getSender());
	}

	final Runnable mDownloadErrorToastRunnable = new Runnable() {
		public void run() {
			Toast.makeText(mContext, "Timeline fetch error, try again later",
					Toast.LENGTH_SHORT).show();
		}
	};

	public static String getBiggerImageUrl(String url) {
		String value = url;
		if ((url != null) && (url.length() > 0)) {
			value = url.replaceFirst("normal", "bigger");
		}
		return value;
	}

}