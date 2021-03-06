package com.metaisle.earlybird.app;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.metaisle.earlybird.R;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.twitter.MessageTask;
import com.metaisle.util.Util;

public class MessageActivity extends FragmentActivity {
	private EditText mTweetEdit;
	private Button mTweetButton;
	private TextView mOriTV;
	private DiscardDialogFragment mDiscardDialogFragment;
	NotificationManager mNotificationManager;

	public static final String KEY_ORI_TEXT = "key_pre_text";
	public static final String KEY_IN_REPLY_TO = "key_in_reply_to";

	private String mOriText;
	private long mInReplyTo;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tweet);

		if (getIntent() != null && getIntent().getExtras() != null) {
			mOriText = getIntent().getExtras().getString(KEY_ORI_TEXT);
			mInReplyTo = getIntent().getExtras().getLong(KEY_IN_REPLY_TO);
		}

		mTweetEdit = (EditText) findViewById(R.id.tweet_edit);
		mOriTV = (TextView) findViewById(R.id.orig_text);

		if (mOriText != null) {
			mOriTV.setVisibility(View.VISIBLE);
			mOriTV.setText(mOriText);
		} else {
			mOriTV.setVisibility(View.GONE);
		}

		Button cancelButton = (Button) findViewById(R.id.cancel_button);
		mTweetButton = (Button) findViewById(R.id.tweet_button);

		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (mTweetEdit.getText().toString().equals("")) {
					finish();
				} else {
					promptExit();
				}
			}
		});

		mTweetButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendTweet();
			}
		});

		mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	}

	@Override
	public void onBackPressed() {
		if (mTweetEdit.getText().toString().equals("")) {
			super.onBackPressed();
		} else {
			promptExit();
		}
	}

	@SuppressWarnings("deprecation")
	private void sendTweet() {
		String status_text = mTweetEdit.getText().toString();

		if (status_text.equals("")) {
			return;
		}
		Util.log("tweeting " + status_text);
		mTweetEdit.setEnabled(false);
		mTweetButton.setEnabled(false);

		new MessageTask(MessageActivity.this, status_text, mInReplyTo)
				.execute();

		CharSequence tickerText = "Sending tweet.";
		long when = System.currentTimeMillis();
		Notification notification = new Notification(R.drawable.ic_launcher,
				tickerText, when);
		Intent notificationIntent = new Intent();
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				notificationIntent, 0);
		notification.setLatestEventInfo(this, "", "", contentIntent);
		mNotificationManager.notify(Prefs.NOTIFICATION_TWEET_ID, notification);

		finish();
	}

	private void promptExit() {
		mDiscardDialogFragment = DiscardDialogFragment
				.newInstance("Tweet not sent, discard?");
		mDiscardDialogFragment.show(getSupportFragmentManager(), "dialog");
	}

	public static class DiscardDialogFragment extends DialogFragment {

		public static DiscardDialogFragment newInstance(String title) {
			DiscardDialogFragment frag = new DiscardDialogFragment();
			Bundle args = new Bundle();
			args.putString("title", title);
			frag.setArguments(args);
			return frag;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			String title = getArguments().getString("title");

			return new AlertDialog.Builder(getActivity())
					.setIcon(android.R.drawable.ic_dialog_alert)
					.setTitle(title)
					.setPositiveButton("Discard",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									getActivity().finish();
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									dismiss();
								}
							}).create();
		}
	}
}
