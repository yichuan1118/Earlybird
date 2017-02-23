package com.metaisle.earlybird.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.earlybird.R;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.TimelineTable;
import com.metaisle.earlybird.data.UserTable;
import com.metaisle.earlybird.lazyload.ImageLoader;
import com.metaisle.earlybird.twitter.FavoriteTask;
import com.metaisle.earlybird.twitter.RetweetTask;
import com.metaisle.util.Util;

public class StatusActivity extends SherlockFragmentActivity implements
		LoaderCallbacks<Cursor> {
	public static final String KEY_STATUS_ID = "key_status_id";

	private ImageLoader imageLoader;

	private ImageButton mReplyButton;
	private ImageButton mRetweetButton;
	private ImageButton mFavoriteButton;

	private long mStatusID;

	private long mIsRetweetedByMe;
	private int mIsFavorited;

	private TextView mRetweetBy;
	private TextView mTimeView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_status);

		mReplyButton = (ImageButton) findViewById(R.id.reply_button);
		mRetweetButton = (ImageButton) findViewById(R.id.retweet_button);
		mFavoriteButton = (ImageButton) findViewById(R.id.favorite_button);

		mRetweetBy = (TextView) findViewById(R.id.retweeted_by);
		mTimeView = (TextView) findViewById(R.id.time_span);

		imageLoader = new ImageLoader(this.getApplicationContext());

		mStatusID = getIntent().getLongExtra(KEY_STATUS_ID, -1);
		if (mStatusID == -1) {
			Util.log("status id wrong");
			finish();
		}

		getSupportLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {

		return new CursorLoader(this, Provider.TIMELINE_CONTENT_URI,
				new String[] {
						TimelineTable.TIMELINE_TABLE + "." + TimelineTable._ID,
						TimelineTable.STATUS_ID,
						UserTable.USER_TABLE + "."
								+ UserTable.PROFILE_IMAGE_URL,
						UserTable.USER_TABLE + "." + UserTable.USER_NAME,
						TimelineTable.STATUS_TEXT,
						TimelineTable.STATUS_TEXT_EXP,
						TimelineTable.IS_RETWEETED_BY_ME,
						TimelineTable.IS_FAVORITED,
						UserTable.USER_TABLE + "." + UserTable.SCREEN_NAME,
						TimelineTable.FROM_ID,
						TimelineTable.IS_RETWEET,
						TimelineTable.RT_USER_NAME,
						TimelineTable.TIMELINE_TABLE + "."
								+ TimelineTable.CREATED_AT },
				TimelineTable.STATUS_ID + "=?",
				new String[] { String.valueOf(mStatusID) },
				TimelineTable.TIMELINE_TABLE + "." + TimelineTable.CREATED_AT
						+ " DESC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

		if (cursor == null || cursor.getCount() == 0) {
			finish();
			return;
		}
		cursor.moveToFirst();

		ImageView profile_image_view = (ImageView) findViewById(R.id.profile_image);
		TextView user_name_view = (TextView) findViewById(R.id.user_name);
		TextView status_text_view = (TextView) findViewById(R.id.status_text);
		TextView screen_name_view = (TextView) findViewById(R.id.screen_name);

		final String mScreenName = cursor.getString(cursor
				.getColumnIndexOrThrow(UserTable.SCREEN_NAME));
		screen_name_view.setText(mScreenName);

		int imagecol = cursor
				.getColumnIndexOrThrow(UserTable.PROFILE_IMAGE_URL);
		imageLoader
				.DisplayImage(cursor.getString(imagecol), profile_image_view);

		final long from_id = cursor.getLong(cursor
				.getColumnIndex(TimelineTable.FROM_ID));
		profile_image_view.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(StatusActivity.this, UserActivity.class);
				i.putExtra(UserActivity.KEY_USER_ID, from_id);
				startActivity(i);
			}
		});

		int namecol = cursor.getColumnIndexOrThrow(UserTable.USER_NAME);
		user_name_view.setText(cursor.getString(namecol));

//		int expdcol = cursor.getColumnIndex(TimelineTable.STATUS_TEXT_EXP);
		String text = null;
