package com.metaisle.earlybird.twitter;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Button;

import com.metaisle.earlybird.data.Prefs;

public class CheckFollowTask extends AsyncTask<Void, Void, Boolean> {
	private Context mContext;

	private long mTargetId;

	private Button mFollowButton;
	private Button mMsgButton;
	private boolean is_following_you = false;

	public CheckFollowTask(Context context, long target_id, Button followBtn,
			Button msgBtn) {
		mContext = context;
		mTargetId = target_id;
		mFollowButton = followBtn;
		mMsgButton = msgBtn;
	}

	@Override
	protected Boolean doInBackground(Void... params) {

		try {
			Twitter twitter = Prefs.getTwitter(mContext);
			long my_id = twitter.getId();

			is_following_you = twitter.existsFriendship(
					String.valueOf(mTargetId), String.valueOf(my_id));

			return twitter.existsFriendship(String.valueOf(my_id),
					String.valueOf(mTargetId));

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
		
		
		if(is_following_you){
			mMsgButton.setEnabled(true);
		}else{
			mMsgButton.setEnabled(false);
		}
	}
}
