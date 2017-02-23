package com.metaisle.earlybird.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.earlybird.R;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.UserTable;
import com.metaisle.earlybird.fragment.RelationshipFragment;
import com.metaisle.earlybird.fragment.TimelineFragment;
import com.metaisle.earlybird.lazyload.ImageLoader;
import com.metaisle.earlybird.twitter.CheckFollowTask;
import com.metaisle.earlybird.twitter.FollowTask;
import com.metaisle.earlybird.twitter.TimelineTask;
import com.metaisle.util.Util;

public class UserActivity extends SherlockFragmentActivity implements
		LoaderCallbacks<Cursor> {

	private ImageView profile_image;
	private TextView user_name;
	private TextView screen_name;
	private TextView description;
	private Button status_count;
	private Button friends_count;
	private Button followers_count;
	private Button follow_btn;
	private Button msg_btn;

	public static final String KEY_USER_ID = "key_user_id";

	private long mUserID;
	private ImageLoader imageLoader;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUserID = getIntent().getLongExtra(KEY_USER_ID, 0);
		if (mUserID == 0) {
			finish();
		}

		setContentView(R.layout.activity_user);

		profile_image = (ImageView) findViewById(R.id.profile_image);
		user_name = (TextView) findViewById(R.id.user_name);
		screen_name = (TextView) findViewById(R.id.screen_name);
		description = (TextView) findViewById(R.id.description);
		status_count = (Button) findViewById(R.id.status_count);
		friends_count = (Button) findViewById(R.id.friends_count);
		followers_count = (Button) findViewById(R.id.followers_count);
		follow_btn = (Button) findViewById(R.id.follow_btn);
		msg_btn = (Button) findViewById(R.id.msg_button);

		imageLoader = new ImageLoader(this.getApplicationContext());
		getSupportLoaderManager().initLoader(0, null, this);

		// -----
		status_count.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new TimelineTask(UserActivity.this, mUserID).execute();
				Intent i = new Intent(UserActivity.this, TimelineActivity.class);
				i.putExtra(TimelineFragment.KEY_OTHER_USER_ID, mUserID);
				startActivity(i);
			}
		});

		// -----
		friends_count.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(UserActivity.this,
						RelationshipActivity.class);
				i.putExtra(RelationshipFragment.KEY_USER_ID, mUserID);
				startActivity(i);
			}
		});

		followers_count.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(UserActivity.this,
						RelationshipActivity.class);
				i.putExtra(RelationshipFragment.KEY_USER_ID, mUserID);
				startActivity(i);
			}
		});

		// -----
		follow_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String t = follow_btn.getText().toString();
				follow_btn.setEnabled(false);
				if (t.equals("Unfollow")) {
					new FollowTask(UserActivity.this, mUserID, follow_btn)
							.execute();
				} else {
					Util.log("follow");
					new FollowTask(UserActivity.this, mUserID, follow_btn)
							.execute();
				}
			}
		});

		follow_btn.setEnabled(false);
		new CheckFollowTask(this, mUserID, follow_btn, msg_btn).execute();

		// -----
		msg_btn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(UserActivity.this, MessageActivity.class);
				i.putExtra(MessageActivity.KEY_IN_REPLY_TO, mUserID);
				i.putExtra(MessageActivity.KEY_ORI_TEXT, "");
				startActivity(i);
			}
		});
		msg_btn.setEnabled(false);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		Uri uri = Uri.parse(Provider.USER_CONTENT_URI + "/" + mUserID);

		return new CursorLoader(this, uri, new String[] { UserTable._ID,
				UserTable.PROFILE_IMAGE_URL, UserTable.USER_NAME,
				UserTable.SCREEN_NAME, UserTable.FRIENDS_COUNT,
				UserTable.FOLLOWERS_COUNT, UserTable.DESCRIPTION,
				UserTable.STATUS_COUNT }, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		cursor.moveToFirst();

		imageLoader.DisplayImage(cursor.getString(cursor
				.getColumnIndex(UserTable.PROFILE_IMAGE_URL)), profile_image);

		user_name.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.USER_NAME)));

		screen_name.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.SCREEN_NAME)));

		description.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.DESCRIPTION)));

		status_count.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.STATUS_COUNT)) + "\ntweets");

		friends_count.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.FRIENDS_COUNT)) + "\nfollowing");

		followers_count.setText(cursor.getString(cursor
				.getColumnIndex(UserTable.FOLLOWERS_COUNT)) + "\nfollowers");

	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		profile_image.setImageDrawable(null);
		user_name.setText(null);
		screen_name.setText(null);
	}

}
