package com.metaisle.earlybird.app;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import android.app.Activity;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.metaisle.earlybird.R;
import com.metaisle.earlybird.data.Prefs;
import com.metaisle.util.Util;

public class OAuthActivity extends Activity {
	public static final String OAUTH_CONSUMER_KEY = "KFKmbNw21eZ8yRHVESomg";
	public static final String OAUTH_CONSUMER_SECRET = "hFHZkwuTp07PK3i8viNPffMnHurVKwJetgCtMQqsmg";

	private Twitter mTwitter;
	private RequestToken mRequestToken;

	private WebView mWebView;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_PROGRESS);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.activity_oauth);

		setProgressBarVisibility(true);

		mWebView = (WebView) findViewById(R.id.webview);
		mWebView.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url) {
				// Util.log(url);
				if (url.contains(getResources().getString(
						R.string.twitter_callback))) {
					Uri uri = Uri.parse(url);
					String oauthVerifier = uri
							.getQueryParameter("oauth_verifier");
					new SaveAccessTokenTask(oauthVerifier).execute();
					return true;
				}
				return false;
			}
		});
		final Activity activity = this;
		mWebView.setWebChromeClient(new WebChromeClient() {

			@Override
			public void onProgressChanged(WebView view, int progress) {
				activity.setProgress(progress * 100);
			}

		});

		new OAuthRequestTask().execute();
	}

	/*
	 * OAuth
	 */
	private class OAuthRequestTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected void onPreExecute() {
			mTwitter = new TwitterFactory().getInstance();
			mRequestToken = null;
			mTwitter.setOAuthConsumer(OAUTH_CONSUMER_KEY, OAUTH_CONSUMER_SECRET);
		}

		@Override
		public Void doInBackground(Void... params) {
			String callbackURL = getResources().getString(
					R.string.twitter_callback);
			try {
				mRequestToken = mTwitter.getOAuthRequestToken(callbackURL);
			} catch (TwitterException e) {
				e.printStackTrace();
				runOnUiThread(mLoginErrorToastRunnable);
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			if (mRequestToken == null) {
				runOnUiThread(mLoginErrorToastRunnable);
			} else {
				String authUrl = mRequestToken.getAuthorizationURL();
				// Util.log("auth url " + authUrl);
				mWebView.loadUrl(authUrl);
			}
		}
	};

	private class SaveAccessTokenTask extends AsyncTask<Void, Void, Void> {
		private String mOauthVerifier;
		private String token;
		private String secret;
		private long id;

		public SaveAccessTokenTask(String oauthVerifier) {
			mOauthVerifier = oauthVerifier;
		}

		@Override
		protected Void doInBackground(Void... params) {
			AccessToken at = null;
			try {
				at = mTwitter
						.getOAuthAccessToken(mRequestToken, mOauthVerifier);
				id = mTwitter.getId();
				Util.log("id " + id);
			} catch (TwitterException e) {
				runOnUiThread(mLoginErrorToastRunnable);
				e.printStackTrace();
			}
			token = at.getToken();
			secret = at.getTokenSecret();

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			getSharedPreferences(Prefs.PREFS_NAME, MODE_PRIVATE).edit()
					.putString(Prefs.KEY_OAUTH_TOKEN, token)
					.putString(Prefs.KEY_OAUTH_TOKEN_SECRET, secret)
					.putLong(Prefs.KEY_ID, id).commit();
			finish();
		}
	}

	/*
	 * Misc.
	 */
	final Runnable mLoginErrorToastRunnable = new Runnable() {
		public void run() {
			Toast.makeText(OAuthActivity.this,
					"Twitter Login error, try again later", Toast.LENGTH_SHORT)
					.show();
		}
	};
}
