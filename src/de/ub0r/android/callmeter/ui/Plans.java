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

import java.util.Calendar;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.LogRunnerReceiver;
import de.ub0r.android.callmeter.data.LogRunnerService;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.DonationHelper;
import de.ub0r.android.lib.Log;

/**
 * Callmeter's Main {@link ListActivity}.
 * 
 * @author flx
 */
public class Plans extends ListActivity {
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
	/** Bytes: kB. */
	private static final long BYTE_KB = 1024L;
	/** Bytes: MB. */
	private static final long BYTE_MB = BYTE_KB * BYTE_KB;
	/** Bytes: GB. */
	private static final long BYTE_GB = BYTE_MB * BYTE_KB;
	/** Bytes: TB. */
	private static final long BYTE_TB = BYTE_GB * BYTE_KB;

	/** Dialog: update. */
	private static final int DIALOG_UPDATE = 0;

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";

	/**
	 * Adapter binding plans to View.
	 * 
	 * @author flx
	 */
	public static class PlanAdapter extends ResourceCursorAdapter {
		/** Now. */
		private final Calendar now;
		/** Separator for the data. */
		private static final String SEP = " | ";

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

			this.now = Calendar.getInstance();
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
				return (int) (amount / BYTE_KB);
			default:
				return (int) amount;
			}
		}

		/**
		 * Calculate limit set for given plan.
		 * 
		 * @param pType
		 *            type of plan
		 * @param limit
		 *            limit
		 * @return set limit
		 */
		private static int getLimit(final int pType, final int limit) {
			switch (pType) {
			case DataProvider.TYPE_DATA:
				return limit * (int) BYTE_KB;
			case DataProvider.TYPE_CALL:
				return limit * CallMeter.SECONDS_MINUTE;
			default:
				return limit;
			}
		}

		/**
		 * Get a {@link String} showing all the data to the user.
		 * 
		 * @param pType
		 *            type of plan
		 * @param lType
		 *            type of limit
		 * @param limit
		 *            limit
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
		private String getString(final int pType, final int lType,
				final int limit, final int used, final long count,
				final long amount, final float cost, final long allAmount,
				final long allCount) {
			final StringBuilder ret = new StringBuilder();

			// usage
			if (lType != DataProvider.LIMIT_TYPE_NONE && limit > 0) {
				ret.append((used * CallMeter.HUNDRET) / limit);
				ret.append("%");
				ret.append(SEP);
			}

			// amount
			ret.append(formatAmount(pType, amount));
			// count
			if (pType == DataProvider.TYPE_CALL) {
				ret.append(" (" + count + ")");
			}
			ret.append(SEP);

			// amount all time
			ret.append(formatAmount(pType, allAmount));
			// count all time
			if (pType == DataProvider.TYPE_CALL) {
				ret.append(" (" + allCount + ")");
			}

			// cost
			if (cost >= 0) {
				ret.append(String.format("\n%." + CallMeter.currencyDigits
						+ "f" + CallMeter.currencySymbol, cost));
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
			final int billPeriod = cursor
					.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
			Calendar billDay = Calendar.getInstance();
			billDay.setTimeInMillis(cursor
					.getLong(DataProvider.Plans.INDEX_BILLDAY));
			billDay = DataProvider.Plans.getBillDay(billPeriod, billDay, null,
					false);
			if (t == DataProvider.TYPE_SPACING) {
				view.findViewById(R.id.bigtitle).setVisibility(View.INVISIBLE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (t == DataProvider.TYPE_TITLE) {
				final TextView tw = ((TextView) view
						.findViewById(R.id.bigtitle));
				tw.setText(cursor.getString(DataProvider.Plans.INDEX_NAME));
				tw.setVisibility(View.VISIBLE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
			} else if (t == DataProvider.TYPE_BILLPERIOD) {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.GONE);
				view.findViewById(R.id.period_layout).setVisibility(
						View.VISIBLE);
				final ProgressBar pb = (ProgressBar) view
						.findViewById(R.id.period_pb);
				if (billPeriod == DataProvider.BILLPERIOD_INFINITE) {
					pb.setIndeterminate(true);
					((TextView) view.findViewById(R.id.period))
							.setText("\u221E");
				} else {
					pb.setIndeterminate(false);
					final Calendar nextBillDay = DataProvider.Plans.getBillDay(
							billPeriod, billDay, null, true);
					long pr = billDay.getTimeInMillis() / CallMeter.MILLIS;
					long nx = (nextBillDay.getTimeInMillis() / CallMeter.MILLIS)
							- pr;
					long nw = (this.now.getTimeInMillis() / CallMeter.MILLIS)
							- pr;
					pb.setMax((int) nx);
					pb.setProgress((int) nw);
					((TextView) view.findViewById(R.id.period))
							.setText(DateFormat.getDateFormat(context).format(
									billDay.getTime()));
				}
			} else {
				view.findViewById(R.id.bigtitle).setVisibility(View.GONE);
				view.findViewById(R.id.content).setVisibility(View.VISIBLE);
				view.findViewById(R.id.period_layout).setVisibility(View.GONE);
				((TextView) view.findViewById(R.id.normtitle)).setText(cursor
						.getString(DataProvider.Plans.INDEX_NAME));
				final int limitType = cursor
						.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
				ProgressBar pb = null;
				int limit = 0;
				int used = 0;
				switch (limitType) {
				case DataProvider.LIMIT_TYPE_UNITS:
					limit = getLimit(t, cursor
							.getInt(DataProvider.Plans.INDEX_LIMIT));
					break;
				case DataProvider.LIMIT_TYPE_COST:
					limit = cursor.getInt(DataProvider.Plans.INDEX_LIMIT)
							* CallMeter.HUNDRET;
					break;
				default:
					limit = -1;
					break;
				}
				if (limit > 0) {
					pb = (ProgressBar) view.findViewById(R.id.progressbarLimit);
					pb.setMax(limit);
					pb.setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.progressbarLimit).setVisibility(
							View.GONE);
				}
				final long pid = cursor.getLong(DataProvider.Plans.INDEX_ID);
				long p = cursor.getLong(DataProvider.Plans.INDEX_BILLPERIOD);
				final Calendar ps = Calendar.getInstance();
				Cursor c = context.getContentResolver().query(
						ContentUris.withAppendedId(
								DataProvider.Plans.CONTENT_URI, p),
						DataProvider.Plans.PROJECTION, null, null, null);
				if (c != null && c.moveToFirst()) {
					ps.setTimeInMillis(c
							.getLong(DataProvider.Plans.INDEX_BILLDAY));
					p = c.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
				}
				if (c != null && !c.isClosed()) {
					c.close();
				}
				final String where = DbUtils.sqlAnd(DataProvider.Plans
						.getBilldayWhere((int) p, ps, this.now),
						DataProvider.Logs.PLAN_ID + " = " + pid);
				Log.d(TAG, "where: " + where);

				float cost = -1;
				long billedAmount = 0;
				int count = 0;
				long allBilledAmount = 0;
				int allCount = 0;

				c = context.getContentResolver().query(
						DataProvider.Logs.SUM_URI,
						DataProvider.Logs.PROJECTION_SUM, where, null, null);
				if (c != null && c.moveToFirst()) {
					cost = c.getFloat(DataProvider.Logs.INDEX_SUM_COST);
					billedAmount = c
							.getLong(DataProvider.Logs.INDEX_SUM_BILL_AMOUNT);
					count = c.getInt(DataProvider.Logs.INDEX_SUM_COUNT);

					Log.d(TAG, "plan: " + pid);
					Log.d(TAG, "count: " + count);
					Log.d(TAG, "cost: " + cost);
					Log.d(TAG, "billedAmount: " + billedAmount);

					switch (limitType) {
					case DataProvider.LIMIT_TYPE_COST:
						used = (int) (cost * CallMeter.HUNDRET);
						break;
					case DataProvider.LIMIT_TYPE_UNITS:
						used = getUsed(t, billedAmount);
						break;
					default:
						used = 0;
						break;
					}

					if (pb != null) {
						pb.setProgress(used);
					}
				}
				if (c != null && !c.isClosed()) {
					c.close();
				}
				// get all time
				c = context.getContentResolver().query(
						DataProvider.Logs.SUM_URI,
						DataProvider.Logs.PROJECTION_SUM,
						DataProvider.Logs.PLAN_ID + " = " + pid, null, null);
				if (c != null && c.moveToFirst()) {
					allBilledAmount = c
							.getLong(DataProvider.Logs.INDEX_SUM_BILL_AMOUNT);
					allCount = c.getInt(DataProvider.Logs.INDEX_SUM_COUNT);
				}
				if (c != null && !c.isClosed()) {
					c.close();
				}

				final float costPerPlan = // .
				cursor.getFloat(DataProvider.Plans.INDEX_COST_PER_PLAN);
				if (costPerPlan > 0) {
					if (cost <= 0) {
						cost = costPerPlan;
					} else {
						cost += costPerPlan;
					}
				}

				((TextView) view.findViewById(R.id.data)).setText(this
						.getString(t, limitType, limit, used, count,
								billedAmount, cost, allBilledAmount, allCount));
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
		if (value < BYTE_KB) {
			sb.append(String.valueOf(value));
			sb.append(BYTE_UNITS_B);
		} else if (value < BYTE_MB) {
			sb.append(String.format("%.1f", value / (BYTE_KB * 1.0)));
			sb.append(BYTE_UNITS_KB);
		} else if (value < BYTE_GB) {
			sb.append(String.format("%.2f", value / (BYTE_MB * 1.0)));
			sb.append(BYTE_UNITS_MB);
		} else if (value < BYTE_TB) {
			sb.append(String.format("%.3f", value / (BYTE_GB * 1.0)));
			sb.append(BYTE_UNITS_GB);
		} else {
			sb.append(String.format("%.4f", value / (BYTE_TB * 1.0)));
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
		int d = (int) (seconds / 86400);
		int h = (int) ((seconds % 86400) / 3600);
		int m = (int) ((seconds % 3600) / 60);
		int s = (int) (seconds % 60);
		if (d > 0) {
			ret = d + "d ";
		} else {
			ret = "";
		}
		if (h > 0 || d > 0) {
			if (h < 10) {
				ret += "0";
			}
			ret += h + ":";
		}
		if (m > 0 || h > 0 || d > 0) {
			if (m < 10 && h > 0) {
				ret += "0";
			}
			ret += m + ":";
		}
		if (s < 10 && (m > 0 || h > 0 || d > 0)) {
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
	 * Round up time with bill mode in mind.
	 * 
	 * @param time
	 *            time
	 * @param firstLength
	 *            length of minimal billed call
	 * @param nextLength
	 *            length of following time slots billed
	 * @return rounded time
	 */
	public static final int roundTime(final int time, final int firstLength,
			final int nextLength) {
		// 0 => 0
		if (time == 0) {
			return 0;
		}
		// !0 ..
		if (time <= firstLength) { // round first slot
			return firstLength;
		}
		if (nextLength == 0) {
			return firstLength;
		}
		if (time % nextLength == 0 || nextLength == 1) {
			return time;
		}
		// round up to next full slot
		return ((time / nextLength) + 1) * nextLength;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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

		// TextView tv = (TextView) this.findViewById(R.id.calls_);
		// Preferences.textSizeMedium = tv.getTextSize();
		// tv = (TextView) this.findViewById(R.id.calls1_in_);
		// Preferences.textSizeSmall = tv.getTextSize();

		// final ListView list = this.getListView();
		this.adapter = new PlanAdapter(this);
		this.setListAdapter(this.adapter);
		// list.setOnItemClickListener(this);
		// list.setOnItemLongClickListener(this);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		if (!prefsNoAds) {
			this.findViewById(R.id.ad).setVisibility(View.VISIBLE);
		}
		// start LogRunner
		LogRunnerService.update(this);
		// schedule next update
		LogRunnerReceiver.schedNext(this);
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

			buf.append(this.getString(R.string.see_about));

			for (int i = 0; i < changes.length; i++) {
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
}
