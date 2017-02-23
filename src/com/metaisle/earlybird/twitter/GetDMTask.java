package com.metaisle.earlybird.twitter;

import java.util.List;

import twitter4j.Paging;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import android.content.Context;
import android.os.AsyncTask;

import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.util.Util;

public class GetDMTask extends AsyncTask<Void, Void, Void> {

	private Context mContext;

	private PullToRefreshListView mPtr = null;

	Paging paging = new Paging();
	Paging sent_paging = new Paging();

	public GetDMTask(Context context) {
		mContext = context;
	}

	public GetDMTask setSince(long id) {
		paging.setSinceId(id);
		return this;
	}

	public GetDMTask setMax(long id) {
		paging.setMaxId(id);
		return this;
	}

	public GetDMTask setSentSince(long id) {
		sent_paging.setSinceId(id);
		return this;
	}

	public GetDMTask setSentMax(long id) {
		sent_paging.setMaxId(id);
		return this;
	}

	public GetDMTask setPtr(PullToRefreshListView ptr) {
		mPtr = ptr;
		return this;
	}

	@Override
	protected Void doInBackground(Void... params) {
		Twitter t = Prefs.getTwitter(mContext);

		List<twitter4j.DirectMessage> msgs;
		try {
			msgs = t.getDirectMessages(paging);
			for (twitter4j.DirectMessage m : msgs) {
				TimelineTask.insertMessage(mContext, m);
			}

			msgs = t.getSentDirectMessages(sent_paging);
			for (twitter4j.DirectMessage m : msgs) {
				TimelineTask.insertMessage(mContext, m);
			}
		} catch (TwitterException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		Util.log("finished");
		if (mPtr != null) {
			mPtr.onRefreshComplete();
		}
	}
}
