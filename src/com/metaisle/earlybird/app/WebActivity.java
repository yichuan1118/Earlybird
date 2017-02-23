package com.metaisle.earlybird.app;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.view.KeyEvent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.integralblue.httpresponsecache.HttpResponseCache;
import com.metaisle.earlybird.R;
import com.metaisle.util.Util;

@SuppressLint("SetJavaScriptEnabled")
public class WebActivity extends SherlockActivity {
	public static final String KEY_URL = "key_url";

	private WebView mWebView;
	private String mUrl;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		mWebView = new WebView(this);
		setContentView(mWebView);

		setSupportProgressBarVisibility(true);
		setSupportProgressBarIndeterminateVisibility(true);

		mUrl = getIntent().getStringExtra(KEY_URL);

		try {
			File httpCacheDir = null;
			if (getExternalCacheDir() == null) {
				httpCacheDir = new File(getCacheDir(), "http");
			} else {
				httpCacheDir = new File(getExternalCacheDir(), "http");
			}
			long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
			HttpResponseCache.install(httpCacheDir, httpCacheSize);
		} catch (IOException e) {
			Util.log("HTTP response cache installation failed:" + e);
		}

		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings()
				.setPluginState(WebSettings.PluginState.ON_DEMAND);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setDomStorageEnabled(true);
		mWebView.getSettings().setAllowFileAccess(true);
		mWebView.getSettings().setAppCacheEnabled(true);
		mWebView.getSettings()
				.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
		mWebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 10);
		mWebView.setWebViewClient(new WebViewClient() {
			private String original_url = null;
			private long start_rx = -1L;
			private long start_tx = -1L;

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

					Util.profile(WebActivity.this, "url_click.csv",
							original_url + ", " + url + ", " + rx + ", " + tx);
				}
				super.onPageFinished(view, url);
			}

			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				Map<String, String> cacheHeaders = new HashMap<String, String>(
						1);
				int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
				cacheHeaders.put("Cache-Control", "max-stale=" + maxStale);
				view.loadUrl(url, cacheHeaders);
				return true;
			}
		});
		mWebView.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int progress) {
				WebActivity.this.setSupportProgress(progress * 100);
			}

		});

		Map<String, String> cacheHeaders = new HashMap<String, String>(1);
		int maxStale = 60 * 60 * 24 * 28; // tolerate 4-weeks stale
		cacheHeaders.put("Cache-Control", "max-stale=" + maxStale);

		mWebView.loadUrl(mUrl, cacheHeaders);
	}

	@Override
	protected void onStop() {
		HttpResponseCache cache = HttpResponseCache.getInstalled();
		if (cache != null) {
			cache.flush();
		}
		super.onStop();
	}

	@Override
	public void finish() {
		Util.profile(this, "url_click.csv", mUrl + ", leave");
		super.finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.activity_web, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_open_in_browser:
			Intent browserIntent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(mUrl));
			startActivity(browserIntent);

			break;

		default:
			break;
		}

		return true;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Check if the key event was the Back button and if there's history
		if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
			mWebView.goBack();
			return true;
		}
		// If it wasn't the Back key or there's no web page history, bubble up
		// to the default
		// system behavior (probably exit the activity)
		return super.onKeyDown(keyCode, event);
	}
}
