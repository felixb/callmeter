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
import android.support.v4.view.Window;
import android.view.View;

import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

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
	/** {@link Message} for {@link Handler}: start background: LogRunner. */
	public static final int MSG_BACKGROUND_START_RUNNER = 3;
	/** {@link Message} for {@link Handler}: stop background: LogRunner. */
	public static final int MSG_BACKGROUND_STOP_RUNNER = 4;
	/** {@link Message} for {@link Handler}: progress: LogMatcher. */
	public static final int MSG_BACKGROUND_PROGRESS_MATCHER = 5;

	/** Display ads? */
	private static boolean prefsNoAds;

	/** {@link ViewPager}. */
	private ViewPager pager;
	/** {@link PlansFragmentAdapter}. */
	private PlansFragmentAdapter fadapter;

	/** {@link Handler} for handling messages from background process. */
	private final Handler handler = new Handler() {
		/** LogRunner running in background? */
		private boolean inProgressRunner = false;
		/** {@link ProgressDialog} showing LogMatcher's status. */
		private ProgressDialog statusMatcher = null;
		/** Is statusMatcher a {@link ProgressDialog}? */
		private boolean statusMatcherProgress = false;

		/** String for recalculate message. */
		private String recalc = null;

		@Override
		public void handleMessage(final Message msg) {
			switch (msg.what) {
			case MSG_BACKGROUND_START_RUNNER:
				inProgressRunner = true;
			case MSG_BACKGROUND_START_MATCHER:
				statusMatcherProgress = false;
				Plans.this.setInProgress(1);
				break;
			case MSG_BACKGROUND_STOP_RUNNER:
				inProgressRunner = false;
			case MSG_BACKGROUND_STOP_MATCHER:
				Plans.this.setInProgress(-1);
				Plans.this.getSupportActionBar().setSubtitle(null);
				PlansFragment f = Plans.this.fadapter.getHomeFragment();
				if (f != null) {
					f.requery();
				}
				break;
			case MSG_BACKGROUND_PROGRESS_MATCHER:
				if (Plans.this.progressCount == 0) {
					Plans.this.setProgress(1);
				}
				if (statusMatcher == null || (!statusMatcherProgress || // .
						statusMatcher.isShowing())) {
					Log.d(TAG, "matcher progress: " + msg.arg1);
					if (statusMatcher == null || !statusMatcherProgress) {
						final ProgressDialog dold = statusMatcher;
						statusMatcher = new ProgressDialog(Plans.this);
						statusMatcher.setCancelable(true);
						if (recalc == null) {
							recalc = Plans.this
									.getString(R.string.reset_data_progr2);
						}
						statusMatcher.setMessage(recalc);
						statusMatcher.setProgressStyle(// .
								ProgressDialog.STYLE_HORIZONTAL);
						statusMatcher.setMax(msg.arg2);
						statusMatcher.setIndeterminate(false);
						statusMatcherProgress = true;
						Log.d(TAG, "showing dialog..");
						statusMatcher.show();
						if (dold != null) {
							dold.dismiss();
						}
					}
					statusMatcher.setProgress(msg.arg1);
				}
				if (recalc == null) {
					recalc = Plans.this.getString(R.string.reset_data_progr2);
				}
				Plans.this.getSupportActionBar().setSubtitle(
						recalc + " " + msg.arg1 + "/" + msg.arg2);
				break;
			default:
				break;
			}

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
				if (statusMatcher != null && statusMatcher.isShowing()) {
					try {
						statusMatcher.dismiss();
					} catch (IllegalArgumentException e) {
						Log.e(TAG, "error dismissing dialog", e);
					}
					statusMatcher = null;
				}
			}
		}
	};

	/** Number of background tasks. */
	private int progressCount = 0;

	/** {@link Handler} for outside. */
	private static Handler currentHandler = null;

	/**
	 * Show all {@link PlansFragment}s.
	 * 
	 * @author flx
	 */
	private static class PlansFragmentAdapter extends FragmentPagerAdapter
			implements TitleProvider {
		/** List of positions. */
		private final Long[] positions;
		/** List of titles. */
		private final String[] titles;
		/** {@link LogsFragment}. */
		private LogsFragment logs;
		/** Home {@link PlansFragment}. */
		private PlansFragment home;

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
				c = cr.query(
						DataProvider.Plans.CONTENT_URI,
						DataProvider.Plans.PROJECTION,
						DataProvider.Plans.TYPE + "=? and "
								+ DataProvider.Plans.BILLPERIOD + "!=?",
						new String[] {
								String.valueOf(DataProvider.TYPE_BILLPERIOD),
								String.valueOf(// .
								DataProvider.BILLPERIOD_INFINITE) }, null);
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
			Common.setDateFormat(context);
			final int l = this.positions.length;
			this.titles = new String[l];
			for (int i = 0; i < l - 2; i++) {
				this.titles[i] = Common.formatDate(context, this.positions[i]);
			}
			this.titles[l - 2] = context.getString(R.string.now);
			this.titles[l - 1] = context.getString(R.string.logs);
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
			if (position == this.getLogsFragmentPos()) {
				this.logs = new LogsFragment();
				return this.logs;
			} else {
				PlansFragment f = PlansFragment
						.newInstance(positions[position]);
				if (position == this.getHomeFragmentPos()) {
					this.home = f;
				}
				return f;
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
		 * Get Home {@link Fragment}.
		 * 
		 * @return {@link PlansFragment}
		 */
		public PlansFragment getHomeFragment() {
			return this.home;
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

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getTitle(final int position) {
			return this.titles[position];
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
		this.setContentView(R.layout.plans);

		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (p.getAll().isEmpty()) {
			// set date of recordings to beginning of last month
			Calendar c = Calendar.getInstance();
			c.set(Calendar.DAY_OF_MONTH, 0);
			c.add(Calendar.MONTH, -1);
			Log.i(TAG, "set date of recording: " + c);
			p.edit().putLong(Preferences.PREFS_DATE_BEGIN, c.getTimeInMillis())
					.commit();
		}
		p = null;

		ChangelogHelper.showChangelog(this, true);
		ChangelogHelper.showNotes(this, true, null, null, null);

		prefsNoAds = DonationHelper.hideAds(this);

		this.pager = (ViewPager) findViewById(R.id.pager);

		this.fadapter = new PlansFragmentAdapter(this,
				getSupportFragmentManager());
		this.pager.setAdapter(this.fadapter);

		TitlePageIndicator indicator = (TitlePageIndicator) // .
		findViewById(R.id.titles);
		indicator.setViewPager(this.pager);

		this.pager.setCurrentItem(this.fadapter.getHomeFragmentPos());
		indicator.setOnPageChangeListener(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Utils.setLocale(this);
		currentHandler = this.handler;
		this.setInProgress(0);
		PlansFragment.reloadPreferences(this);

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
			DonationHelper.startDonationActivity(this, true);
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
			return super.onOptionsItemSelected(item);
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

	/**
	 * Set progress indicator.
	 * 
	 * @param add
	 *            add number of running tasks
	 */
	public synchronized void setInProgress(final int add) {
		Log.d(TAG, "setInProgress(" + add + ")");
		this.progressCount += add;

		if (this.progressCount < 0) {
			Log.w(TAG, "this.progressCount: " + this.progressCount);
			this.progressCount = 0;
		}

		Log.d(TAG, "progressCount: " + this.progressCount);
		if (this.progressCount == 0) {
			this.setProgressBarIndeterminateVisibility(Boolean.FALSE);
		} else {
			this.setProgressBarIndeterminateVisibility(Boolean.TRUE);
		}
	}
}
