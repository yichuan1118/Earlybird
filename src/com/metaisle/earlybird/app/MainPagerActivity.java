package com.metaisle.earlybird.app;

import java.io.IOException;

import twitter4j.Paging;
import twitter4j.StatusUpdate;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.integralblue.httpresponsecache.HttpResponseCache;
import com.metaisle.earlybird.R;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.fragment.IRefreshable;
import com.metaisle.earlybird.twitter.TimelineTask;
import com.metaisle.earlybird.twitter.TweetTask;
import com.metaisle.profiler.CollectorService;
import com.metaisle.util.Util;

public class MainPagerActivity extends SherlockFragmentActivity {
	private SharedPreferences mPrefs;
	private ActionBar mBar;
	private ViewPager mPager;
	private Button mLoginButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mPrefs = getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);
		mPrefs.edit().putBoolean(Prefs.KEY_DEBUG, false).commit();
		Util.DEBUG = mPrefs.getBoolean(Prefs.KEY_DEBUG, false);

		SharedPreferences dprefs = PreferenceManager
				.getDefaultSharedPreferences(this);

		dprefs.edit()
				.putString("KEY_FTP_SERVER", "earlybird_profile.metaisle.com")
				.commit();

		setContentView(R.layout.activity_main_pager);
		startService(new Intent(this, CollectorService.class));

		// if (HttpResponseCache.getInstalled() == null) {
		// try {
		// File httpCacheDir = null;
		//
		// if (getExternalCacheDir() == null) {
		// httpCacheDir = new File(getCacheDir(), "http");
		// } else {
		// httpCacheDir = new File(getExternalCacheDir(), "http");
		// }
		// if (HttpResponseCache.getInstalled() == null) {
		// long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
		// HttpResponseCache.install(httpCacheDir, httpCacheSize);
		// }
		// } catch (IOException e) {
		// Util.log("HTTP response cache installation failed:" + e);
		// }
		// }

		mPager = (ViewPager) findViewById(R.id.main_pager);
		mPager.setAdapter(new MainPagerAdapter(this, mPager));

		mBar = getSupportActionBar();
		mBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mPrefs = getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE);

		mLoginButton = (Button) findViewById(R.id.login_button);
		mLoginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MainPagerActivity.this,
						OAuthActivity.class);
				startActivity(intent);
			}
		});

		promote();
	}

	@Override
	protected void onResume() {
		super.onResume();

		String oauth_token = mPrefs.getString(Prefs.KEY_OAUTH_TOKEN, null);
		if (oauth_token == null) {
			mLoginButton.setVisibility(View.VISIBLE);
			mPager.setVisibility(View.GONE);
		} else {
			mLoginButton.setVisibility(View.GONE);
			mPager.setVisibility(View.VISIBLE);

			long last_update_at = mPrefs.getLong(Prefs.KEY_LAST_UPDATED_AT, -1);
			Time now = new Time();
			now.setToNow();
			float since_last_update = (float) (now.toMillis(true) - last_update_at) / 1000 / 60;
			Util.log("last_update_at " + last_update_at + " now "
					+ now.toMillis(true) + " diff " + since_last_update);
			if (since_last_update > 60) {
				new TimelineTask(this, new Paging[] { new Paging(),
						new Paging(), new Paging(), new Paging() }).execute();
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// getContentResolver().delete(Provider.HOME_CONTENT_URI, null, null);
		// new TimelineTask(this, new Paging[] { new Paging(), null, null, null
		// })
		// .execute();

		if (item.getItemId() == R.id.menu_tweet) {
			startActivity(new Intent(MainPagerActivity.this,
					TweetActivity.class));
		} else if (item.getItemId() == R.id.menu_refresh_full) {
			getContentResolver().delete(Provider.TIMELINE_CONTENT_URI, null,
					null);
			getContentResolver().delete(Provider.USER_CONTENT_URI, null, null);
			getContentResolver().delete(Provider.MESSAGE_CONTENT_URI, null,
					null);

			try {
				if (HttpResponseCache.getInstalled() != null)
					HttpResponseCache.getInstalled().delete();
			} catch (IOException e) {
				e.printStackTrace();
			}

			new TimelineTask(MainPagerActivity.this, new Paging[] {
					new Paging(), new Paging(), new Paging(), new Paging() })
					.execute();

		} else if (item.getItemId() == R.id.menu_settings) {
			startActivity(new Intent(this, PrefsActivity.class));
		} else if (item.getItemId() == R.id.menu_refresh) {
			Toast.makeText(MainPagerActivity.this, "Refreshing",
					Toast.LENGTH_SHORT).show();
			IRefreshable refreshable = (IRefreshable) mPager.getAdapter()
					.instantiateItem(mPager, mPager.getCurrentItem());

			refreshable.refresh();
		}
		return true;
	}

	// @Override
	// protected void onStop() {
	// HttpResponseCache cache = HttpResponseCache.getInstalled();
	// if (cache != null) {
	// cache.flush();
	// }
	// super.onStop();
	// }

	private void promote() {
		SharedPreferences prefs = getSharedPreferences(Prefs.PREFS_NAME,
				MODE_PRIVATE);
		int times = prefs.getInt(Prefs.KEY_LAUNCHED_TIMES, 0);
		Util.log("times " + times);
		if (Util.isOnline(this)) {
			if (times == 3) {
				FragmentTransaction ft = getSupportFragmentManager()
						.beginTransaction();
				Fragment prev = getSupportFragmentManager().findFragmentByTag(
						"promote");
				if (prev != null) {
					ft.remove(prev);
				}
				ft.addToBackStack(null);

				// Create and show the dialog.
				PromoteDialog newFragment = new PromoteDialog();
				newFragment.show(ft, "promote");
			}
			prefs.edit().putInt(Prefs.KEY_LAUNCHED_TIMES, times + 1).commit();
		}
	}

	public static class PromoteDialog extends DialogFragment {
		public static final String promote_url = "http://earlybird.metaisle.com";
		Button yes;
		Button no;
		EditText tweet;

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View v = inflater
					.inflate(R.layout.dialog_promote, container, false);

			tweet = (EditText) v.findViewById(R.id.promote_tweet);
			yes = (Button) v.findViewById(R.id.promote_yes);
			no = (Button) v.findViewById(R.id.promote_no);

			yes.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					String t = tweet.getText().toString();
					StatusUpdate su = new StatusUpdate(t + " " + promote_url);
					new TweetTask(getActivity()).execute(su);
					Util.profile(getActivity(), "promote.csv", "yes");
					dismiss();
				}
			});

			no.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Util.profile(getActivity(), "promote.csv", "no");
					dismiss();
				}
			});
			return v;
		}
	}

}
