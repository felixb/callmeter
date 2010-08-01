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
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
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
		OnItemLongClickListener {
	/** Tag for output. */
	public static final String TAG = "main";

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
				if (statusMatcher != null && statusMatcher.isShowing()) {
					Log.d(TAG, "matcher progress: " + msg.arg1);
					if (msg.arg1 == 0) {
						final ProgressDialog dold = statusMatcher;
						statusMatcher = new ProgressDialog(Plans.this);
						statusMatcher.setCancelable(true);
						statusMatcher.setMessage(Plans.this
								.getString(R.string.reset_data_progr2));
						statusMatcher.setProgressStyle(// .
								ProgressDialog.STYLE_HORIZONTAL);
						statusMatcher.setMax(msg.arg2);
						statusMatcher.setIndeterminate(false);
						statusMatcher.show();
						dold.dismiss();
					}
					statusMatcher.setProgress(msg.arg1);
					// statusMatcher.show();
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
				if (statusMatcher == null || !statusMatcher.isShowing()) {
					statusMatcher = new ProgressDialog(Plans.this);
					statusMatcher.setCancelable(true);
					statusMatcher.setMessage(Plans.this
							.getString(R.string.reset_data_progr1));
					statusMatcher.setIndeterminate(true);
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
					this.cpp = 0;
				} else {
					this.billday = -1;
					this.billdayc = null;
					this.limittype = cursor
							.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
					long curLimit = cursor
							.getLong(DataProvider.Plans.INDEX_LIMIT);
					switch (this.limittype) {
					case DataProvider.LIMIT_TYPE_UNITS:
						curLimit = DataProvider.Plans.getLimit(this.type,
								curLimit);
						break;
					case DataProvider.LIMIT_TYPE_COST:
						curLimit = curLimit * CallMeter.HUNDRET;
						break;
					default:
						curLimit = -1;
						break;
					}
					this.limit = curLimit;
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
				final Uri uri = ContentUris.withAppendedId(
						DataProvider.Plans.CONTENT_URI, this.id);
				final ContentValues cv = new ContentValues();

				if (this.type == DataProvider.TYPE_BILLPERIOD) {
					Calendar billDay = Calendar.getInstance();
					billDay.setTimeInMillis(this.billday);
					billDay = DataProvider.Plans.getBillDay(this.billperiod,
							billDay, null, false);

					if (this.billperiod == DataProvider.BILLPERIOD_INFINITE) {
						cv.put(DataProvider.Plans.CACHE_PROGRESS_MAX, -1);
						cv.put(DataProvider.Plans.CACHE_PROGRESS_POS, 1);
						cv.put(DataProvider.Plans.CACHE_STRING, "\u221E");
					} else {
						final Calendar nextBillDay = DataProvider.Plans
								.getBillDay(this.billperiod, billDay, null,
										true);
						final long pr = billDay.getTimeInMillis()
								/ CallMeter.MILLIS;
						final long nx = (nextBillDay.getTimeInMillis() // .
								/ CallMeter.MILLIS)
								- pr;
						final long nw = (PlanAdapter.this.now.// .
								getTimeInMillis() / CallMeter.MILLIS) - pr;
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
								billp.billperiod, billp.billdayc,
								PlanAdapter.this.now);
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

						switch (this.limittype) {
						case DataProvider.LIMIT_TYPE_COST:
							used = (int) (cost * CallMeter.HUNDRET);
							break;
						case DataProvider.LIMIT_TYPE_UNITS:
							used = getUsed(this.type, billedAmount);
							break;
						default:
							used = 0;
							break;
						}

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

					cv.put(DataProvider.Plans.CACHE_STRING, getString(this,
							used, count, billedAmount, cost, allBilledAmount,
							allCount));
				}
				this.ctx.getContentResolver().update(uri, cv, null, null);
			}
		}

		/** Now. */
		private final Calendar now;
		/** Separator for the data. */
		private static final String SEP = " | ";

		/** List of plans. */
		private ArrayList<Plan> plansList = new ArrayList<Plan>();
		/** Hash of plans. plan.id -> plan. */
		private HashMap<Long, Plan> plansMap = new HashMap<Long, Plan>();

		/** {@link Context}. */
		private final Context ctx;

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
			this.now = Calendar.getInstance();
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
		 * Calculate used amount for given plan.
		 * 
		 * @param pType
		 *            type of plan
		 * @param amount
		 *            amount
		 * @return used amount
		 */
		private static int getUsed(final int pType, final long amount) {
			switch (pType) {
			case DataProvider.TYPE_DATA:
				return (int) (amount / CallMeter.BYTE_KB);
			default:
				return (int) amount;
			}
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
				final long allAmount, final long allCount) {
			final StringBuilder ret = new StringBuilder();

			// usage
			if (p.limittype != DataProvider.LIMIT_TYPE_NONE && p.limit > 0) {
				ret.append((used * CallMeter.HUNDRET) / p.limit);
				ret.append("%");
				ret.append(SEP);
			}

			// amount
			ret.append(formatAmount(p.type, amount));
			// count
			if (p.type == DataProvider.TYPE_CALL) {
				ret.append(" (" + count + ")");
			}
			ret.append(SEP);

			// amount all time
			ret.append(formatAmount(p.type, allAmount));
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
			final String cacheStr = cursor
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
			} else {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.spacer).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.VISIBLE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
				((TextView) view.findViewById(R.id.normtitle)).setText(cursor
						.getString(DataProvider.Plans.INDEX_NAME));
				twCache = (TextView) view.findViewById(R.id.data);
				pbCache = (ProgressBar) view
						.findViewById(R.id.progressbarLimit);
			}
			if (twCache != null && pbCache != null) {
				twCache.setText(cacheStr);
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
	 * @return parsed string
	 */
	public static final String prettySeconds(final long seconds) {
		String ret;
		int d = (int) (seconds / CallMeter.SECONDS_DAY);
		int h = (int) ((seconds % CallMeter.SECONDS_DAY) / // .
		CallMeter.SECONDS_HOUR);
		int m = (int) ((seconds % CallMeter.SECONDS_HOUR) / // .
		CallMeter.SECONDS_MINUTE);
		int s = (int) (seconds % CallMeter.SECONDS_MINUTE);
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
	 * @return {@link String} representing amount
	 */
	public static final String formatAmount(final int pType, // .
			final long amount) {
		switch (pType) {
		case DataProvider.TYPE_DATA:
			return prettyBytes(amount);
		case DataProvider.TYPE_CALL:
			return prettySeconds(amount);
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
		this.setTheme(Preferences.getTheme(this));
		this.setContentView(R.layout.plans);
		// get prefs.
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
		String v0 = this.preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = this.preferences.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
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
		currentHandler = this.handler;
		Plans.this.setProgressBarIndeterminateVisibility(inProgressMatcher);
		if (!prefsNoAds) {
			this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		}
		currencyFormat = Preferences.getCurrencyFormat(this);
		dateFormat = Preferences.getDateFormat(this);

		if (reloadList) {
			this.adapter = new PlanAdapter(this);
			this.setListAdapter(this.adapter);
		}

		if (this.getListView().getCount() == 0) {
			this.findViewById(R.id.import_default).setVisibility(View.VISIBLE);
		} else {
			this.findViewById(R.id.import_default).setVisibility(View.GONE);
		}
		// reload plan configuration
		this.adapter.reloadPlans(this.getContentResolver(), this.adapter
				.getCursor());
		// start LogRunner
		LogRunnerService.update(this, LogRunnerReceiver.ACTION_FORCE_UPDATE);
		// schedule next update
		LogRunnerReceiver.schedNext(this);
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
}
