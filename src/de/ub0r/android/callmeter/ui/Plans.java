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
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.ActivityNotFoundException;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import de.ub0r.android.callmeter.Ads;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.DataProvider.Plans.Plan;
import de.ub0r.android.callmeter.data.LogRunnerReceiver;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.PlanEdit;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.ChangelogHelper;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Callmeter's Main {@link ListActivity}.
 * 
 * @author flx
 */
public class Plans extends ListActivity implements OnClickListener,
		OnItemLongClickListener, OnDateSetListener, OnTimeSetListener {
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

	/** Dialog: show date. */
	private static final int DIALOG_SHOW_DATE = 1;
	/** Dialog: show time. */
	private static final int DIALOG_SHOW_TIME = 2;

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
	/** Selected date format. */
	private static String dateFormat = null;

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

	/** Plans. */
	private PlanAdapter adapter = null;

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

				// FIXME Plans.this.adapter.updatePlans();
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
	 * Class holding Status of a {@link Plan}.
	 * 
	 * @author flx
	 */
	@Deprecated
	public static final class PlanStatus {
		/** Id of plan. */
		public long id = -1L;
		/** Count of items. */
		public int count = 0;
		/** Billed amount. */
		public float billedAmount = 0f;
		/** Cost. */
		public float cost = 0f;

		/**
		 * Get {@link PlanStatus} from database.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param where
		 *            where clause on DataProvider.Logs.SUM_URI
		 * @param mixedMerger
		 *            isMerger && type == mixed
		 * @param upc
		 *            units per minute
		 * @param upm
		 *            units per mms
		 * @param ups
		 *            units per sms
		 * @return {@link PlanStatus}
		 */
		public static PlanStatus get(final ContentResolver cr,
				final String where, final boolean mixedMerger, final long upc,
				final long upm, final long ups) {

			final Cursor c = cr.query(DataProvider.Logs.SUM_URI,
					DataProvider.Logs.PROJECTION_SUM, where, null, null);
			if (c == null || !c.moveToFirst()) {
				if (c != null && !c.isClosed()) {
					c.close();
				}
				return null;
			}
			final PlanStatus ret = new PlanStatus();
			ret.id = c.getLong(DataProvider.Logs.INDEX_SUM_PLAN_ID);
			do {
				ret.cost += c.getFloat(DataProvider.Logs.INDEX_SUM_COST);
				float ba = c.getFloat(DataProvider.Logs.// .
						INDEX_SUM_BILL_AMOUNT);
				if (mixedMerger) {
					final int t = c.getInt(// .
							DataProvider.Logs.INDEX_SUM_PLAN_TYPE);
					switch (t) {
					case DataProvider.TYPE_CALL:
						ba = ba * upc / CallMeter.SECONDS_MINUTE;
						break;
					case DataProvider.TYPE_MMS:
						ba *= upm;
						break;
					case DataProvider.TYPE_SMS:
						ba *= ups;
						break;
					default:
						break;
					}
				}
				ret.billedAmount += ba;
				ret.count += c.getInt(DataProvider.Logs.INDEX_SUM_COUNT);
			} while (c.moveToNext());

			if (!c.isClosed()) {
				c.close();
			}
			return ret;
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

			/**
			 * A helper class to help make handling asynchronous
			 * {@link ContentResolver} queries easier.
			 * 
			 * @param cr
			 *            {@link ContentResolver}
			 */
			public BackgroundQueryHandler(final ContentResolver cr) {
				super(cr);
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			protected void onQueryComplete(final int token,
					final Object cookie, final Cursor cursor) {
				switch (token) {
				case PLAN_QUERY_TOKEN:
					PlanAdapter.this.changeCursor(cursor);
					// FIXME PlanAdapter.this.activity
					// .setProgressBarIndeterminateVisibility(Boolean.FALSE);
					return;
				default:
					return;
				}
			}
		}

		/** Now. */
		private Calendar now;

		/** {@link Context}. */
		private final Context ctx; // FIXME: delete
		/** Textsizes. */
		private int textSize, textSizeBigTitle, textSizeTitle, textSizeSpacer,
				textSizePBar, textSizePBarBP;
		/** Prepaid plan? */
		private boolean prepaid;
		/** Visability for {@link ProgressBar}s. */
		private final int progressBarVisability;

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 * @param lnow
		 *            {@link Calendar} pointing to current time shown
		 * @param lhideZero
		 *            hide zero plans
		 * @param lhideNoCost
		 *            hide no cost plans
		 */
		public PlanAdapter(final Context context, final Calendar lnow,
				final boolean lhideZero, final boolean lhideNoCost) {
			super(context, R.layout.plans_item, null, true);
			this.ctx = context;
			queryHandler = new BackgroundQueryHandler(
					context.getContentResolver());
			final SharedPreferences p = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (p.getBoolean(Preferences.PREFS_HIDE_PROGRESSBARS, false)) {
				this.progressBarVisability = View.GONE;
			} else {
				this.progressBarVisability = View.VISIBLE;
			}
			// this.updateGUI();
		}

		/**
		 * Start ConversationList query.
		 * 
		 * @param lnow
		 *            {@link Calendar} pointing to current time shown
		 * @param lhideZero
		 *            hide zero plans
		 * @param lhideNoCost
		 *            hide no cost plans
		 */
		public final void startPlanQuery(final Calendar lnow,
				final boolean lhideZero, final boolean lhideNoCost) {
			// Cancel any pending queries
			this.queryHandler.cancelOperation(PLAN_QUERY_TOKEN);
			try {
				// Kick off the new query
				// FIXME
				// this.activity.setProgressBarIndeterminateVisibility(Boolean.TRUE);
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
										String.valueOf(lhideZero))
								.appendQueryParameter(
										DataProvider.Plans.PARAM_HIDE_NOCOST,
										String.valueOf(lhideNoCost)).build(),
						DataProvider.Plans.PROJECTION_SUM, null, null, null);
			} catch (SQLiteException e) {
				Log.e(TAG, "error starting query", e);
			}
		}

		/**
		 * Close inner {@link Cursor}.
		 */
		private void close() {
			final Cursor c = this.getCursor();
			if (c != null && !c.isClosed()) {
				c.close();
			}
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
		 * {@inheritDoc}
		 */
		@Override
		public final void bindView(final View view, final Context context,
				final Cursor cursor) {
			DataProvider.Plans.Plan plan = new DataProvider.Plans.Plan(cursor);

			String cacheStr = null;
			if (plan.type != DataProvider.TYPE_SPACING // .
					&& plan.type != DataProvider.TYPE_TITLE) {
				cacheStr = "<b>"
						+ formatValues(context, plan.now, plan.type,
								plan.bpCount, plan.bpBa, plan.billperiod,
								plan.billday) + "</b>";
				if (plan.type != DataProvider.TYPE_BILLPERIOD) {
					if (showTotal) {
						cacheStr += delimiter;
						cacheStr += formatValues(context, plan.now, plan.type,
								plan.atCount, plan.atBa, plan.billperiod,
								plan.billday);
					}
					if (showToday) {
						cacheStr = delimiter + cacheStr;
						cacheStr = formatValues(context, plan.now, plan.type,
								plan.tdCount, plan.tdBa, plan.billperiod,
								plan.billday)
								+ cacheStr;
					}
				}
				if (plan.cost > 0f) { // TODO: migrate "free"
					cacheStr += "\n" + String.format(currencyFormat, plan.cost);
				}
				if (plan.limit > 0) {
					cacheStr = ((int) plan.usage) + "%" + delimiter + cacheStr;
				}
			}

			Log.d(TAG, "plan id: " + plan.id);
			Log.d(TAG, "plan name: " + plan.name);
			Log.d(TAG, "type: " + plan.type);
			Log.d(TAG, "cost: " + plan.cost);
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
					if (plan.usage >= CallMeter.HUNDRET) {
						pbCache = (ProgressBar) view
								.findViewById(R.id.progressbarLimitRed);
						view.findViewById(R.id.progressbarLimitGreen)
								.setVisibility(View.GONE);
						view.findViewById(R.id.progressbarLimitYellow)
								.setVisibility(View.GONE);
					} else if (bpos >= 0f
							&& (float) plan.limitPos / plan.limit > bpos) {
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
	 * Return pretty bytes.
	 * 
	 * @author Cyril Jaquier, flx
	 * @param value
	 *            bytes
	 * @return pretty bytes
	 */
	public static final String prettyBytes(final float value) {
		StringBuilder sb = new StringBuilder();
		if (value < CallMeter.BYTE_KB) {
			sb.append(String.format("%.1f", value));
			sb.append(BYTE_UNITS_B);
		} else if (value < CallMeter.BYTE_MB) {
			sb.append(String.format("%.1f", value / CallMeter.BYTE_KB));
			sb.append(BYTE_UNITS_KB);
		} else if (value < CallMeter.BYTE_GB) {
			sb.append(String.format("%.2f", value / CallMeter.BYTE_MB));
			sb.append(BYTE_UNITS_MB);
		} else if (value < CallMeter.BYTE_TB) {
			sb.append(String.format("%.3f", value / CallMeter.BYTE_GB));
			sb.append(BYTE_UNITS_GB);
		} else {
			sb.append(String.format("%.4f", value / CallMeter.BYTE_TB));
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
	public static final String prettySeconds(final float seconds,
			final boolean showHours) {
		String ret;
		final long ls = (long) seconds;
		long d, h, m;
		if (showHours) {
			d = ls / CallMeter.SECONDS_DAY;
			h = (ls % CallMeter.SECONDS_DAY) / CallMeter.SECONDS_HOUR;
			m = (ls % CallMeter.SECONDS_HOUR) / CallMeter.SECONDS_MINUTE;
		} else {
			d = 0L;
			h = 0L;
			m = ls / CallMeter.SECONDS_MINUTE;
		}
		final long s = ls % CallMeter.SECONDS_MINUTE;
		if (d > 0L) {
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
			final float amount, final boolean showHours) {
		switch (pType) {
		case DataProvider.TYPE_DATA:
			return prettyBytes(amount);
		case DataProvider.TYPE_CALL:
			return prettySeconds(amount, showHours);
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_SMS:
			return String.valueOf((long) amount);
		default:
			return String.format("%.2f", amount).replaceAll("[\\.,]?0*$", "");
		}
	}

	/**
	 * Format bill periods start date.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param billperiod
	 *            bill period type
	 * @param billDay
	 *            {@link Calendar} for bill periods bill day
	 * @return formated date
	 */
	public static String formatDate(final Context context,
			final int billperiod, final Calendar billDay) {
		if (billperiod == DataProvider.BILLPERIOD_INFINITE) {
			if (billDay == null) {
				return "\u221E";
			} else {
				if (dateFormat == null) {
					return DateFormat.getDateFormat(context).format(
							billDay.getTime());
				} else {
					return String.format(dateFormat, billDay, billDay, billDay);
				}
			}
		} else {
			if (dateFormat == null) {
				return DateFormat.getDateFormat(context).format(
						billDay.getTime());
			} else {
				return String.format(dateFormat, billDay, billDay, billDay);
			}
		}
	}

	/**
	 * Format count/amount along with type.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param now
	 *            current time
	 * @param pType
	 *            type of plan
	 * @param count
	 *            count
	 * @param amount
	 *            amount
	 * @param billperiod
	 *            bill period type
	 * @param billday
	 *            bill period's bill day
	 * @return string
	 */
	public static String formatValues(final Context context, final long now,
			final int pType, final long count, final float amount,
			final int billperiod, final long billday) {
		switch (pType) {
		case DataProvider.TYPE_BILLPERIOD:
			Calendar billDay = Calendar.getInstance();
			billDay.setTimeInMillis(billday);
			return formatDate(context, billperiod, billDay);
		case DataProvider.TYPE_CALL:
			return formatAmount(pType, amount, pShowHours) + " (" + count + ")";
		case DataProvider.TYPE_DATA:
		case DataProvider.TYPE_SMS:
		case DataProvider.TYPE_MMS:
		case DataProvider.TYPE_MIXED:
			return formatAmount(pType, amount, pShowHours);
		default:
			return "";
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onDestroy() {
		super.onDestroy();
		this.adapter.close();
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
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final boolean showTitlebar = p.getBoolean(
				Preferences.PREFS_SHOWTITLEBAR, true);
		if (!showTitlebar) {
			this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		}
		this.setTheme(Preferences.getTheme(this));
		Utils.setLocale(this);
		this.setContentView(R.layout.plans);

		ChangelogHelper.showChangelog(this, false);
		ChangelogHelper.showNotes(this, false, null, null, null);

		prefsNoAds = DonationHelper.hideAds(this);
		this.findViewById(R.id.import_default).setOnClickListener(this);
		this.getListView().setOnItemLongClickListener(this);

		this.adapter = new PlanAdapter(this, Calendar.getInstance(), hideZero,
				hideNoCost);
		this.setListAdapter(this.adapter);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		Utils.setLocale(this);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		pShowHours = p.getBoolean(Preferences.PREFS_SHOWHOURS, true);
		currentHandler = this.handler;
		Plans.this.setProgressBarIndeterminateVisibility(inProgressMatcher);

		currencyFormat = Preferences.getCurrencyFormat(this);
		dateFormat = Preferences.getDateFormat(this);
		showToday = p.getBoolean(Preferences.PREFS_SHOWTODAY, false);
		showTotal = p.getBoolean(Preferences.PREFS_SHOWTOTAL, true);
		hideZero = p.getBoolean(Preferences.PREFS_HIDE_ZERO, false);
		hideNoCost = p.getBoolean(Preferences.PREFS_HIDE_NOCOST, false);
		delimiter = p.getString(Preferences.PREFS_DELIMITER, " | ");

		// FIXME if (reloadList) {
		// this.adapter = new PlanAdapter(this, Calendar.getInstance(),
		// hideZero, hideNoCost);
		// this.setListAdapter(this.adapter);
		// }

		if (this.getListView().getCount() == 0) {
			this.findViewById(R.id.import_default).setVisibility(View.VISIBLE);
		} else {
			this.findViewById(R.id.import_default).setVisibility(View.GONE);
		}
		// reload plan configuration
		Calendar now = this.getNowFromIntent(this.getIntent(), false);
		adapter.startPlanQuery(now, hideZero, hideNoCost);
		// start LogRunner
		LogRunnerService.update(this, LogRunnerReceiver.ACTION_FORCE_UPDATE);
		// schedule next update
		LogRunnerReceiver.schedNext(this);
		if (this.getListView().getCount() > 0 && !prefsNoAds) {
			Ads.loadAd(this, R.id.ad, AD_UNITID, AD_KEYWORDS);
		} else {
			this.findViewById(R.id.ad).setVisibility(View.GONE);
		}
	}

	/**
	 * Get "now" from {@link Intent}.
	 * 
	 * @param intent
	 *            {@link Intent}
	 * @param updatePlans
	 *            update plans after setting "now"
	 * @return "now"
	 */
	private Calendar getNowFromIntent(final Intent intent, // .
			final boolean updatePlans) {
		final long now = intent.getLongExtra(EXTRA_NOW, -1);
		if (now >= 0L) {
			this.setNow(now, updatePlans, false);
		} else {
			this.adapter.now = null;
			this.setTitle(R.string.app_name);
			if (updatePlans) {
				// FIXME: this.adapter.updatePlans();
			}
		}
		Calendar ret = Calendar.getInstance();
		if (now >= 0L) {
			ret.setTimeInMillis(now);
		}
		return ret;
	}

	/**
	 * Set "now" to main view.
	 * 
	 * @param now
	 *            now as milliseconds
	 * @param updatePlans
	 *            update plans after setting "now"
	 * @param fromTime
	 *            time set too?
	 */
	private void setNow(final long now, final boolean updatePlans,
			final boolean fromTime) {
		this.adapter.setNow(now);
		final Calendar cal = this.adapter.now;
		String formatedDate;
		if (dateFormat == null) {
			formatedDate = DateFormat.getDateFormat(this).format(now);
		} else {
			formatedDate = String.format(dateFormat, cal, cal, cal);
		}
		String t = this.getString(R.string.app_name) + " - " + formatedDate;
		t += String.format(" %tR", cal);
		this.setTitle(t);
		if (!fromTime) {
			this.showDialog(DIALOG_SHOW_TIME);
		}
		if (updatePlans) {
			// FIXME: this.adapter.updatePlans();
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
		// FIXME: reloadList = true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		switch (id) {
		case DIALOG_SHOW_DATE:
			Calendar cal = this.adapter.now;
			if (cal == null) {
				cal = Calendar.getInstance();
			}
			return new DatePickerDialog(this, this, cal.get(Calendar.YEAR),
					cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
		case DIALOG_SHOW_TIME:
			cal = this.adapter.now;
			int h;
			int m;
			if (cal == null) {
				h = 23;
				m = 59;
			} else {
				h = cal.get(Calendar.HOUR_OF_DAY);
				m = cal.get(Calendar.MINUTE);
				if (h == 0 && m == 0) {
					h = 23;
					m = 59;
				}
			}
			return new TimePickerDialog(this, this, h, m, true);
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
		inflater.inflate(R.menu.menu_main, menu);
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
			DonationHelper.startDonationActivity(this, false);
			return true;
		case R.id.item_logs:
			this.startActivity(new Intent(this, Logs.class));
			return true;
		case R.id.item_show_time:
			this.showDialog(DIALOG_SHOW_DATE);
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
		date.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
		final Intent intent = new Intent(this, Plans.class);
		intent.putExtra(EXTRA_NOW, date.getTimeInMillis());
		if (this.adapter.now != null) {
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		}
		this.startActivity(intent);
	}

	/**
	 * {@inheritDoc}
	 */
	public final void onTimeSet(final TimePicker view, final int hourOfDay,
			final int minute) {
		Calendar cal = this.adapter.now;
		if (cal == null) {
			cal = Calendar.getInstance();
		}
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
				cal.get(Calendar.DAY_OF_MONTH), hourOfDay, minute);
		this.setNow(cal.getTimeInMillis(), true, true);
	}
}
