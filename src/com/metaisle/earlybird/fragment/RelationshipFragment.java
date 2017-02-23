package com.metaisle.earlybird.fragment;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask.Status;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter.ViewBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.metaisle.earlybird.R;
import com.metaisle.earlybird.app.OAuthActivity;
import com.metaisle.earlybird.app.UserActivity;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.RelationshipTable;
import com.metaisle.earlybird.data.UserTable;
import com.metaisle.earlybird.lazyload.ImageLoader;
import com.metaisle.earlybird.twitter.GetFollowersTask;
import com.metaisle.earlybird.twitter.GetFriendsTask;
import com.metaisle.util.Util;

public class RelationshipFragment extends SherlockFragment implements
		LoaderCallbacks<Cursor>, OnScrollListener, IRefreshable {
	public static final String KEY_USER_ID = "KEY_USER_ID";

	private int CURRENT_LOADER = LOADER_FRIENDS;
	public static final int LOADER_FRIENDS = 0x101;
	public static final int LOADER_FOLLOWERS = 0x102;

	private SharedPreferences mPrefs;
	private long mUserID = -1;

	private PullToRefreshListView mPtrListView;
	private ListView mListView;

	private SimpleCursorAdapter mAdapter;
	private ImageLoader imageLoader;

	private Button mFriendsButton;
	private Button mFollowerButton;

	private GetFollowersTask followersTask = null;
	private GetFriendsTask friendsTask = null;

	private Handler mHandler;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mPrefs = getActivity().getSharedPreferences(Prefs.PREFS_NAME,
				Context.MODE_PRIVATE);
		imageLoader = new ImageLoader(getActivity().getApplicationContext());

		mHandler = new Handler() {
			public static final int MSG_ID = 1;

			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case MSG_ID:
					mUserID = msg.getData().getLong(KEY_USER_ID);
					getLoaderManager().restartLoader(CURRENT_LOADER, null,
							RelationshipFragment.this);
					break;

				default:
					break;
				}
			}
		};

		Bundle b = getArguments();
		if (b != null) {
			Util.log(b.toString());
			mUserID = b.getLong(KEY_USER_ID, -1);
			Util.log("mUserID: " + mUserID);
		}

		if (mUserID < 0) {
			mUserID = mPrefs.getLong(Prefs.KEY_ID, -1);
		}

		if (mUserID < 0) {
			new Thread() {
				@Override
				public void run() {
					String accessToken = mPrefs.getString(
							Prefs.KEY_OAUTH_TOKEN, null);
					String accessTokenSecret = mPrefs.getString(
							Prefs.KEY_OAUTH_TOKEN_SECRET, null);

					Configuration conf = new ConfigurationBuilder()
							.setOAuthConsumerKey(
									OAuthActivity.OAUTH_CONSUMER_KEY)
							.setOAuthConsumerSecret(
									OAuthActivity.OAUTH_CONSUMER_SECRET)
							.setOAuthAccessToken(accessToken)
							.setJSONStoreEnabled(true)
							.setOAuthAccessTokenSecret(accessTokenSecret)
							.build();
					Twitter t = new TwitterFactory(conf).getInstance();

					long id;
					try {
						id = t.getId();
						Bundle b = new Bundle();
						b.putLong(KEY_USER_ID, id);
						Message m = new Message();
						m.what = 1;
						m.setData(b);
						mHandler.sendMessage(m);
					} catch (IllegalStateException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (TwitterException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}.start();
		}
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle arg1) {
		String selection = null;
		String[] selectionArgs = null;
		Uri uri = null;
		if (id == LOADER_FOLLOWERS) {
			selection = RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FRIEND + "=?";
			selectionArgs = new String[] { String.valueOf(mUserID) };
			uri = Provider.FOLLOWER_CONTENT_URI;
		} else if (id == LOADER_FRIENDS) {
			selection = RelationshipTable.RELATIONSHIP_TABLE + "."
					+ RelationshipTable.FOLLOWER + "=?";
			selectionArgs = new String[] { String.valueOf(mUserID) };
			uri = Provider.FRIEND_CONTENT_URI;
		}

		return new CursorLoader(getActivity(), uri, new String[] {
				RelationshipTable.RELATIONSHIP_TABLE + "."
						+ RelationshipTable._ID,
				UserTable.USER_TABLE + "." + UserTable.PROFILE_IMAGE_URL,
				UserTable.USER_TABLE + "." + UserTable.USER_NAME,
				UserTable.USER_TABLE + "." + UserTable.USER_ID }, selection,
				selectionArgs, null);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.fragment_relationship, null);

		View view = v.findViewById(R.id.control_switch);
		view.setVisibility(View.VISIBLE);
		mFriendsButton = (Button) v.findViewById(R.id.friends_btn);
		mFollowerButton = (Button) v.findViewById(R.id.followers_btn);

		mFriendsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CURRENT_LOADER = LOADER_FRIENDS;
				getLoaderManager().restartLoader(CURRENT_LOADER, null,
						RelationshipFragment.this);
			}
		});

		mFollowerButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				CURRENT_LOADER = LOADER_FOLLOWERS;
				getLoaderManager().restartLoader(CURRENT_LOADER, null,
						RelationshipFragment.this);
			}
		});

		mPtrListView = (PullToRefreshListView) v
				.findViewById(R.id.relationship_list);

		mListView = mPtrListView.getRefreshableView();
		// ------------------
		FrameLayout fl = (FrameLayout) inflater.inflate(
				R.layout.footer_loading, null);
		mListView.addFooterView(fl);
		// ------------------

		mAdapter = new SimpleCursorAdapter(getActivity(),
				R.layout.fragment_relationship_item, null, new String[] {
						UserTable.PROFILE_IMAGE_URL, UserTable.USER_NAME },
				new int[] { R.id.profile_image, R.id.user_name }, 0);

		mAdapter.setViewBinder(new ViewBinder() {
			@Override
			public boolean setViewValue(View view, Cursor cursor,
					int columnIndex) {
				if (view.getId() == R.id.profile_image) {
					final long uid = cursor.getLong(cursor
							.getColumnIndex(UserTable.USER_ID));
					view.setTag(R.id.TAG_USER_ID, uid);

					imageLoader.DisplayImage(cursor.getString(columnIndex),
							(ImageView) view);
					return true;
				}

				return false;
			}
		});

		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				Cursor cursor = mAdapter.getCursor();
				final long uid = cursor.getLong(cursor
						.getColumnIndex(UserTable.USER_ID));
				Intent i = new Intent(getActivity(), UserActivity.class);
				i.putExtra(UserActivity.KEY_USER_ID, uid);
				startActivity(i);
			}
		});

		mListView.setAdapter(mAdapter);
		mListView.setOnScrollListener(this);

		mPtrListView.setOnRefreshListener(new OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				doRefresh();
			}
		});

		getLoaderManager().initLoader(CURRENT_LOADER, null, this);

		return v;
	}

	private void doRefresh() {

		Util.log("onRefresh " + mUserID);

		if (CURRENT_LOADER == LOADER_FRIENDS) {
			if (friendsTask == null
					|| friendsTask.getStatus() != Status.RUNNING) {
				friendsTask = (GetFriendsTask) new GetFriendsTask(getActivity())
						.setUserID(mUserID).setPtr(mPtrListView).execute();
			}
		} else if (CURRENT_LOADER == LOADER_FOLLOWERS) {
			if (followersTask == null
					|| followersTask.getStatus() != Status.RUNNING) {
				followersTask = (GetFollowersTask) new GetFollowersTask(
						getActivity()).setUserID(mUserID).setPtr(mPtrListView)
						.execute();
			}
		}

	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		Util.log("cursor " + data.getCount());

		if (data.getCount() == 0) {
			if (CURRENT_LOADER == LOADER_FRIENDS)
				new GetFriendsTask(getActivity()).setUserID(mUserID).execute();
			else if (CURRENT_LOADER == LOADER_FOLLOWERS)
				new GetFollowersTask(getActivity()).setUserID(mUserID)
						.execute();
		}
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
			if (CURRENT_LOADER == LOADER_FRIENDS
					&& (friendsTask == null || friendsTask.getStatus() != Status.RUNNING)) {
				long cursor = getActivity().getSharedPreferences(
						GetFriendsTask.KEY_FRIENDS_CURSOR_OF_,
						Context.MODE_PRIVATE).getLong("" + mUserID, -1);

				Util.log("friend cursor " + cursor);

				friendsTask = (GetFriendsTask) new GetFriendsTask(getActivity())
						.setUserID(mUserID).setPtr(mPtrListView)
						.setCursor(cursor).execute();

			} else if (CURRENT_LOADER == LOADER_FOLLOWERS
					&& (followersTask == null || followersTask.getStatus() != Status.RUNNING)) {
				long cursor = getActivity().getSharedPreferences(
						GetFollowersTask.PREFS_FOLLOWERS_CURSOR,
						Context.MODE_PRIVATE).getLong("" + mUserID, -1);

				Util.log("follower cursor " + cursor);

				followersTask = (GetFollowersTask) new GetFollowersTask(
						getActivity()).setUserID(mUserID).setPtr(mPtrListView)
						.setCursor(cursor).execute();
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
