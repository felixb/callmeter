/*
 * Copyright (C) 2010 Felix Bechstein
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
package de.ub0r.android.callmeter.widget;

import java.util.Calendar;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RemoteViews;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.Plans;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;

/**
 * Stats Widget.
 * 
 * @author flx
 */
public final class StatsAppWidgetProvider extends AppWidgetProvider {
	/** Tag for debug out. */
	private static final String TAG = "wdgt";

	/** Plan.id for widget. */
	static final String WIDGET_PLANID = "widget_planid_";
	/** Show short name for widget. */
	static final String WIDGET_SHORTNAME = "widget_shortname_";
	/** Show cost for widget. */
	static final String WIDGET_COST = "widget_cost_";
	/** Show progress of billing period for widget. */
	static final String WIDGET_BILLPERIOD = "widget_billp_";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onUpdate(final Context context,
			final AppWidgetManager appWidgetManager, final int[] appWidgetIds) {
		final int count = appWidgetIds.length;

		// Perform this loop procedure for each App Widget that belongs to this
		// provider
		for (int i = 0; i < count; i++) {
			updateWidget(context, appWidgetManager, appWidgetIds[i]);
		}
	}

	/**
	 * Update all running widgets.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void updateWidgets(final Context context) {
		final AppWidgetManager appWidgetManager = AppWidgetManager
				.getInstance(context);
		final int[] appWidgetIds = appWidgetManager
				.getAppWidgetIds(new ComponentName(context,
						StatsAppWidgetProvider.class));
		final int count = appWidgetIds.length;
		for (int i = 0; i < count; i++) {
			updateWidget(context, appWidgetManager, appWidgetIds[i]);
		}
	}

	/**
	 * Update a single widget.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param appWidgetManager
	 *            {@link AppWidgetManager}
	 * @param appWidgetId
	 *            id of widget
	 */
	static void updateWidget(final Context context,
			final AppWidgetManager appWidgetManager, final int appWidgetId) {
		SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final long pid = p.getLong(WIDGET_PLANID + appWidgetId, -1L);
		final boolean showShortname = p.getBoolean(WIDGET_SHORTNAME
				+ appWidgetId, false);
		final boolean showCost = p.getBoolean(WIDGET_COST + appWidgetId, false);
		final boolean showBillPeriod = p.getBoolean(WIDGET_BILLPERIOD
				+ appWidgetId, false);
		Log.d(TAG, "planid: " + pid);
		ContentResolver cr = context.getContentResolver();

		if (pid < 0L) {
			return;
		}
		final long ppid = DataProvider.Plans.getParent(cr, pid);
		final Uri puri = ContentUris.withAppendedId(
				DataProvider.Plans.CONTENT_URI, pid);
		long bid = -1L;
		String pname = null;
		float cpp = 0F;
		int ltype = DataProvider.LIMIT_TYPE_NONE;
		long limit = 0L;
		int ptype = -1;
		String where;
		int upc, upm, ups;
		boolean isMerger;
		// float cost = 0F;
		String billdayWhere = null;
		Cursor cursor = cr.query(puri, DataProvider.Plans.PROJECTION, null,
				null, null);
		if (cursor != null && cursor.moveToFirst()) {
			if (showShortname) {
				pname = cursor.getString(DataProvider.Plans.INDEX_SHORTNAME);
			} else {
				pname = cursor.getString(DataProvider.Plans.INDEX_NAME);
			}
			ptype = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
			bid = cursor.getLong(DataProvider.Plans.INDEX_BILLPERIOD_ID);
			ltype = cursor.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			limit = DataProvider.Plans.getLimit(ptype, ltype, cursor
					.getLong(DataProvider.Plans.INDEX_LIMIT));
			upc = cursor.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_CALL);
			upm = cursor.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_MMS);
			ups = cursor.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_SMS);
			cpp = cursor.getFloat(DataProvider.Plans.INDEX_COST_PER_PLAN);

			final String s = cursor
					.getString(DataProvider.Plans.INDEX_MERGED_PLANS);

			if (s == null || s.length() == 0) {
				where = DataProvider.Logs.PLAN_ID + " = " + pid;
				isMerger = false;
			} else {
				StringBuilder sb = new StringBuilder(DataProvider.Logs.PLAN_ID
						+ " = " + pid);
				for (String ss : s.split(",")) {
					if (ss.length() == 0) {
						continue;
					}
					sb.append(" OR " + DataProvider.Logs.PLAN_ID + " = " + ss);
				}
				where = sb.toString();
				isMerger = true;
			}
		} else {
			return;
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		int bpos = 0;
		int bmax = -1;
		if (bid >= 0L) {
			cursor = cr.query(ContentUris.withAppendedId(
					DataProvider.Plans.CONTENT_URI, bid),
					DataProvider.Plans.PROJECTION, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				final int bp = cursor
						.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
				final long bday = cursor
						.getLong(DataProvider.Plans.INDEX_BILLDAY);
				billdayWhere = DataProvider.Plans.getBilldayWhere(bp, bday,
						null);
				if (showBillPeriod && bp != DataProvider.BILLPERIOD_INFINITE) {
					Calendar billDay = Calendar.getInstance();
					billDay.setTimeInMillis(bday);
					billDay = DataProvider.Plans.getBillDay(bp, billDay, null,
							false);
					final Calendar nextBillDay = DataProvider.Plans.getBillDay(
							bp, billDay, null, true);

					final long pr = billDay.getTimeInMillis()
							/ CallMeter.MILLIS;
					final long nx = (nextBillDay.getTimeInMillis() // .
							/ CallMeter.MILLIS)
							- pr;
					long nw = System.currentTimeMillis();
					nw = (nw / CallMeter.MILLIS) - pr;

					bmax = (int) nx;
					bpos = (int) nw;
				}
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
		}
		Log.d(TAG, "bpos/bmax: " + bpos + "/" + bmax);
		billdayWhere = DbUtils.sqlAnd(billdayWhere, where);
		cursor = cr.query(DataProvider.Logs.SUM_URI,
				DataProvider.Logs.PROJECTION_SUM, billdayWhere, null, null);
		float cost = cpp;
		long billedAmount = 0;
		int count = 0;
		int used = 0;
		if (cursor == null || !cursor.moveToFirst()) {
			used = 0;
		} else {
			do {
				cost += cursor.getFloat(DataProvider.Logs.INDEX_SUM_COST);
				long ba = cursor.getLong(// .
						DataProvider.Logs.INDEX_SUM_BILL_AMOUNT);
				if (isMerger && ptype == DataProvider.TYPE_MIXED) {
					final int t = cursor
							.getInt(DataProvider.Logs.INDEX_SUM_TYPE);
					switch (t) {
					case DataProvider.TYPE_CALL:
						ba = (ba * upc) / CallMeter.SECONDS_MINUTE;
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
				billedAmount += ba;
				count += cursor.getInt(DataProvider.Logs.INDEX_SUM_COUNT);
			} while (cursor.moveToNext());

			Log.d(TAG, "plan: " + pid);
			Log.d(TAG, "count: " + count);
			Log.d(TAG, "cost: " + cost);
			Log.d(TAG, "billedAmount: " + billedAmount);

			used = DataProvider.Plans.getUsed(ptype, ltype, billedAmount, cost);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		if (ppid >= 0L) {
			cost = 0F;
		}

		String stats = Plans.formatAmount(ptype, billedAmount,
				PreferenceManager.getDefaultSharedPreferences(context)
						.getBoolean(Preferences.PREFS_SHOWHOURS, true));
		if (ptype == DataProvider.TYPE_CALL) {
			stats += " (" + count + ")";
		}
		if (limit > 0) {
			stats += "\n" + (used * CallMeter.HUNDRET / limit) + "%";
		}
		if (showCost && cost > 0F) {
			stats += "\n"
					+ String.format(Preferences.getCurrencyFormat(context),
							cost);
		}

		Log.d(TAG, "limit: " + limit);
		Log.d(TAG, "used: " + used);
		Log.d(TAG, "stats: " + stats);

		// Create an Intent to launch Plans
		Intent intent = new Intent(context, Plans.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
				intent, 0);

		// Get the layout for the App Widget and attach an on-click listener
		// to the button
		RemoteViews views = new RemoteViews(context.getPackageName(),
				R.layout.stats_appwidget);
		views.setOnClickPendingIntent(R.id.widget, pendingIntent);
		views.setTextViewText(R.id.plan, pname);
		views.setTextViewText(R.id.stats, stats);
		if (bmax > 0) {
			views.setProgressBar(R.id.appwidget_bg_top_progress, bmax, bpos,
					false);
		} else {
			views.setViewVisibility(R.id.appwidget_bg_top_bg, View.GONE);
			views.setProgressBar(R.id.appwidget_bg_top_progress, 1, 0, false);
		}
		if (limit > 0) {
			views.setViewVisibility(R.id.appwidget_bg_bottom_bg, View.VISIBLE);
			final int u = (int) ((used * CallMeter.HUNDRET) / limit);
			if (u > CallMeter.HUNDRET) {
				views.setProgressBar(R.id.appwidget_bg_bottom_progress_red,
						(int) limit, used, false);
			} else if (u > CallMeter.EIGHTY) {
				views.setProgressBar(R.id.appwidget_bg_bottom_progress_red,
						(int) limit, 0, false);
				views.setProgressBar(R.id.appwidget_bg_bottom_progress_yellow,
						(int) limit, used, false);
			} else {
				views.setProgressBar(R.id.appwidget_bg_bottom_progress_red,
						(int) limit, 0, false);
				views.setProgressBar(R.id.appwidget_bg_bottom_progress_yellow,
						(int) limit, 0, false);
				views.setProgressBar(R.id.appwidget_bg_bottom_progress_green,
						(int) limit, used, false);
			}
		} else {
			views.setViewVisibility(R.id.appwidget_bg_bottom_bg, View.GONE);
			views.setProgressBar(R.id.appwidget_bg_bottom_progress_green, 1, 0,
					false);
			views.setProgressBar(R.id.appwidget_bg_bottom_progress_yellow, 1,
					0, false);
			views.setProgressBar(R.id.appwidget_bg_bottom_progress_red, 1, 0,
					false);
		}

		// Tell the AppWidgetManager to perform an update on the current App
		// Widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}
