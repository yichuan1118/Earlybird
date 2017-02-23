package com.metaisle.earlybird.twitter;

import twitter4j.StatusUpdate;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;

import com.metaisle.earlybird.data.Prefs;
import com.metaisle.util.ErrorToast;

public class TweetTask extends AsyncTask<StatusUpdate, Void, Void> {
	private Context mContext;
	NotificationManager mNotificationManager;

	public TweetTask(Context context) {
		mContext = context;
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	protected Void doInBackground(StatusUpdate... params) {
		if (params == null || params.length == 0) {
			return null;
		}
		StatusUpdate status = params[0];
		Twitter twitter = Prefs.getTwitter(mContext);
		try {
			if (mContext instanceof Activity) {
				((Activity) mContext).runOnUiThread(new ErrorToast(mContext,
						"Sending tweets."));
			}
			twitter.updateStatus(status);
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
