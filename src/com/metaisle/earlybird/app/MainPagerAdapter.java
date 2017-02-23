package com.metaisle.earlybird.app;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.metaisle.earlybird.R;
import com.metaisle.earlybird.fragment.MessageFragment;
import com.metaisle.earlybird.fragment.RelationshipFragment;
import com.metaisle.earlybird.fragment.TimelineFragment;

public class MainPagerAdapter extends FragmentPagerAdapter implements
		TabListener, ViewPager.OnPageChangeListener {

	private final static String[] mFragmentTitles = { "Home", "Mentions",
			"Messages", "Favorite", "People" };

	private SherlockFragmentActivity mFragmentActivity;
	private ViewPager mPager;
	private ActionBar mActionBar;

	public MainPagerAdapter(SherlockFragmentActivity activity, ViewPager pager) {
		super(activity.getSupportFragmentManager());
		mFragmentActivity = activity;
		mPager = pager;
		mPager.setOnPageChangeListener(this);
		mActionBar = activity.getSupportActionBar();

		mActionBar.addTab(mActionBar.newTab().setIcon(R.drawable.ic_tab_home)
				.setTabListener(this));
		mActionBar.addTab(mActionBar.newTab()
				.setIcon(R.drawable.ic_tab_mention).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab()
				.setIcon(R.drawable.ic_tab_favorite).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab()
				.setIcon(R.drawable.ic_tab_message).setTabListener(this));
		mActionBar.addTab(mActionBar.newTab().setIcon(R.drawable.ic_tab_people)
				.setTabListener(this));
	}

	@Override
	public int getCount() {
		return mFragmentTitles.length;
	}

	@Override
	public Fragment getItem(int position) {
		Bundle b = new Bundle();
		switch (position) {
		case 0: {
			b.putInt(TimelineFragment.KEY_FRAGEMENT_TYPE,
					TimelineFragment.TYPE_HOME);
			break;
		}
		case 1: {
			b.putInt(TimelineFragment.KEY_FRAGEMENT_TYPE,
					TimelineFragment.TYPE_MENTION);
			break;
		}
		case 2: {
			b.putInt(TimelineFragment.KEY_FRAGEMENT_TYPE,
					TimelineFragment.TYPE_FAVORITE);
			break;
		}
		case 3: {
			return Fragment.instantiate(mFragmentActivity,
					MessageFragment.class.getName());
		}
		case 4: {
			return Fragment.instantiate(mFragmentActivity,
					RelationshipFragment.class.getName());
		}

		}

		return Fragment.instantiate(mFragmentActivity,
				TimelineFragment.class.getName(), b);
	}

	@Override
	public CharSequence getPageTitle(int position) {
		return mFragmentTitles[position];
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int arg0, float arg1, int arg2) {
	}

	@Override
	public void onPageSelected(int position) {
		mActionBar.setSelectedNavigationItem(position);
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		int position = tab.getPosition();
		mPager.setCurrentItem(position);
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

}
