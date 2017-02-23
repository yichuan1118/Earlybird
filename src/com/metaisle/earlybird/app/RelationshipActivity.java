package com.metaisle.earlybird.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.earlybird.fragment.RelationshipFragment;

public class RelationshipActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			Bundle b = new Bundle();
			b.putLong(RelationshipFragment.KEY_USER_ID, getIntent()
					.getLongExtra(RelationshipFragment.KEY_USER_ID, -1));
			getSupportFragmentManager()
					.beginTransaction()
					.add(android.R.id.content,
							Fragment.instantiate(this,
									RelationshipFragment.class.getName(), b))
					.commit();
		}

	}

}