//		if (cursor.isNull(expdcol)) {
//			int textcol = cursor
//					.getColumnIndexOrThrow(TimelineTable.STATUS_TEXT);
//			text = cursor.getString(textcol);
//		} else {
//			text = cursor.getString(expdcol);
//		}

		int textcol = cursor.getColumnIndexOrThrow(TimelineTable.STATUS_TEXT);
		text = cursor.getString(textcol);

		// Get URLSpan first.
		Spannable sp = new SpannableString(text);
		Linkify.addLinks(sp, Linkify.ALL);
		SpannableStringBuilder strBuilder = new SpannableStringBuilder(sp);
		URLSpan[] underlines = strBuilder.getSpans(0, strBuilder.length(),
				URLSpan.class);

		// Replace with ClickableSpan.
		for (final URLSpan span : underlines) {
			int start = strBuilder.getSpanStart(span);
			int end = strBuilder.getSpanEnd(span);
			int flags = strBuilder.getSpanFlags(span);
			ClickableSpan loadInWebView = new ClickableSpan() {
				public void onClick(View view) {
					Util.profile(StatusActivity.this, "url_click.csv",
							span.getURL()+" click");
					Intent i = new Intent(StatusActivity.this,
							WebActivity.class);
					i.putExtra(WebActivity.KEY_URL, span.getURL());
					startActivity(i);
				}
			};
			strBuilder.removeSpan(span);
			strBuilder.setSpan(loadInWebView, start, end, flags);
		}
		status_text_view.setText(strBuilder);
		status_text_view.setMovementMethod(LinkMovementMethod.getInstance());

		mIsRetweetedByMe = cursor.getLong(cursor
				.getColumnIndex(TimelineTable.IS_RETWEETED_BY_ME));
		Util.log("mIsRetweeted " + mIsRetweetedByMe);

		mIsFavorited = cursor.getInt(cursor
				.getColumnIndex(TimelineTable.IS_FAVORITED));
		Util.log("mIsFavorited " + mIsFavorited);

		if (mIsRetweetedByMe != 0) {
			mRetweetButton.setImageResource(R.drawable.ic_action_rt_on);
		} else {
			mRetweetButton.setImageResource(R.drawable.ic_action_rt_off);
		}

		if (mIsFavorited != 0) {
			mFavoriteButton.setImageResource(R.drawable.ic_action_fave_on);
		} else {
			mFavoriteButton.setImageResource(R.drawable.ic_action_fave_off);
		}

		mReplyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent i = new Intent(StatusActivity.this, TweetActivity.class);
				i.putExtra(TweetActivity.KEY_PRE_TEXT, mScreenName);
				Util.log("mScreenName " + mScreenName);
				startActivity(i);
			}
		});

		mRetweetButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mIsRetweetedByMe == 0) {
					new RetweetTask(StatusActivity.this).execute(mStatusID);
				} else {
					Toast.makeText(StatusActivity.this, "Already retweeted.",
							Toast.LENGTH_SHORT).show();
				}

			}
		});

		mFavoriteButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Util.log("mIsFavorited " + mIsFavorited);
				if (mIsFavorited != 0) {
					new FavoriteTask(StatusActivity.this, false)
							.execute(mStatusID);
				} else {
					new FavoriteTask(StatusActivity.this, true)
							.execute(mStatusID);
				}
			}
		});

		int is_retweet = cursor.getInt(cursor
				.getColumnIndex(TimelineTable.IS_RETWEET));

		if (is_retweet == 1) {
			String retweet_by = cursor.getString(cursor
					.getColumnIndex(TimelineTable.RT_USER_NAME));
			mRetweetBy.setVisibility(View.VISIBLE);
			mRetweetBy.setText("Retweeted by " + retweet_by);
		} else {
			mRetweetBy.setVisibility(View.GONE);
		}

		long time = cursor.getLong(cursor
				.getColumnIndex(TimelineTable.CREATED_AT));
		mTimeView.setText(DateUtils.getRelativeTimeSpanString(time));
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		ImageView profile_image = (ImageView) findViewById(R.id.profile_image);
		TextView user_name = (TextView) findViewById(R.id.user_name);
		TextView status_text = (TextView) findViewById(R.id.status_text);

		profile_image.setImageDrawable(null);
		user_name.setText(null);
		status_text.setText(null);
	}
	
	
	@Override
	public void finish() {
		Util.profile(this, "status_click.csv", mStatusID + ", leave");
		super.finish();
	}
}
