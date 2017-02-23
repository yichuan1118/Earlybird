package com.metaisle.earlybird.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Button;

import com.metaisle.earlybird.data.Prefs;

public class FollowTask extends AsyncTask<Void, Void, Boolean> {
	private Context mContext;

	private long mTargetId;

	private Button mFollowButton;

	public FollowTask(Context context, long target_id, Button btn) {
		mContext = context;
		mTargetId = target_id;
		mFollowButton = btn;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		try {
			Twitter twitter = Prefs.getTwitter(mContext);
			if (mFollowButton.getText().equals("Follow")) {
				twitter.createFriendship(mTargetId);
				return true;
			} else {
				twitter.destroyFriendship(mTargetId);
				return false;
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (TwitterException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		mFollowButton.setEnabled(true);
		if (result) {
			mFollowButton.setText("Unfollow");
		} else {
			mFollowButton.setText("Follow");
		}
	}
}
