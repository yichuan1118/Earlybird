package com.metaisle.earlybird.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;

import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.TimelineTable;
import com.metaisle.util.ErrorToast;

public class FavoriteTask extends AsyncTask<Long, Void, Void> {
	private Context mContext;
	NotificationManager mNotificationManager;
	boolean mIsCreateFavorite;

	public FavoriteTask(Context context, boolean is_create) {
		mContext = context;
		mNotificationManager = (NotificationManager) mContext
				.getSystemService(Context.NOTIFICATION_SERVICE);
		mIsCreateFavorite = is_create;
	}

	@Override
	protected Void doInBackground(Long... params) {
		if (params == null || params.length == 0) {
			return null;
		}

		long id = params[0];

		Twitter twitter = Prefs.getTwitter(mContext);

		try {
			if (mIsCreateFavorite) {
				twitter.createFavorite(id);

				// -----------------------------
				ContentValues values = new ContentValues();
				values.put(TimelineTable.IS_FAVORITED, true);
				mContext.getContentResolver().update(
						Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI,
								String.valueOf(id)), values, null, null);
				// -----------------------------
			} else {
				twitter.destroyFavorite(id);

				// -----------------------------
				ContentValues values = new ContentValues();
				values.put(TimelineTable.IS_FAVORITED, false);
				mContext.getContentResolver().update(
						Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI,
								String.valueOf(id)), values, null, null);
				// -----------------------------
			}
		} catch (TwitterException e) {
			if (mContext instanceof Activity) {
				((Activity) mContext).runOnUiThread(new ErrorToast(mContext,
						"Error while modifying favorite."));
			}
			e.printStackTrace();
		}

		return null;
	}

}
