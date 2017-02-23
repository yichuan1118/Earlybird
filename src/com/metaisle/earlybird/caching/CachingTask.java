package com.metaisle.earlybird.caching;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Process;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.metaisle.earlybird.data.Provider;
import com.metaisle.earlybird.data.TimelineTable;
import com.metaisle.util.FindUrls;
import com.metaisle.util.Util;

@SuppressLint("SetJavaScriptEnabled")
public class CachingTask extends AsyncTask<Void, Void, Void> {

	private Context mContext;
	private boolean mLaunchedNext = false;
	private List<String> mOriginalUrls;
	private List<String> mExpdUrls = new ArrayList<String>();
	private long mStatusID;
	private String mStatusText;

	private WebView mWebView;
	private boolean mCacheOnMobile;

	public CachingTask(Context context) {
		mContext = context;
		mCacheOnMobile = PreferenceManager
				.getDefaultSharedPreferences(mContext).getBoolean(
						"caching_on_mobile", false);
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (mCacheOnMobile && !Util.isOnline(mContext)) {
			return null;
		}

		if (!mCacheOnMobile && !Util.isOnWifi(mContext)) {
			return null;
		}

		Cursor cursor = mContext.getContentResolver().query(
				Provider.TIMELINE_CONTENT_URI,
				new String[] { TimelineTable.STATUS_TEXT,
						TimelineTable.STATUS_ID },
				TimelineTable.CACHED_AT + " is null AND "
						+ TimelineTable.IS_HOME + "=1",
				null,
				TimelineTable.TIMELINE_TABLE + "." + TimelineTable.CREATED_AT
						+ " DESC LIMIT 1");

		if (!cursor.moveToFirst()) {
			return null;
		}
		mStatusText = cursor.getString(0);
		mStatusID = cursor.getLong(1);
		Util.log("-----------------------------------------");
		Util.log("Cache tweet " + mStatusText);
		mOriginalUrls = FindUrls.extractUrls(mStatusText);

		if (mOriginalUrls.size() == 0) {
			Util.log("No URL");
			// -----------------------------
			ContentValues values = new ContentValues();
			Time now = new Time();
			now.setToNow();
			values.put(TimelineTable.CACHED_AT, now.toMillis(true));
			values.put(TimelineTable.STATUS_TEXT_EXP, mStatusText);
			mContext.getContentResolver().update(
					Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI, ""
							+ mStatusID), values, null, null);
			// -----------------------------
			new CachingTask(mContext).execute();
			return null;
		}

		Util.log("mOriginalUrls " + mOriginalUrls);
		for (String orig_url : mOriginalUrls) {
			try {
				HttpURLConnection connection = getConnection(orig_url);
				int code = connection.getResponseCode();
				String url = connection.getURL().toString();
				Util.log("original " + orig_url + " redirected url " + url
						+ " code " + code);
				mExpdUrls.add(url);

			} catch (IOException e) {
				e.printStackTrace();

				Util.log("Redirect failed.");
				mOriginalUrls = null;
				// -----------------------------
				ContentValues values = new ContentValues();
				Time now = new Time();
				now.setToNow();
				values.put(TimelineTable.CACHED_AT, now.toMillis(true));
				values.put(TimelineTable.STATUS_TEXT_EXP, mStatusText);
				mContext.getContentResolver().update(
						Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI, ""
								+ mStatusID), values, null, null);
				// -----------------------------
				new CachingTask(mContext).execute();
				return null;
			}
		}

		Util.log("mExpdUrls " + mExpdUrls);
		return null;
	}

	@Override
	protected void onPostExecute(Void result) {
		if (mCacheOnMobile && !Util.isOnline(mContext)) {
			return;
		}

		if (!mCacheOnMobile && !Util.isOnWifi(mContext)) {
			return;
		}

		if (mOriginalUrls == null || mOriginalUrls.size() == 0) {
			return;
		}

		// ========================================================================
		mWebView = new WebView(mContext);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setDomStorageEnabled(true);

		mWebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.getSettings().setAppCacheEnabled(true);

		mWebView.setWebViewClient(new CachingWebViewClient());

		// ========================================================================

		for (String url : mOriginalUrls) {
			mWebView.loadUrl(url);
		}
	}

	public HttpURLConnection getConnection(String strUrl) throws IOException {
		URL url = new URL(strUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setInstanceFollowRedirects(true);

		return connection;
	}

	private class CachingWebViewClient extends WebViewClient {
		private String original_url = null;
		private long start_rx = -1L;
		private long start_tx = -1L;

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			Util.log(" shouldOverrideUrlLoading " + url);
			view.loadUrl(url);
			return false;
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			Util.log("onPageStarted " + url);

			if (original_url == null) {
				original_url = url;
			}
			if (start_rx < 0) {
				start_rx = TrafficStats.getUidRxBytes(Process.myUid());
				start_tx = TrafficStats.getUidTxBytes(Process.myUid());
				Util.log("start_rx " + start_rx);
				Util.log("start_tx " + start_tx);
			}

			super.onPageStarted(view, url, favicon);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			Util.log("onPageFinished " + url);

			if (!url.startsWith("http://t.co")) {
				long rx = TrafficStats.getUidRxBytes(Process.myUid())
						- start_rx;
				long tx = TrafficStats.getUidTxBytes(Process.myUid())
						- start_tx;

				Util.log("rx " + rx);
				Util.log("tx " + tx);

				Util.profile(mContext, "url_cache.csv", original_url + ", "
						+ url + ", " + rx + ", " + tx);

				// -------------------------
				if (mLaunchedNext == false) {

					for (int i = 0; i < mOriginalUrls.size(); i++) {
						mStatusText = mStatusText.replace(mOriginalUrls.get(i),
								mExpdUrls.get(i));
					}
					Util.log("Status after replace: " + mStatusText);

					// -----------------------------
					ContentValues values = new ContentValues();
					Time now = new Time();
					now.setToNow();
					values.put(TimelineTable.CACHED_AT, now.toMillis(true));
					values.put(TimelineTable.STATUS_TEXT_EXP, mStatusText);
					mContext.getContentResolver().update(
							Uri.withAppendedPath(Provider.TIMELINE_CONTENT_URI,
									"" + mStatusID), values, null, null);
					// -----------------------------

					new CachingTask(mContext).execute();
					mLaunchedNext = true;
				}
			}
			super.onPageFinished(view, url);
		}
	}

}
