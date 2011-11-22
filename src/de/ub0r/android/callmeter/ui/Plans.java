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

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import de.ub0r.android.callmeter.Ads;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerReceiver;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.PlanEdit;
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
	public static final String TAG = "main";

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

	/** Extra for setting now. */
	public static final String EXTRA_NOW = "now";
	/** Separator for the data. */
	public static String delimiter = " | ";

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

	/** Show hours and days. */
	private static boolean pShowHours = true;

	/** Selected currency format. */
	private static String currencyFormat = "$%.2f";

	/** Show today stats. */
	private static boolean showToday = false;
	/** Show total stats. */
	private static boolean showTotal = true;
	/** Hide zero plans. */
	private static boolean hideZero = false;
	/** Hide no cost plans. */
	private static boolean hideNoCost = false;

	/** Display ads? */
	private static boolean prefsNoAds;

	/** {@link ViewPager}. */
	private ViewPager pager;
	/** {@link PlanFragmentAdapter}. */
	private PlanFragmentAdapter fadapter;

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
	 * Show all {@link PlanFragment}s.
	 * 
	 * @author flx
	 */
	private static class PlanFragmentAdapter extends FragmentPagerAdapter {
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
		public PlanFragmentAdapter(final Context context,
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
				return PlanFragment.newInstance(positions[position]);
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
	 * Show plans.
	 * 
	 * @author flx
	 */
	public static final class PlanFragment extends ListFragment implements
			OnClickListener, OnItemLongClickListener {

		/** This fragments time stamp. */
		private long now;

		/** Handle for view. */
		private View vLoading, vImport;

		/**
		 * Get new {@link PlanFragment}.
		 * 
		 * @param now
		 *            This fragments current time
		 * @return {@link PlanFragment}
		 */
		public static Fragment newInstance(final long now) {
			PlanFragment f = new PlanFragment();
			Bundle args = new Bundle();
			args.putLong("now", now);
			f.setArguments(args);
			return f;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.setHasOptionsMenu(true);
			if (this.getArguments() == null) {
				now = -1L;
			} else {
				now = getArguments().getLong("now", -1L);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public View onCreateView(final LayoutInflater inflater,
				final ViewGroup container, final Bundle savedInstanceState) {
			View v = inflater
					.inflate(R.layout.plans_fragment, container, false);
			this.vLoading = v.findViewById(R.id.loading);
			this.vImport = v.findViewById(R.id.import_default);
			this.vImport.setOnClickListener(this);
			return v;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onActivityCreated(final Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			Calendar c = Calendar.getInstance();
			if (this.now >= 0L) {
				c.setTimeInMillis(now);
			}
			PlanAdapter adapter = new PlanAdapter(this.getActivity(), c, false,
					false, this.vLoading, this.vImport);
			this.setListAdapter(adapter);
			this.getListView().setOnItemLongClickListener(this);
			adapter.startPlanQuery(c);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreateOptionsMenu(final Menu menu,
				final MenuInflater inflater) {
			inflater.inflate(R.menu.menu_plans, menu);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onClick(final View v) {
			switch (v.getId()) {
			case R.id.import_default:
				final Intent intent = new Intent(Intent.ACTION_VIEW,
						Uri.parse(this.getString(R.string.url_rulesets)));
				try {
					this.startActivity(intent);
				} catch (ActivityNotFoundException e) {
					Log.e(TAG, "no activity to load url", e);
					Toast.makeText(
							this.getActivity(),
							"no activity to load url: "
									+ intent.getDataString(), Toast.LENGTH_LONG)
							.show();
				}
				break;
			default:
				break;
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean onItemLongClick(final AdapterView<?> parent,
				final View view, final int position, final long id) {
			final Builder builder = new Builder(this.getActivity());
			builder.setItems(R.array.dialog_edit_plan,
					new android.content.DialogInterface.OnClickListener() {
						@Override
						public void onClick(final DialogInterface dialog,
								final int which) {
							Intent intent = null;
							switch (which) {
							case 0:
								intent = new Intent(PlanFragment.this
										.getActivity(), PlanEdit.class);
								intent.setData(ContentUris.withAppendedId(
										DataProvider.Plans.CONTENT_URI, id));
								PlanFragment.this.getActivity().startActivity(
										intent);
								break;
							case 1:
								((Plans) PlanFragment.this.getActivity())
										.showLogsFragment(id);
								break;
							default:
								break;
							}
						}
					});
			builder.setNegativeButton(android.R.string.cancel, null);
			builder.show();
			return true;
		}
	}

	/**
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	private static class PlanAdapter extends ResourceCursorAdapter {
		/** {@link BackgroundQueryHandler}. */
		private final BackgroundQueryHandler queryHandler;
		/** Token for {@link BackgroundQueryHandler}: plan summary query. */
		private static final int PLAN_QUERY_TOKEN = 0;

		/**
		 * Handle queries in background.
		 * 
		 * @author flx
		 */
		private final class BackgroundQueryHandler extends AsyncQueryHandler {
			/** Reference to {@link Activity}. */
			private final Activity activity;

			/** Handle to views. */
			private final View vLoading, vImport;

			/**
			 * A helper class to help make handling asynchronous queries easier.
			 * 
			 * @param context
			 *            {@link Activity}
			 * @param loading
			 *            Handle to view: loading.
			 * @param imp
			 *            Handle to view: import
			 */
			public BackgroundQueryHandler(final Activity context,
					final View loading, final View imp) {
				super(context.getContentResolver());
				this.activity = context;
				this.vLoading = loading;
				this.vImport = imp;
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void onQueryComplete(final int token,
					final Object cookie, final Cursor cursor) {
				switch (token) {
				case PLAN_QUERY_TOKEN:
					this.vLoading.setVisibility(View.GONE);
					PlanAdapter.this.changeCursor(cursor);
					if (cursor != null && cursor.getCount() > 0) {
						this.vImport.setVisibility(View.GONE);
					} else {
						this.vImport.setVisibility(View.VISIBLE);
					}
					this.activity.setProgressBarIndeterminateVisibility(// .
							Boolean.FALSE);
					return;
				default:
					return;
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public void startQuery(final int token, final Object cookie,
					final Uri uri, final String[] projection,
					final String selection, final String[] selectionArgs,
					final String orderBy) {
				if (PlanAdapter.this.getCount() == 0) {
					vLoading.setVisibility(View.VISIBLE);
				}
				this.activity.setProgressBarIndeterminateVisibility(// .
						Boolean.TRUE);
				super.startQuery(token, cookie, uri, projection, selection,
						selectionArgs, orderBy);
			}
		}

		/** Text sizes. */
		private int textSize, textSizeBigTitle, textSizeTitle, textSizeSpacer,
				textSizePBar, textSizePBarBP;

		/** Prepaid plan? */
		private boolean prepaid;
		/** Visibility for {@link ProgressBar}s. */
		private final int progressBarVisability;

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Activity}
		 * @param lnow
		 *            {@link Calendar} pointing to current time shown
		 * @param lhideZero
		 *            hide zero plans
		 * @param lhideNoCost
		 *            hide no cost plans
		 * @param loading
		 *            Handle to view: loading.
		 * @param imp
		 *            Handle to view: import
		 */
		public PlanAdapter(final Activity context, final Calendar lnow,
				final boolean lhideZero, final boolean lhideNoCost,
				final View loading, final View imp) {
			super(context, R.layout.plans_item, null, true);
			queryHandler = new BackgroundQueryHandler(context, loading, imp);
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (p.getBoolean(Preferences.PREFS_HIDE_PROGRESSBARS, false)) {
				this.progressBarVisability = View.GONE;
			} else {
				this.progressBarVisability = View.VISIBLE;
			}
		}

		/**
		 * Start ConversationList query.
		 * 
		 * @param lnow
		 *            {@link Calendar} pointing to current time shown
		 */
		public final void startPlanQuery(final Calendar lnow) {
			// Cancel any pending queries
			this.queryHandler.cancelOperation(PLAN_QUERY_TOKEN);
			try {
				// Kick off the new query
				Calendar n = lnow;
				if (n == null) {
					n = Calendar.getInstance();
				}
				this.queryHandler.startQuery(
						PLAN_QUERY_TOKEN,
						null,
						DataProvider.Plans.CONTENT_URI_SUM
								.buildUpon()
								.appendQueryParameter(
										DataProvider.Plans.PARAM_DATE,
										String.valueOf(n.getTimeInMillis()))
								.appendQueryParameter(
										DataProvider.Plans.PARAM_HIDE_ZERO,
										String.valueOf(hideZero))
								.appendQueryParameter(
										DataProvider.Plans.PARAM_HIDE_NOCOST,
										String.valueOf(hideNoCost)).build(),
						DataProvider.Plans.PROJECTION_SUM, null, null, null);
			} catch (SQLiteException e) {
				Log.e(TAG, "error starting query", e);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void bindView(final View view, final Context context,
				final Cursor cursor) {
			DataProvider.Plans.Plan plan = new DataProvider.Plans.Plan(cursor);

			String cacheStr = null;
			float cost = plan.getAccumCost();
			float free = plan.getFree();
			if (plan.type != DataProvider.TYPE_SPACING // .
					&& plan.type != DataProvider.TYPE_TITLE) {
				cacheStr = "<b>"
						+ Common.formatValues(context, plan.now, plan.type,
								plan.bpCount, plan.bpBa, plan.billperiod,
								plan.billday, pShowHours) + "</b>";
				if (plan.type != DataProvider.TYPE_BILLPERIOD) {
					if (showTotal) {
						cacheStr += delimiter;
						cacheStr += Common.formatValues(context, plan.now,
								plan.type, plan.atCount, plan.atBa,
								plan.billperiod, plan.billday, pShowHours);
					}
					if (showToday) {
						cacheStr = delimiter + cacheStr;
						cacheStr = Common.formatValues(context, plan.now,
								plan.type, plan.tdCount, plan.tdBa,
								plan.billperiod, plan.billday, pShowHours)
								+ cacheStr;
					}
				}
				if (free > 0f || cost > 0f) {
					cacheStr += "\n";
					if (free > 0f) {
						cacheStr += "(" + String.format(currencyFormat, free)
								+ ")";
					}
					if (cost > 0f) {
						cacheStr += " " + String.format(currencyFormat, cost);
					}
				}
				if (plan.limit > 0) {
					cacheStr = ((int) (plan.usage * CallMeter.HUNDRET)) + "%"
							+ delimiter + cacheStr;
				}
			}

			Log.d(TAG, "plan id: " + plan.id);
			Log.d(TAG, "plan name: " + plan.name);
			Log.d(TAG, "type: " + plan.type);
			Log.d(TAG, "cost: " + cost);
			Log.d(TAG, "limit: " + plan.limit);
			Log.d(TAG, "limitPos: " + plan.limitPos);
			Log.d(TAG, "text: " + cacheStr);

			TextView twCache = null;
			ProgressBar pbCache = null;
			if (plan.type == DataProvider.TYPE_SPACING) {
				final View v = view.findViewById(R.id.spacer);
				if (this.textSizeSpacer > 0) {
					final LayoutParams lp = v.getLayoutParams();
					lp.height = this.textSizeSpacer;
					v.setLayoutParams(lp);
				}
				v.setVisibility(View.INVISIBLE);
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (plan.type == DataProvider.TYPE_TITLE) {
				final TextView tw = ((TextView) view
						.findViewById(R.id.bigtitle));
				tw.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
				if (this.textSizeBigTitle > 0) {
					tw.setTextSize(this.textSizeBigTitle);
				}
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (plan.type == DataProvider.TYPE_BILLPERIOD) {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(
						View.VISIBLE);
				twCache = (TextView) view.findViewById(R.id.period);
				pbCache = (ProgressBar) view.findViewById(R.id.period_pb);
			} else {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.VISIBLE);
				final TextView twNormTitle = (TextView) view
						.findViewById(R.id.normtitle);
				if (this.textSizeTitle > 0) {
					twNormTitle.setTextSize(this.textSizeTitle);
				}
				twNormTitle.setText(cursor
						.getString(DataProvider.Plans.INDEX_NAME));
				twCache = (TextView) view.findViewById(R.id.data);
				if (plan.limit > 0) {
					float bpos = plan.getBillPlanUsage();
					if (plan.usage >= 1) {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitRed);
						view.findViewById(R.id.progressbarLimitGreen)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitYellow)
								.setVisibility(View.GONE);
					} else if (bpos >= 0f && plan.usage > bpos) {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitYellow);
						view.findViewById(R.id.progressbarLimitGreen)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitRed)
								.setVisibility(View.GONE);
					} else {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitGreen);
						view.findViewById(R.id.progressbarLimitYellow)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitRed)
								.setVisibility(View.GONE);
					}
				} else {
					pbCache = (ProgressBar) view
							.findViewById(R.id.progressbarLimitYellow);
					view.findViewById(R.id.progressbarLimitGreen)
							.setVisibility(View.GONE);
					view.findViewById(R.id.progressbarLimitRed).setVisibility(
							View.GONE);
				}
			}
			if (twCache != null && pbCache != null) {
				if (!TextUtils.isEmpty(cacheStr)) {
					twCache.setText(Html.fromHtml(cacheStr.replaceAll("\n",
							"<br>")));
				} else {
					twCache.setText(null);
				}
				if (this.textSize > 0) {
					twCache.setTextSize(this.textSize);
				}
				if (plan.limit == 0) {
					pbCache.setVisibility(View.GONE);
				} else if (plan.limit > 0) {
					pbCache.setIndeterminate(false);
					pbCache.setMax((int) plan.limit);
					pbCache.setProgress((int) plan.limitPos);
					pbCache.setVisibility(this.progressBarVisability);
					int pbs = 0;
					if (plan.type == DataProvider.TYPE_BILLPERIOD) {
						pbs = this.textSizePBarBP;
					} else {
						pbs = this.textSizePBar;
					}
					if (pbs > 0) {
						final LayoutParams lp = pbCache.getLayoutParams();
						lp.height = pbs;
						pbCache.setLayoutParams(lp);
					}
				} else {
					pbCache.setIndeterminate(true);
					pbCache.setVisibility(this.progressBarVisability);
				}
			}
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
		this.fadapter = new PlanFragmentAdapter(this,
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
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		pShowHours = p.getBoolean(Preferences.PREFS_SHOWHOURS, true);
		currentHandler = this.handler;
		Plans.this.setProgressBarIndeterminateVisibility(inProgressMatcher);

		currencyFormat = Preferences.getCurrencyFormat(this);
		Common.setDateFormat(this);
		showToday = p.getBoolean(Preferences.PREFS_SHOWTODAY, false);
		showTotal = p.getBoolean(Preferences.PREFS_SHOWTOTAL, true);
		hideZero = p.getBoolean(Preferences.PREFS_HIDE_ZERO, false);
		hideNoCost = p.getBoolean(Preferences.PREFS_HIDE_NOCOST, false);
		delimiter = p.getString(Preferences.PREFS_DELIMITER, " | ");

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
