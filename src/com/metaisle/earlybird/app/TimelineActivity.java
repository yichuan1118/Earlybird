package com.metaisle.earlybird.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.earlybird.fragment.TimelineFragment;

public class TimelineActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle b = new Bundle();
			b.putInt(TimelineFragment.KEY_FRAGEMENT_TYPE,
					TimelineFragment.TYPE_OTHER_USER);
			b.putLong(TimelineFragment.KEY_OTHER_USER_ID, getIntent()
					.getLongExtra(TimelineFragment.KEY_OTHER_USER_ID, 0));
			getSupportFragmentManager()
					.beginTransaction()
					.add(android.R.id.content,
							Fragment.instantiate(this,
									TimelineFragment.class.getName(), b))
					.commit();
		}

	}
}
