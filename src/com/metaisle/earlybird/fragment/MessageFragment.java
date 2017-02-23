package com.metaisle.earlybird.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.metaisle.earlybird.R;
import com.metaisle.earlybird.app.MessageActivity;
import com.metaisle.earlybird.data.MessageTable;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.UserTable;
import com.metaisle.earlybird.lazyload.ImageLoader;
import com.metaisle.earlybird.twitter.GetDMTask;
import com.metaisle.util.Util;

public class MessageFragment extends SherlockFragment implements
		LoaderCallbacks<Cursor>, OnScrollListener, IRefreshable {
	private SimpleCursorAdapter mAdapter;
	public ImageLoader imageLoader;
	public SharedPreferences mPrefs;
	private PullToRefreshListView mPtrListView;
	private ListView mListView;

	private GetDMTask mTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = getActivity().getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		imageLoader = new ImageLoader(getActivity().getApplicationContext());
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), Provider.MESSAGE_CONTENT_URI,
				new String[] {
						MessageTable.MESSAGE_TABLE + "." + MessageTable._ID,
						MessageTable.STATUS_ID,
						UserTable.PROFILE_IMAGE_URL,
						UserTable.USER_NAME,
						MessageTable.STATUS_TEXT,
						MessageTable.SENDER_ID,
						MessageTable.RECIPIENT_ID,
						MessageTable.MESSAGE_TABLE + "."
								+ MessageTable.CREATED_AT }, null, null,
				MessageTable.MESSAGE_TABLE + "." + MessageTable.CREATED_AT
						+ " DESC");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mPtrListView = (PullToRefreshListView) inflater.inflate(
				R.layout.fragment_timeline, null);

		mListView = mPtrListView.getRefreshableView();
		// ------------------
		FrameLayout fl = (FrameLayout) inflater.inflate(
				R.layout.footer_loading, null);
		mListView.addFooterView(fl);
		// ------------------

		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.fragment_timeline_item, null, new String[] {
						UserTable.PROFILE_IMAGE_URL, UserTable.USER_NAME,
						MessageTable.STATUS_TEXT, UserTable.USER_NAME,
						UserTable.USER_NAME, MessageTable.CREATED_AT },
				new int[] { R.id.profile_image, R.id.user_name,
						R.id.status_text, R.id.screen_name, R.id.retweeted_by,
						R.id.time_span }, 0);

		mAdapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				((View) view.getParent()).setTag(R.id.TAG_STATUS_ID,
						cursor.getLong(1));

				if (view.getId() == R.id.profile_image) {
					imageLoader.DisplayImage(cursor.getString(columnIndex),
							(ImageView) view);

					return true;
				}

				else if (view.getId() == R.id.retweeted_by) {
					view.setVisibility(View.GONE);
				}

				else if (view.getId() == R.id.screen_name) {
					view.setVisibility(View.GONE);
				}

				else if (view.getId() == R.id.time_span) {
					long time = cursor.getLong(columnIndex);
					((TextView) view).setText(DateUtils
							.getRelativeTimeSpanString(time));
					return true;
				}
				return false;
			}
		});

		mListView.setAdapter(mAdapter);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position,
					long id) {
				Util.log("onitemclick");
				Cursor c = mAdapter.getCursor();
				c.moveToPosition(position - 1);
				long sender_id = c.getLong(c
						.getColumnIndex(MessageTable.SENDER_ID));
				String ori_text = c.getString(c
						.getColumnIndex(MessageTable.STATUS_TEXT));

				Util.log(ori_text);

				Intent i = new Intent(getActivity(), MessageActivity.class);
				i.putExtra(MessageActivity.KEY_IN_REPLY_TO, sender_id);
				i.putExtra(MessageActivity.KEY_ORI_TEXT, ori_text);
				startActivity(i);
			}

		});

		mListView.setOnScrollListener(this);

		getLoaderManager().initLoader(0, null, this);

		mPtrListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				doRefresh();
			}
		});

		getLoaderManager().initLoader(0, null, this);

		return mPtrListView;
	}

	private void doRefresh() {

		Cursor c = mAdapter.getCursor();

		Util.log("Cursor count " + c.getCount());

		if (c == null || c.getCount() < 1) {
			if (mTask == null || mTask.getStatus() != Status.RUNNING) {
				mTask = new GetDMTask(getActivity()).setPtr(mPtrListView);
				mTask.execute();
			}
			return;
		}

		if (mTask == null || mTask.getStatus() != Status.RUNNING) {
			long since = getSinceID();
			long sent_since = getSentSinceID();
			mTask = new GetDMTask(getActivity()).setPtr(mPtrListView);
			if (since > 0) {
				mTask.setSince(since);
			}
			if (sent_since > 0) {
				mTask.setSentSince(sent_since);
			}
			mTask.execute();
		}

	}

	private long getSinceID() {
		long myid = mPrefs.getLong(Prefs.KEY_ID, -1);
		Cursor c = mAdapter.getCursor();

		if (!c.moveToFirst()) {
			return -1;
		}

		do {
			long recp_id = c.getLong(c
					.getColumnIndex(MessageTable.RECIPIENT_ID));
			if (recp_id == myid) {
				return c.getLong(c.getColumnIndex(MessageTable.STATUS_ID));
			}
		} while (c.moveToNext());

		return -1;
	}

	private long getSentSinceID() {
		long myid = mPrefs.getLong(Prefs.KEY_ID, -1);
		Cursor c = mAdapter.getCursor();

		if (!c.moveToFirst()) {
			return -1;
		}

		do {
			long sent_id = c.getLong(c.getColumnIndex(MessageTable.SENDER_ID));
			if (sent_id == myid) {
				return c.getLong(c.getColumnIndex(MessageTable.STATUS_ID));
			}
		} while (c.moveToNext());

		return -1;
	}

	private long getMaxID() {
		long myid = mPrefs.getLong(Prefs.KEY_ID, -1);
		Cursor c = mAdapter.getCursor();

		if (!c.moveToLast()) {
			return -1;
		}

		do {
			long recp_id = c.getLong(c
					.getColumnIndex(MessageTable.RECIPIENT_ID));
			if (recp_id == myid) {
				return c.getLong(c.getColumnIndex(MessageTable.STATUS_ID));
			}
		} while (c.moveToPrevious());

		return -1;
	}

	private long getSentMaxID() {
		long myid = mPrefs.getLong(Prefs.KEY_ID, -1);
		Cursor c = mAdapter.getCursor();

		if (!c.moveToLast()) {
			return -1;
		}

		do {
			long sent_id = c.getLong(c.getColumnIndex(MessageTable.SENDER_ID));
			if (sent_id == myid) {
				return c.getLong(c.getColumnIndex(MessageTable.STATUS_ID));
			}
		} while (c.moveToPrevious());

		return -1;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {

		boolean loadMore = /* maybe add a padding */
		firstVisibleItem + visibleItemCount >= totalItemCount;
		if (loadMore) {
			Util.log("Getting more");

			Cursor c = mAdapter.getCursor();

			if (c == null || c.getCount() == 0)
				return;

			Util.log("Cursor count " + c.getCount());

			if (mTask == null || mTask.getStatus() != Status.RUNNING) {
				long max = getMaxID();
				long sent_max = getSentMaxID();

				mTask = new GetDMTask(getActivity());
				if (max > 0) {
					mTask.setMax(max);
				}
				if (sent_max > 0) {
					mTask.setSentMax(sent_max);
				}
				mTask.execute();

			}
		}

	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
	}

	@Override
	public void refresh() {
		if (mPtrListView != null) {
			doRefresh();
			mListView.setSelection(0);
		}

	}

}
