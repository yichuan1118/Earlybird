package com.metaisle.earlybird.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.metaisle.earlybird.R;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.TimelineTable;

public class RetweetTask extends AsyncTask<Long, Void, Void> {
	private Context mContext;
	NotificationManager mNotificationManager;

	public RetweetTask(Context context) {
		mContext = context;
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected Void doInBackground(Long... params) {
		long id = params[0];
		Twitter twitter = Prefs.getTwitter(mContext);

		CharSequence tickerText = "Retweeting.";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(R.drawable.ic_launcher,
				tickerText, when);
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(mContext, "", "", contentIntent);
		mNotificationManager
				.notify(Prefs.NOTIFICATION_RETWEET_ID, notification);
		try {
			twitter.retweetStatus(id);
			// -----------------------------
			ContentValues values = new ContentValues();
			values.put(TimelineTable.IS_RETWEETED_BY_ME, true);
			mContext.getContentResolver().update(
					Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI, "" + id),
					values, null, null);
			// -----------------------------
			mNotificationManager.cancel(Prefs.NOTIFICATION_RETWEET_ID);
		} catch (TwitterException e) {
			e.printStackTrace();
		}

		return null;
	}

}
