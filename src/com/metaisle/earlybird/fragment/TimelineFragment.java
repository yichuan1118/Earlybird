package com.metaisle.earlybird.fragment;

import twitter4j.Paging;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.text.format.DateUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.metaisle.earlybird.R;
import com.metaisle.earlybird.app.StatusActivity;
import com.metaisle.earlybird.app.UserActivity;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.TimelineTable;
import com.metaisle.earlybird.data.UserTable;
import com.metaisle.earlybird.lazyload.ImageLoader;
import com.metaisle.earlybird.twitter.TimelineTask;
import com.metaisle.util.Util;

public class TimelineFragment extends SherlockFragment implements
		LoaderCallbacks<Cursor>, OnScrollListener, IRefreshable {
	public SharedPreferences mPrefs;
	private int mFragmentType;
	public static final int TYPE_HOME = 101;
	public static final int TYPE_MENTION = 102;
	public static final int TYPE_FAVORITE = 103;
	public static final int TYPE_OTHER_USER = 104;

	private long mOtherUserId;

	public static final String KEY_FRAGEMENT_TYPE = "key_fragement_type";
	public static final String KEY_OTHER_USER_ID = "key_user_id";

	private SimpleCursorAdapter mAdapter;
	public ImageLoader imageLoader;

	private PullToRefreshListView mPtrListView;
	private ListView mListView;

	private TimelineTask mTask;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = getActivity().getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		imageLoader = new ImageLoader(getActivity().getApplicationContext());

		mFragmentType = TYPE_HOME;
		Bundle b = getArguments();
		if (b != null) {
			mFragmentType = b.getInt(KEY_FRAGEMENT_TYPE);
			Util.log("Timeline type: " + mFragmentType);
			mOtherUserId = b.getLong(KEY_OTHER_USER_ID);
			Util.log("User ID: " + mOtherUserId);
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		mPtrListView = (PullToRefreshListView) inflater.inflate(
				R.layout.fragment_timeline, null);

		mListView = mPtrListView.getRefreshableView();
		// ------------------
		LinearLayout ll = new LinearLayout(getActivity());
		TextView tv = new TextView(getActivity());
		tv.setText("Loading...");
		LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
				LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER;
		tv.setLayoutParams(lp);
		tv.setGravity(Gravity.CENTER);
		ll.addView(tv, lp);
		mListView.addFooterView(ll);
		// ------------------
		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.fragment_timeline_item, null, new String[] {
						UserTable.PROFILE_IMAGE_URL, UserTable.USER_NAME,
						TimelineTable.STATUS_TEXT, UserTable.SCREEN_NAME,
						TimelineTable.IS_RETWEETED_BY_ME,
						TimelineTable.RT_USER_NAME, TimelineTable.CREATED_AT },
				new int[] { R.id.profile_image, R.id.user_name,
						R.id.status_text, R.id.screen_name, R.id.retweet_badge,
						R.id.retweeted_by, R.id.time_span }, 0);

		mAdapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {

				((View) view.getParent()).setTag(R.id.TAG_STATUS_ID,
						cursor.getLong(cursor
								.getColumnIndex(TimelineTable.STATUS_ID)));

				// -------------------------------------------------
				if (view.getId() == R.id.retweeted_by) {
					final int is_retweet = cursor.getInt(cursor
							.getColumnIndex(TimelineTable.IS_RETWEET));
					if (is_retweet != 0) {
						String rt_user = cursor.getString(cursor
								.getColumnIndex(TimelineTable.RT_USER_NAME));

						((TextView) view).setText("Retweeted by " + rt_user);
						view.setVisibility(View.VISIBLE);
						return true;
					} else {
						view.setVisibility(View.GONE);
					}

				}

				// -------------------------------------------------
				else if (view.getId() == R.id.profile_image) {
					final long uid = cursor.getLong(cursor
							.getColumnIndex(TimelineTable.FROM_ID));
					view.setTag(R.id.TAG_USER_ID, uid);
					imageLoader.DisplayImage(cursor.getString(columnIndex),
							(ImageView) view);

					view.setOnClickListener(new OnClickListener() {

						@Override
						public void onClick(View v) {
							Intent i = new Intent(getActivity(),
									UserActivity.class);
							i.putExtra(UserActivity.KEY_USER_ID, uid);
							startActivity(i);
						}
					});

					return true;
				}

				// -------------------------------------------------
				else if (view.getId() == R.id.retweet_badge) {
					final int retweet_by_me = cursor.getInt(cursor
							.getColumnIndex(TimelineTable.IS_RETWEETED_BY_ME));
					if (retweet_by_me == 0) {
						((ImageView) view).setImageDrawable(null);
					} else {
						((ImageView) view)
								.setImageResource(R.drawable.ic_action_rt_on);
					}
					return true;
				}

				// -------------------------------------------------
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
		mListView.setOnScrollListener(this);

		getLoaderManager().initLoader(0, null, this);

		mListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> l, View v, int position,
					long id) {
				long status_id = (Long) v.getTag(R.id.TAG_STATUS_ID);
				Util.log("click " + status_id);
				Util.profile(getActivity(), "status_click.csv", "" + status_id
						+ " click");
				Intent i = new Intent(getActivity(), StatusActivity.class);
				i.putExtra(StatusActivity.KEY_STATUS_ID, status_id);
				startActivity(i);
			}

		});

		mPtrListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				doRefresh();
			}
		});

		return mPtrListView;
	}

	private void doRefresh() {

		Cursor c = mAdapter.getCursor();

		Util.log("Cursor count " + c.getCount());

		if (c == null || c.getCount() < 1) {
			if (mTask == null
					|| mTask.getStatus() != android.os.AsyncTask.Status.RUNNING) {
				mTask = new TimelineTask(getActivity(), new Paging[] {
						new Paging(), new Paging(), new Paging(), null },
						mPtrListView);
				mTask.execute();
			}
			return;
		}

		c.moveToFirst();
		long status_id = c.getLong(1);
		Util.log("Refresh, since id " + status_id);
		Paging p = new Paging().sinceId(status_id);
		Paging[] pagings = null;

		if (mTask == null
				|| mTask.getStatus() != android.os.AsyncTask.Status.RUNNING) {
			switch (mFragmentType) {
			case TYPE_HOME:
				pagings = new Paging[] { p, null, null, null };
				mTask = new TimelineTask(getActivity(), pagings, mPtrListView);
				mTask.execute();
				break;
			case TYPE_MENTION:
				pagings = new Paging[] { null, p, null, null };
				mTask = new TimelineTask(getActivity(), pagings, mPtrListView);
				mTask.execute();
				break;
			case TYPE_FAVORITE:
				pagings = new Paging[] { null, null, p, null };
				mTask = new TimelineTask(getActivity(), pagings, mPtrListView);
				mTask.execute();
				break;
			case TYPE_OTHER_USER:
				mTask = new TimelineTask(getActivity(), mOtherUserId, p,
						mPtrListView);
				mTask.execute();
				break;
			default:
				break;
			}
		}

	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String selection = null;
		String[] selectionArg = null;

		switch (mFragmentType) {
		case TYPE_HOME:
			selection = TimelineTable.IS_HOME + "=?";
			selectionArg = new String[] { "1" };
			break;
		case TYPE_MENTION:
			selection = TimelineTable.IS_MENTION + "=?";
			selectionArg = new String[] { "1" };
			break;
		case TYPE_FAVORITE:
			selection = TimelineTable.IS_FAVORITED + "=?";
			selectionArg = new String[] { "1" };
			break;
		case TYPE_OTHER_USER:
			selection = TimelineTable.USER_TIMELINE + "=?";
			selectionArg = new String[] { String.valueOf(mOtherUserId) };
			break;

		default:
			throw new IllegalArgumentException("Unknown Fragment type. ");
		}
		// TimelineTable.STATUS_ID has to be index 1!!!
		return new CursorLoader(getActivity(), Provider.TIMELINE_CONTENT_URI,
				new String[] {
						TimelineTable.TIMELINE_TABLE + "." + TimelineTable._ID,
						TimelineTable.STATUS_ID,
						UserTable.PROFILE_IMAGE_URL,
						UserTable.USER_TABLE + "." + UserTable.USER_NAME,
						TimelineTable.STATUS_TEXT,
						TimelineTable.FROM_ID,
						UserTable.SCREEN_NAME,
						TimelineTable.IS_RETWEETED_BY_ME,
						TimelineTable.IS_RETWEET,
						TimelineTable.RT_USER_NAME,
						TimelineTable.STATUS_TEXT,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.CREATED_AT }, selection,
				selectionArg, TimelineTable.TIMELINE_TABLE + "."
						+ TimelineTable.CREATED_AT + " DESC");
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
		// Util.log("firstVisibleItem " + firstVisibleItem +
		// " visibleItemCount "
		// + visibleItemCount + " totalItemCount " + totalItemCount);
		if (totalItemCount < 10) {
			return;
		}

		boolean loadMore = /* maybe add a padding */
		firstVisibleItem + visibleItemCount >= totalItemCount;

		if (loadMore) {
			Cursor c = mAdapter.getCursor();

			Util.log("Cursor count " + c.getCount());

			c.moveToLast();
			long status_id = c.getLong(1);
			Util.log("Load more, last id " + status_id);
			Paging p = new Paging().maxId(status_id - 1);
			Paging[] pagings = null;

			if (mTask == null
					|| mTask.getStatus() != android.os.AsyncTask.Status.RUNNING) {
				switch (mFragmentType) {
				case TYPE_HOME:
					pagings = new Paging[] { p, null, null, null };
					mTask = new TimelineTask(getActivity(), pagings);
					mTask.execute();
					break;
				case TYPE_MENTION:
					pagings = new Paging[] { null, p, null, null };
					mTask = new TimelineTask(getActivity(), pagings);
					mTask.execute();
					break;
				case TYPE_FAVORITE:
					pagings = new Paging[] { null, null, p, null };
					mTask = new TimelineTask(getActivity(), pagings);
					mTask.execute();
					break;
				case TYPE_OTHER_USER:
					mTask = new TimelineTask(getActivity(), mOtherUserId, p);
					mTask.execute();
					break;
				default:
					break;
				}
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
