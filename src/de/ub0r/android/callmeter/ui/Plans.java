/*
 * Copyright (C) 2009-2011 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; If not, see <http://www.gnu.org/licenses/>.
 */
package de.ub0r.android.callmeter.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.Window;
import de.ub0r.android.callmeter.Ads;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerReceiver;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.ChangelogHelper;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Callmeter's Main {@link FragmentActivity}.
 * 
 * @author flx
 */
public final class Plans extends FragmentActivity implements
		OnPageChangeListener {
	/** Tag for output. */
	private static final String TAG = "main";

	/** Ad's unit id. */
	private static final String AD_UNITID = "a14c185ce8841c6";

	/** Ad's keywords. */
	public static final HashSet<String> AD_KEYWORDS = new HashSet<String>();
	static {
		AD_KEYWORDS.add("android");
		AD_KEYWORDS.add("mobile");
		AD_KEYWORDS.add("handy");
		AD_KEYWORDS.add("cellphone");
		AD_KEYWORDS.add("google");
		AD_KEYWORDS.add("htc");
		AD_KEYWORDS.add("samsung");
		AD_KEYWORDS.add("motorola");
		AD_KEYWORDS.add("market");
		AD_KEYWORDS.add("app");
		AD_KEYWORDS.add("report");
		AD_KEYWORDS.add("calls");
		AD_KEYWORDS.add("game");
		AD_KEYWORDS.add("traffic");
		AD_KEYWORDS.add("data");
		AD_KEYWORDS.add("amazon");
	}

	/** {@link Message} for {@link Handler}: start background: LogMatcher. */
	public static final int MSG_BACKGROUND_START_MATCHER = 1;
	/** {@link Message} for {@link Handler}: stop background: LogMatcher. */
	public static final int MSG_BACKGROUND_STOP_MATCHER = 2;
	/** {@link Message} for {@link Handler}: start background: PlanAdapter. */
	public static final int MSG_BACKGROUND_START_PLANADAPTER = 3;
	/** {@link Message} for {@link Handler}: stop background: PlanAdapter. */
	public static final int MSG_BACKGROUND_STOP_PLANADAPTER = 4;
	/** {@link Message} for {@link Handler}: start background: LogRunner. */
	public static final int MSG_BACKGROUND_START_RUNNER = 5;
	/** {@link Message} for {@link Handler}: stop background: LogRunner. */
	public static final int MSG_BACKGROUND_STOP_RUNNER = 6;
	/** {@link Message} for {@link Handler}: progress: LogMatcher. */
	public static final int MSG_BACKGROUND_PROGRESS_MATCHER = 7;

	/** Display ads? */
	private static boolean prefsNoAds;

	/** {@link ViewPager}. */
	private ViewPager pager;
	/** {@link PlansFragmentAdapter}. */
	private PlansFragmentAdapter fadapter;

	/** {@link Handler} for handling messages from background process. */
	private final Handler handler = new Handler() {
		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MSG_BACKGROUND_START_MATCHER:
				inProgressMatcher = true;
				break;
			case MSG_BACKGROUND_START_RUNNER:
				inProgressRunner = true;
				break;
			case MSG_BACKGROUND_STOP_RUNNER:
			case MSG_BACKGROUND_STOP_MATCHER:
				inProgressRunner = false;
				inProgressMatcher = false;
				// FIXME adapter.startPlanQuery(now, hideZero, hideNoCost);
				break;
			case MSG_BACKGROUND_START_PLANADAPTER:
				inProgressPlanadapter = true;
				break;
			case MSG_BACKGROUND_STOP_PLANADAPTER:
				inProgressPlanadapter = false;
				break;
			case MSG_BACKGROUND_PROGRESS_MATCHER:
				if (statusMatcher != null && (!statusMatcherProgress || // .
						statusMatcher.isShowing())) {
					Log.d(TAG, "matcher progress: " + msg.arg1);
					if (!statusMatcherProgress) {
						final ProgressDialog dold = statusMatcher;
						statusMatcher = new ProgressDialog(Plans.this);
						statusMatcher.setCancelable(true);
						statusMatcher.setMessage(Plans.this
								.getString(R.string.reset_data_progr2));
						statusMatcher.setProgressStyle(// .
								ProgressDialog.STYLE_HORIZONTAL);
						statusMatcher.setMax(msg.arg2);
						statusMatcher.setIndeterminate(false);
						statusMatcherProgress = true;
						statusMatcher.show();
						dold.dismiss();
					}
					statusMatcher.setProgress(msg.arg1);
				}
				break;
			default:
				break;
			}

			Plans.this.setProgressBarIndeterminateVisibility(inProgressMatcher
					|| inProgressPlanadapter);
			if (inProgressRunner) {
				if (statusMatcher == null
						|| (msg.arg1 <= 0 && !statusMatcher.isShowing())) {
					statusMatcher = new ProgressDialog(Plans.this);
					statusMatcher.setCancelable(true);
					statusMatcher.setMessage(Plans.this
							.getString(R.string.reset_data_progr1));
					statusMatcher.setIndeterminate(true);
					statusMatcherProgress = false;
					statusMatcher.show();
				}
			} else {
				if (statusMatcher != null) {
					statusMatcher.dismiss();
					statusMatcher = null;
				}
			}
		}
	};

	/** {@link Handler} for outside. */
	private static Handler currentHandler = null;
	/** LogMatcher running in background? */
	private static boolean inProgressMatcher = false;
	/** LogRunner running in background? */
	private static boolean inProgressRunner = false;
	/** PlanAdapter running in background? */
	private static boolean inProgressPlanadapter = false;
	/** {@link ProgressDialog} showing LogMatcher's status. */
	private static ProgressDialog statusMatcher = null;
	/** Is statusMatcher a {@link ProgressDialog}? */
	private static boolean statusMatcherProgress = false;

	/**
	 * Show all {@link PlansFragment}s.
	 * 
	 * @author flx
	 */
	private static class PlansFragmentAdapter extends FragmentPagerAdapter {
		/** List of positions. */
		private final Long[] positions;
		/** {@link LogsFragment}. */
		private LogsFragment logs;

		/**
		 * Default constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 * @param fm
		 *            {@link FragmentManager}
		 */
		public PlansFragmentAdapter(final Context context,
				final FragmentManager fm) {
			super(fm);
			ContentResolver cr = context.getContentResolver();
			Cursor c = cr.query(DataProvider.Logs.CONTENT_URI,
					new String[] { DataProvider.Logs.DATE }, null, null,
					DataProvider.Logs.DATE + " ASC");
			if (!c.moveToFirst()) {
				positions = new Long[] { -1L, -1L };
				c.close();
			} else {
				final long minDate = c.getLong(0);
				c.close();
				c = cr.query(DataProvider.Plans.CONTENT_URI,
						DataProvider.Plans.PROJECTION, DataProvider.Plans.TYPE
								+ "=?", new String[] { String
								.valueOf(DataProvider.TYPE_BILLPERIOD) }, null);
				if (minDate < 0L || !c.moveToFirst()) {
					positions = new Long[] { -1L, -1L };
					c.close();
				} else {
					ArrayList<Long> list = new ArrayList<Long>();
					do { // walk all bill periods
						Calendar lastBP = Calendar.getInstance();
						lastBP.setTimeInMillis(c
								.getLong(DataProvider.Plans.INDEX_BILLDAY));
						Calendar now = Calendar.getInstance();
						long billday = -1L;
						do { // get all bill days
							Calendar billDay = DataProvider.Plans.getBillDay(
									c.getInt(// .
									DataProvider.Plans.INDEX_BILLPERIOD),
									lastBP, now, false);
							billday = billDay.getTimeInMillis();
							lastBP.setTimeInMillis(billday);
							billday -= 1L;
							now.setTimeInMillis(billday);
							list.add(billday);
						} while (billday > minDate);
					} while (c.moveToNext());
					c.close();
					list.add(-1L); // current time
					list.add(-1L); // logs
					positions = list.toArray(new Long[] {});
					list = null;
					Arrays.sort(positions, 0, positions.length - 2);
				}
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getCount() {
			return positions.length;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Fragment getItem(final int position) {
			if (position == getLogsFragmentPos()) {
				this.logs = new LogsFragment();
				return this.logs;
			} else {
				return PlansFragment.newInstance(positions[position]);
			}
		}

		/**
		 * Get position of home {@link Fragment}.
		 * 
		 * @return position of home {@link Fragment}
		 */
		public int getHomeFragmentPos() {
			return positions.length - 2;
		}

		/**
		 * Get position of Logs {@link Fragment}.
		 * 
		 * @return position of Logs {@link Fragment}
		 */
		public int getLogsFragmentPos() {
			return positions.length - 1;
		}

		/**
		 * Get Logs {@link Fragment}.
		 * 
		 * @return {@link LogsFragment}
		 */
		public LogsFragment getLogsFragment() {
			return this.logs;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		currentHandler = this.handler;
		// get prefs.
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean showTitlebar = p.getBoolean(
				Preferences.PREFS_SHOWTITLEBAR, true);
		if (!showTitlebar) {
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		this.setContentView(R.layout.plans);

		ChangelogHelper.showChangelog(this, true);
		ChangelogHelper.showNotes(this, true, null, null, null);

		prefsNoAds = DonationHelper.hideAds(this);

		this.pager = (ViewPager) findViewById(R.id.pager);
		this.fadapter = new PlansFragmentAdapter(this,
				getSupportFragmentManager());
		this.pager.setAdapter(this.fadapter);
		this.pager.setCurrentItem(this.fadapter.getHomeFragmentPos());
		this.pager.setOnPageChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Utils.setLocale(this);
		currentHandler = this.handler;
		Plans.this.setProgressBarIndeterminateVisibility(inProgressMatcher);
		PlansFragment.reloadPreferences(this);
		Common.setDateFormat(this);

		// reload plan configuration
		// FIXME adapter.startPlanQuery(now, hideZero, hideNoCost);
		// start LogRunner
		LogRunnerService.update(this, LogRunnerReceiver.ACTION_FORCE_UPDATE);
		// schedule next update
		LogRunnerReceiver.schedNext(this);
		if (!prefsNoAds) {
			Ads.loadAd(this, R.id.ad, AD_UNITID, AD_KEYWORDS);
		} else {
			this.findViewById(R.id.ad).setVisibility(View.GONE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onNewIntent(final Intent intent) {
		this.setIntent(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		currentHandler = null;
	}

	/**
	 * Get the current {@link Handler}.
	 * 
	 * @return {@link Handler}.
	 */
	public static Handler getHandler() {
		return currentHandler;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		this.getMenuInflater().inflate(R.menu.menu_main, menu);
		if (prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_settings:
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			DonationHelper.startDonationActivity(this, false);
			return true;
		case R.id.item_logs:
			this.showLogsFragment(-1L);
			return true;
		case android.R.id.home:
			this.pager.setCurrentItem(this.fadapter.getHomeFragmentPos(), true);
			LogsFragment lf = this.fadapter.getLogsFragment();
			if (lf != null) {
				lf.setPlanId(-1L);
			}
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void showLogsFragment(final long planId) {
		LogsFragment lf = this.fadapter.getLogsFragment();
		if (lf != null) {
			lf.setPlanId(planId);
		}
		this.pager.setCurrentItem(this.fadapter.getLogsFragmentPos(), true);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageScrolled(final int position, final float positionOffset,
			final int positionOffsetPixels) {
		// nothing to do

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageSelected(final int position) {
		if (position == this.fadapter.getLogsFragmentPos()) {
			this.findViewById(R.id.ad).setVisibility(View.GONE);
		} else if (!prefsNoAds) {
			this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onPageScrollStateChanged(final int state) {
		// nothing to do
	}

}
