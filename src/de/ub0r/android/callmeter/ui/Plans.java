/*
 * Copyright (C) 2009-2010 Felix Bechstein, The Android Open Source Project
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
import java.util.Calendar;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemLongClickListener;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerReceiver;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.PlanEdit;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;

/**
 * Callmeter's Main {@link ListActivity}.
 * 
 * @author flx
 */
public class Plans extends ListActivity implements OnClickListener,
		OnItemLongClickListener, OnDateSetListener {
	/** Tag for output. */
	public static final String TAG = "main";

	/** Extra for setting now. */
	public static final String EXTRA_NOW = "now";

	/** Byte units. */
	private static final String BYTE_UNITS_B = "B";
	/** Byte units: kB. */
	private static final String BYTE_UNITS_KB = "kB";
	/** Byte units: MB. */
	private static final String BYTE_UNITS_MB = "MB";
	/** Byte units: GB. */
	private static final String BYTE_UNITS_GB = "GB";
	/** Byte units: TB. */
	private static final String BYTE_UNITS_TB = "TB";

	/** Dialog: update. */
	private static final int DIALOG_UPDATE = 0;
	/** Dialog: show time. */
	private static final int DIALOG_SHOW_TIME = 1;

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

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";

	/** Show hours and days. */
	private static boolean pShowHours = true;

	/** Selected currency format. */
	private static String currencyFormat = "$%.2f";
	/** Selected date format. */
	private static String dateFormat = null;

	/** Force adapter to reload the list. */
	private static boolean reloadList = false;

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

				Plans.this.adapter.updatePlans();
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
			if (Plans.this.getListView().getCount() == 0) {
				Plans.this.findViewById(R.id.import_default).setVisibility(
						View.VISIBLE);
			} else {
				Plans.this.findViewById(R.id.import_default).setVisibility(
						View.GONE);
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
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	private static class PlanAdapter extends ResourceCursorAdapter {
		/** Newest {@link AsyncTask} which should run. */
		private static Object runningTask = null;

		/** A plan. */
		private class Plan {
			/** {@link Context}. */
			private final Context ctx;

			/** Type of plan. */
			private final int type;
			/** Id of plan. */
			private final long id;
			/** Bill period. */
			private final int billperiod;
			/** Bill period id. */
			private final long billperiodid;
			/** Bill day. */
			private final long billday;
			/** Bill day as {@link Calendar}. */
			private final Calendar billdayc;
			/** Type of limit. */
			private final int limittype;
			/** Limit. */
			private final long limit;
			/** Cost per plan. */
			private final float cpp;

			/**
			 * Default Constructor from {@link Cursor}.
			 * 
			 * @param context
			 *            {@link Context}
			 * @param cursor
			 *            {@link Cursor}
			 */
			Plan(final Context context, final Cursor cursor) {
				this.ctx = context;
				this.type = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
				this.id = cursor.getLong(DataProvider.Plans.INDEX_ID);
				this.billperiod = cursor
						.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
				this.billperiodid = cursor
						.getLong(DataProvider.Plans.INDEX_BILLPERIOD_ID);
				if (this.type == DataProvider.TYPE_SPACING
						|| this.type == DataProvider.TYPE_TITLE) {
					this.billday = -1;
					this.billdayc = null;
					this.limittype = -1;
					this.limit = -1;
					this.cpp = 0;
				} else if (this.type == DataProvider.TYPE_BILLPERIOD) {
					this.billday = cursor
							.getLong(DataProvider.Plans.INDEX_BILLDAY);
					this.billdayc = Calendar.getInstance();
					this.billdayc.setTimeInMillis(this.billday);
					this.limittype = -1;
					this.limit = -1;
					this.cpp = cursor
							.getFloat(DataProvider.Plans.INDEX_COST_PER_PLAN);
				} else {
					this.billday = -1;
					this.billdayc = null;
					this.limittype = cursor
							.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
					this.limit = DataProvider.Plans.getLimit(this.type,
							this.limittype, cursor
									.getLong(DataProvider.Plans.INDEX_LIMIT));
					this.cpp = cursor
							.getFloat(DataProvider.Plans.INDEX_COST_PER_PLAN);
				}
			}

			/**
			 * Update the plan and write cached data to database.
			 */
			final void update() {
				if (this.type == DataProvider.TYPE_SPACING
						|| this.type == DataProvider.TYPE_TITLE) {
					return;
				}
				final Calendar lnow = PlanAdapter.this.now;
				final Uri uri = ContentUris.withAppendedId(
						DataProvider.Plans.CONTENT_URI, this.id);
				final ContentValues cv = new ContentValues();

				if (this.type == DataProvider.TYPE_BILLPERIOD) {
					Calendar billDay = Calendar.getInstance();
					billDay.setTimeInMillis(this.billday);
					billDay = DataProvider.Plans.getBillDay(this.billperiod,
							billDay, lnow, false);

					if (this.billperiod == DataProvider.BILLPERIOD_INFINITE) {
						cv.put(DataProvider.Plans.CACHE_PROGRESS_MAX, -1);
						cv.put(DataProvider.Plans.CACHE_PROGRESS_POS, 1);
						cv.put(DataProvider.Plans.CACHE_STRING, "\u221E");
					} else {
						final Calendar nextBillDay = DataProvider.Plans
								.getBillDay(this.billperiod, billDay, lnow,
										true);
						final long pr = billDay.getTimeInMillis()
								/ CallMeter.MILLIS;
						final long nx = (nextBillDay.getTimeInMillis() // .
								/ CallMeter.MILLIS)
								- pr;
						long nw;
						if (lnow == null) {
							nw = System.currentTimeMillis();
						} else {
							nw = lnow.getTimeInMillis();
						}
						nw = (nw / CallMeter.MILLIS) - pr;

						cv.put(DataProvider.Plans.CACHE_PROGRESS_MAX, nx);
						cv.put(DataProvider.Plans.CACHE_PROGRESS_POS, nw);
						String formatedDate;
						if (dateFormat == null) {
							formatedDate = DateFormat.getDateFormat(this.ctx)
									.format(billDay.getTime());
						} else {
							formatedDate = String.format(dateFormat, billDay,
									billDay, billDay);
						}
						cv.put(DataProvider.Plans.CACHE_STRING, formatedDate);
					}
				} else {
					int used = 0;
					if (this.limit > 0) {
						cv.put(DataProvider.Plans.CACHE_PROGRESS_MAX,
								this.limit);
					} else {
						cv.put(DataProvider.Plans.CACHE_PROGRESS_MAX, 0);
					}
					if (this.id < 0) {
						return;
					}
					final Plan billp = PlanAdapter.this
							.getPlan(this.billperiodid);
					String where = null;
					if (billp != null) {
						where = DataProvider.Plans.getBilldayWhere(
								billp.billperiod, billp.billdayc, lnow);
					}
					where = DbUtils.sqlAnd(where, DataProvider.Logs.PLAN_ID
							+ " = " + this.id);
					Log.d(TAG, "where: " + where);

					float cost = -1;
					long billedAmount = 0;
					int count = 0;
					long allBilledAmount = 0;
					int allCount = 0;

					Cursor c = this.ctx.getContentResolver()
							.query(DataProvider.Logs.SUM_URI,
									DataProvider.Logs.PROJECTION_SUM, where,
									null, null);
					if (c != null && c.moveToFirst()) {
						cost = c.getFloat(DataProvider.Logs.INDEX_SUM_COST);
						billedAmount = c.getLong(DataProvider.Logs.// .
								INDEX_SUM_BILL_AMOUNT);
						count = c.getInt(DataProvider.Logs.INDEX_SUM_COUNT);

						Log.d(TAG, "plan: " + this.id);
						Log.d(TAG, "count: " + count);
						Log.d(TAG, "cost: " + cost);
						Log.d(TAG, "billedAmount: " + billedAmount);

						used = DataProvider.Plans.getUsed(this.type,
								this.limittype, billedAmount, cost);

						cv.put(DataProvider.Plans.CACHE_PROGRESS_POS, used);
					} else {
						cv.put(DataProvider.Plans.CACHE_PROGRESS_POS, -1);
					}
					if (c != null && !c.isClosed()) {
						c.close();
					}
					// get all time
					c = this.ctx.getContentResolver().query(
							DataProvider.Logs.SUM_URI,
							DataProvider.Logs.PROJECTION_SUM,
							DataProvider.Logs.PLAN_ID + " = " + this.id, null,
							null);
					if (c != null && c.moveToFirst()) {
						allBilledAmount = c.getLong(DataProvider.Logs.// .
								INDEX_SUM_BILL_AMOUNT);
						allCount = c.getInt(DataProvider.Logs.INDEX_SUM_COUNT);
					}
					if (c != null && !c.isClosed()) {
						c.close();
					}

					if (this.cpp > 0) {
						if (cost <= 0) {
							cost = this.cpp;
						} else {
							cost += this.cpp;
						}
					}
					cv.put(DataProvider.Plans.CACHE_COST, cost);
					cv.put(DataProvider.Plans.CACHE_STRING, getString(this,
							used, count, billedAmount, cost, allBilledAmount,
							allCount, pShowHours));
				}
				this.ctx.getContentResolver().update(uri, cv, null, null);
			}
		}

		/** Now. */
		private Calendar now;
		/** Separator for the data. */
		private static final String SEP = " | ";

		/** List of plans. */
		private ArrayList<Plan> plansList = new ArrayList<Plan>();
		/** Hash of plans. plan.id -> plan. */
		private HashMap<Long, Plan> plansMap = new HashMap<Long, Plan>();

		/** {@link Context}. */
		private final Context ctx;
		/** Textsize. */
		private final int textSize;

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		public PlanAdapter(final Context context) {
			super(context, R.layout.plans_item, context.getContentResolver()
					.query(DataProvider.Plans.CONTENT_URI,
							DataProvider.Plans.PROJECTION, null, null,
							DataProvider.Plans.ORDER), true);
			this.ctx = context;
			this.textSize = Preferences.getTextsize(context);
		}

		/**
		 * Set date of now.
		 * 
		 * @param time
		 *            time in milliseconds
		 */
		private void setNow(final long time) {
			if (time <= 0) {
				this.now = null;
			} else if (this.now == null) {
				this.now = Calendar.getInstance();
			}
			this.now.setTimeInMillis(time);
		}

		/**
		 * Get a {@link Plan} by id.
		 * 
		 * @param id
		 *            id of {@link Plan}
		 * @return {@link Plan}
		 */
		private Plan getPlan(final long id) {
			return this.plansMap.get(id);
		}

		/**
		 * Append a {@link Plan} to the list of plans.
		 * 
		 * @param plan
		 *            {@link Plan}
		 */
		private void appendPlan(final Plan plan) {
			this.plansList.add(plan);
			this.plansMap.put(plan.id, plan);
		}

		/**
		 * Reload all {@link Plan}s from {@link Cursor}.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param cursor
		 *            {@link Cursor}
		 */
		synchronized void reloadPlans(final ContentResolver cr,
				final Cursor cursor) {
			this.plansList.clear();
			this.plansMap.clear();
			if (cursor == null) {
				return;
			}
			if (!cursor.moveToFirst()) {
				return;
			}
			do {
				this.appendPlan(new Plan(this.ctx, cursor));
			} while (cursor.moveToNext());
		}

		/**
		 * Update bill period's cost.
		 * 
		 * @param p
		 *            bill period
		 */
		private void updateBillperiodCost(final Plan p) {
			final ContentResolver cr = this.ctx.getContentResolver();
			final Cursor cursor = cr.query(DataProvider.Plans.CONTENT_URI,
					new String[] { DataProvider.Plans.CACHE_COST },
					DataProvider.Plans.BILLPERIOD_ID + " == " + p.id, null,
					null);
			float cost = p.cpp;
			if (cursor != null && cursor.moveToFirst()) {
				do {
					final float c = cursor.getFloat(0);
					if (c > 0) {
						cost += c;
					}
				} while (cursor.moveToNext());
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			final ContentValues cv = new ContentValues();
			cv.put(DataProvider.Plans.CACHE_COST, cost);
			cr.update(ContentUris.withAppendedId(
					DataProvider.Plans.CONTENT_URI, p.id), cv, null, null);
		}

		/** Update all {@link Plan}s in background. */
		void updatePlans() {
			Log.d(TAG, "updatePlans()");
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(final Void... params) {
					Log.d(TAG, "updatePlans().task start");
					runningTask = this;
					if (currentHandler != null) {
						currentHandler.sendEmptyMessage(// .
								MSG_BACKGROUND_START_PLANADAPTER);
					}
					final int l = PlanAdapter.this.plansList.size();
					// update bill periods
					for (int i = 0; i < l; i++) {
						if (runningTask != this) {
							break;
						}
						final Plan p = PlanAdapter.this.plansList.get(i);
						if (p.type == DataProvider.TYPE_BILLPERIOD) {
							p.update();
						}
					}
					// update rest
					for (int i = 0; i < l; i++) {
						if (runningTask != this) {
							break;
						}
						final Plan p = PlanAdapter.this.plansList.get(i);
						if (p.type != DataProvider.TYPE_BILLPERIOD) {
							p.update();
						}
					}
					// update cost in bill periods
					for (int i = 0; i < l; i++) {
						if (runningTask != this) {
							break;
						}
						final Plan p = PlanAdapter.this.plansList.get(i);
						if (p.type == DataProvider.TYPE_BILLPERIOD) {
							PlanAdapter.this.updateBillperiodCost(p);
						}
					}
					final boolean last = runningTask == this;
					if (last && currentHandler != null) {
						currentHandler.sendEmptyMessage(// .
								MSG_BACKGROUND_STOP_PLANADAPTER);
					}
					Log.d(TAG, "updatePlans().task finished: " + last);
					return null;
				}
			} // .
					.execute((Void) null);
		}

		/**
		 * Get a {@link String} showing all the data to the user.
		 * 
		 * @param p
		 *            {@link Plan}
		 * @param used
		 *            used limit
		 * @param count
		 *            count
		 * @param amount
		 *            billed amount
		 * @param cost
		 *            cost
		 * @param allAmount
		 *            billed amount all time
		 * @param allCount
		 *            count all time
		 * @return {@link String} holding all the data
		 */
		private static String getString(final Plan p, final int used,
				final long count, final long amount, final float cost,
				final long allAmount, final long allCount,
				final boolean showHours) {
			final StringBuilder ret = new StringBuilder();

			// usage
			if (p.limittype != DataProvider.LIMIT_TYPE_NONE && p.limit > 0) {
				ret.append((used * CallMeter.HUNDRET) / p.limit);
				ret.append("%");
				ret.append(SEP);
			}

			// amount
			ret.append(formatAmount(p.type, amount, showHours));
			// count
			if (p.type == DataProvider.TYPE_CALL) {
				ret.append(" (" + count + ")");
			}
			ret.append(SEP);

			// amount all time
			ret.append(formatAmount(p.type, allAmount, showHours));
			// count all time
			if (p.type == DataProvider.TYPE_CALL) {
				ret.append(" (" + allCount + ")");
			}

			// cost
			if (cost >= 0) {
				ret.append("\n");
				ret.append(String.format(currencyFormat, cost));
			}
			return ret.toString();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context context,
				final Cursor cursor) {
			final int t = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
			final int cacheLimitMax = cursor
					.getInt(DataProvider.Plans.INDEX_CACHE_PROGRESS_MAX);
			final int cacheLimitPos = cursor
					.getInt(DataProvider.Plans.INDEX_CACHE_PROGRESS_POS);
			String cacheStr = cursor
					.getString(DataProvider.Plans.INDEX_CACHE_STRING);
			TextView twCache = null;
			ProgressBar pbCache = null;
			if (t == DataProvider.TYPE_SPACING) {
				view.findViewById(R.id.spacer).setVisibility(View.INVISIBLE);
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (t == DataProvider.TYPE_TITLE) {
				final TextView tw = ((TextView) view
						.findViewById(R.id.bigtitle));
				tw.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (t == DataProvider.TYPE_BILLPERIOD) {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(
						View.VISIBLE);
				twCache = (TextView) view.findViewById(R.id.period);
				pbCache = (ProgressBar) view.findViewById(R.id.period_pb);

				final float cost = cursor
						.getFloat(DataProvider.Plans.INDEX_CACHE_COST);
				if (cost >= 0) {
					cacheStr += "\n" + String.format(currencyFormat, cost);
				}
			} else {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.VISIBLE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
				((TextView) view.findViewById(R.id.normtitle)).setText(cursor
						.getString(DataProvider.Plans.INDEX_NAME));
				twCache = (TextView) view.findViewById(R.id.data);
				int usage = 0;
				if (cacheLimitMax > 0) {
					usage = (cacheLimitPos * CallMeter.HUNDRET) / cacheLimitMax;
					if (usage >= CallMeter.HUNDRET) {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitRed);
						view.findViewById(R.id.progressbarLimitGreen)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitYellow)
								.setVisibility(View.GONE);
					} else if (usage > CallMeter.EIGHTY) {
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
				twCache.setText(cacheStr);
				if (this.textSize > 0) {
					twCache.setTextSize(this.textSize);
				}
				if (cacheLimitMax == 0) {
					pbCache.setVisibility(View.GONE);
				} else if (cacheLimitMax > 0) {
					pbCache.setIndeterminate(false);
					pbCache.setMax(cacheLimitMax);
					pbCache.setProgress(cacheLimitPos);

					pbCache.setVisibility(View.VISIBLE);
				} else {
					pbCache.setIndeterminate(true);
					pbCache.setVisibility(View.VISIBLE);
				}
			}
		}
	}

	/** Display ads? */
	private static boolean prefsNoAds;

	/** SharedPreferences. */
	private SharedPreferences preferences;
	/** Plans. */
	private PlanAdapter adapter = null;

	/**
	 * Return pretty bytes.
	 * 
	 * @author Cyril Jaquier
	 * @param value
	 *            bytes
	 * @return pretty bytes
	 */
	public static final String prettyBytes(final long value) {
		StringBuilder sb = new StringBuilder();
		if (value < CallMeter.BYTE_KB) {
			sb.append(String.valueOf(value));
			sb.append(BYTE_UNITS_B);
		} else if (value < CallMeter.BYTE_MB) {
			sb.append(String.format("%.1f", value / (CallMeter.BYTE_KB * 1.0)));
			sb.append(BYTE_UNITS_KB);
		} else if (value < CallMeter.BYTE_GB) {
			sb.append(String.format("%.2f", value / (CallMeter.BYTE_MB * 1.0)));
			sb.append(BYTE_UNITS_MB);
		} else if (value < CallMeter.BYTE_TB) {
			sb.append(String.format("%.3f", value / (CallMeter.BYTE_GB * 1.0)));
			sb.append(BYTE_UNITS_GB);
		} else {
			sb.append(String.format("%.4f", value / (CallMeter.BYTE_TB * 1.0)));
			sb.append(BYTE_UNITS_TB);
		}
		return sb.toString();
	}

	/**
	 * Parse number of seconds to a readable time format.
	 * 
	 * @param seconds
	 *            seconds
	 * @param showHours
	 *            show hours and days
	 * @return parsed string
	 */
	public static final String prettySeconds(final long seconds,
			final boolean showHours) {
		String ret;
		int d, h, m;
		if (showHours) {
			d = (int) (seconds / CallMeter.SECONDS_DAY);
			h = (int) ((seconds % CallMeter.SECONDS_DAY) / // .
			CallMeter.SECONDS_HOUR);
			m = (int) ((seconds % CallMeter.SECONDS_HOUR) / // .
			CallMeter.SECONDS_MINUTE);
		} else {
			d = 0;
			h = 0;
			m = (int) (seconds / CallMeter.SECONDS_MINUTE);
		}
		final int s = (int) (seconds % CallMeter.SECONDS_MINUTE);
		if (d > 0) {
			ret = d + "d ";
		} else {
			ret = "";
		}
		if (h > 0 || d > 0) {
			if (h < CallMeter.TEN) {
				ret += "0";
			}
			ret += h + ":";
		}
		if (m > 0 || h > 0 || d > 0) {
			if (m < CallMeter.TEN && h > 0) {
				ret += "0";
			}
			ret += m + ":";
		}
		if (s < CallMeter.TEN && (m > 0 || h > 0 || d > 0)) {
			ret += "0";
		}
		ret += s;
		if (d == 0 && h == 0 && m == 0) {
			ret += "s";
		}
		return ret;
	}

	/**
	 * Format amount regarding type of plan.
	 * 
	 * @param pType
	 *            type of plan
	 * @param amount
	 *            amount
	 * @param showHours
	 *            show hours and days
	 * @return {@link String} representing amount
	 */
	public static final String formatAmount(final int pType, // .
			final long amount, final boolean showHours) {
		switch (pType) {
		case DataProvider.TYPE_DATA:
			return prettyBytes(amount);
		case DataProvider.TYPE_CALL:
			return prettySeconds(amount, showHours);
		default:
			return String.valueOf(amount);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		currentHandler = this.handler;
		// get prefs.
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		final boolean showTitlebar = this.preferences.getBoolean(
				Preferences.PREFS_SHOWTITLEBAR, true);
		if (!showTitlebar) {
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		this.setTheme(Preferences.getTheme(this));
		this.setContentView(R.layout.plans);

		String v0 = this.preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			this.preferences.edit().putString(PREFS_LAST_RUN, v1).commit();
			if (v0.length() > 0) {
				this.showDialog(DIALOG_UPDATE);
			}
		}

		prefsNoAds = DonationHelper.hideAds(this);
		this.findViewById(R.id.import_default).setOnClickListener(this);
		this.getListView().setOnItemLongClickListener(this);

		// TextView tv = (TextView) this.findViewById(R.id.calls_);
		// Preferences.textSizeMedium = tv.getTextSize();
		// tv = (TextView) this.findViewById(R.id.calls1_in_);
		// Preferences.textSizeSmall = tv.getTextSize();

		this.adapter = new PlanAdapter(this);
		this.setListAdapter(this.adapter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		pShowHours = PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(Preferences.PREFS_SHOWHOURS, true);
		currentHandler = this.handler;
		Plans.this.setProgressBarIndeterminateVisibility(inProgressMatcher);

		currencyFormat = Preferences.getCurrencyFormat(this);
		dateFormat = Preferences.getDateFormat(this);

		if (reloadList) {
			this.adapter = new PlanAdapter(this);
			this.setListAdapter(this.adapter);
		}

		if (this.getListView().getCount() == 0) {
			this.findViewById(R.id.import_default).setVisibility(View.VISIBLE);
			this.findViewById(R.id.ad).setVisibility(View.GONE);
		} else {
			this.findViewById(R.id.import_default).setVisibility(View.GONE);
			if (!prefsNoAds) {
				this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
			}
		}
		// reload plan configuration
		this.getNow(this.getIntent(), false);
		this.adapter.reloadPlans(this.getContentResolver(), this.adapter
				.getCursor());
		// start LogRunner
		LogRunnerService.update(this, LogRunnerReceiver.ACTION_FORCE_UPDATE);
		// schedule next update
		LogRunnerReceiver.schedNext(this);
	}

	/**
	 * Get "now" from {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}
	 * @param updatePlans
	 *            update plans after setting "now"
	 */
	private void getNow(final Intent intent, final boolean updatePlans) {
		final long now = intent.getLongExtra(EXTRA_NOW, -1);
		if (now >= 0) {
			this.adapter.setNow(now);
			String formatedDate;
			if (dateFormat == null) {
				formatedDate = DateFormat.getDateFormat(this).format(now);
			} else {
				final Calendar n = this.adapter.now;
				formatedDate = String.format(dateFormat, n, n, n);
			}
			String t = this.getString(R.string.app_name) + " - " + formatedDate;
			this.setTitle(t);
		} else {
			this.adapter.now = null;
			this.setTitle(R.string.app_name);
		}
		if (updatePlans) {
			this.adapter.updatePlans();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onNewIntent(final Intent intent) {
		this.setIntent(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPause() {
		super.onPause();
		currentHandler = null;
	}

	/**
	 * Get the current {@link Handler}.
	 * 
	 * @return {@link Handler}.
	 */
	public static final Handler getHandler() {
		return currentHandler;
	}

	/** Force the Adapter to reload the list. */
	public static final void reloadList() {
		reloadList = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		AlertDialog.Builder builder;
		switch (id) {
		case DIALOG_UPDATE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			StringBuilder buf = new StringBuilder();

			buf.append(changes[0]);
			for (int i = 1; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString());
			buf = null;
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		case DIALOG_SHOW_TIME:
			Calendar cal = this.adapter.now;
			if (cal == null) {
				cal = Calendar.getInstance();
			}
			return new DatePickerDialog(this, this, cal.get(Calendar.YEAR), cal
					.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		default:
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		if (prefsNoAds) {
			menu.removeItem(R.id.item_donate);
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_settings:
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			this.startActivity(new Intent(this, DonationHelper.class));
			return true;
		case R.id.item_logs:
			this.startActivity(new Intent(this, Logs.class));
			return true;
		case R.id.item_show_time:
			this.showDialog(DIALOG_SHOW_TIME);
			return true;
		default:
			return false;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onClick(final View v) {
		switch (v.getId()) {
		case R.id.import_default:
			final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(this
					.getString(R.string.url_rulesets)));
			try {
				this.startActivity(intent);
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no activity to load url", e);
				Toast.makeText(this,
						"no activity to load url: " + intent.getDataString(),
						Toast.LENGTH_LONG).show();
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
	public final boolean onItemLongClick(final AdapterView<?> parent,
			final View view, final int position, final long id) {
		final Builder builder = new Builder(this);
		builder.setItems(R.array.dialog_edit_plan,
				new android.content.DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						switch (which) {
						case 0:
							final Intent intent = new Intent(// .
									Plans.this, PlanEdit.class);
							intent.setData(ContentUris.withAppendedId(
									DataProvider.Plans.CONTENT_URI, id));
							Plans.this.startActivity(intent);
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

	/**
	 * {@inheritDoc}
	 */
	public final void onDateSet(final DatePicker view, final int year,
			final int monthOfYear, final int dayOfMonth) {
		final Calendar date = Calendar.getInstance();
		date.set(year, monthOfYear, dayOfMonth);
		final Intent intent = new Intent(this, Plans.class);
		intent.putExtra(EXTRA_NOW, date.getTimeInMillis());
		if (this.adapter.now != null) {
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		this.startActivity(intent);
	}
}
