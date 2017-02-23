package com.metaisle.earlybird.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;

import com.metaisle.earlybird.data.Prefs;
import com.metaisle.util.ErrorToast;

public class MessageTask extends AsyncTask<Void, Void, Void> {
	private Context mContext;
	NotificationManager mNotificationManager;
	private String mMsg;
	private long mID;

	public MessageTask(Context context, String msg, long id) {
		mContext = context;
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mMsg = msg;
		mID = id;
	}

	@Override
	protected Void doInBackground(Void... params) {

		Twitter twitter = Prefs.getTwitter(mContext);
		try {
			if (mContext instanceof Activity) {
				((Activity) mContext).runOnUiThread(new ErrorToast(mContext,
						"Sending tweets."));
			}
			twitter.sendDirectMessage(mID, mMsg);
		} catch (TwitterException e) {
			e.printStackTrace();
			if (mContext instanceof Activity) {
				((Activity) mContext).runOnUiThread(new ErrorToast(mContext,
						"Error while updating tweets."));
			}
		} finally {
			mNotificationManager.cancel(Prefs.NOTIFICATION_TWEET_ID);
		}
		return null;
	}

}
