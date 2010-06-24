/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of CallMeter NG.
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

package de.ub0r.android.callmeter.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import de.ub0r.android.callmeter.ui.Plans;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.TelephonyWrapper;
import de.ub0r.de.android.callMeterNG.UpdaterData;

/**
 * {@link BroadcastReceiver} running updates and postboot checks.
 * 
 * @author Felix Bechstein
 */
public final class LogRunner extends BroadcastReceiver {
	/** Tag for output. */
	private static final String TAG = "receiver";

	/** Time between to update checks. */
	private static final long DELAY = 30 * 60 * 1000; // 30min

	/** Is phone roaming? */
	private static boolean roaming = false;

	/** Plans Activity to show work in progress. */
	private static Plans mainActivity = null;

	/**
	 * Get maximum date of logs type %.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param type
	 *            type
	 * @return maximum date found. -1 if nothing was found.
	 */
	private static long getMaxDate(final ContentResolver cr, final int type) {
		final Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI,
				new String[] { DataProvider.Logs.DATE }, DataProvider.Logs.TYPE
						+ " = " + type, null, DataProvider.Logs.DATE + " DESC");
		long maxdate = -1;
		if (cursor != null && cursor.moveToFirst()) {
			maxdate = cursor.getLong(0);
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return maxdate;
	}

	/**
	 * Run logs: data.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private static void updateData(final ContentResolver cr) {

	}

	/**
	 * Run logs: calls.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private static void updateCalls(final ContentResolver cr) {

	}

	/**
	 * Run logs: sms.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private static void updateSMS(final ContentResolver cr) {
		final long maxdate = getMaxDate(cr, DataProvider.TYPE_SMS);
		final Uri smsUri = Uri.parse("content://sms/");
		final String[] smsProjection = new String[] { Calls.DATE, Calls.TYPE,
				"address", "body" };
		final Cursor cursor = cr.query(smsUri, smsProjection, Calls.DATE
				+ " > " + maxdate, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			final int idDate = cursor.getColumnIndex(Calls.DATE);
			final int idType = cursor.getColumnIndex(Calls.TYPE);
			final int idAddress = cursor.getColumnIndex("address");
			final int idBody = cursor.getColumnIndex("body");
			final TelephonyWrapper wrapper = TelephonyWrapper.getInstance();

			do {
				final ContentValues cv = new ContentValues();
				final int t = cursor.getInt(idType);
				if (t == Calls.INCOMING_TYPE) {
					cv.put(DataProvider.Logs.DIRECTION,
							DataProvider.DIRECTION_IN);
				} else if (t == Calls.OUTGOING_TYPE) {
					cv.put(DataProvider.Logs.DIRECTION,
							DataProvider.DIRECTION_OUT);
				} else {
					continue;
				}
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_SMS);
				cv.put(DataProvider.Logs.DATE, cursor.getLong(idDate));
				cv.put(DataProvider.Logs.REMOTE, cursor.getLong(idAddress));
				cv.put(DataProvider.Logs.AMOUNT, wrapper.calculateLength(cursor
						.getString(idBody), false)[0]);
				if (roaming) {
					cv.put(DataProvider.Logs.ROAMED, true);
				}
				cr.insert(DataProvider.Logs.CONTENT_URI, cv);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	/**
	 * Run logs: mms.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private static void updateMMS(final ContentResolver cr) {

	}

	/**
	 * Run logs.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	public static synchronized void update(final ContentResolver cr) {
		// TODO: move to async task
		if (mainActivity != null) {
			mainActivity.setProgressBarIndeterminateVisibility(true);
		}
		updateData(cr);
		updateCalls(cr);
		updateSMS(cr);
		updateMMS(cr);
		if (mainActivity != null) {
			mainActivity.setProgressBarIndeterminateVisibility(false);
		}
	}

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void schedNext(final Context context) {
		final Intent i = new Intent(context, LogRunner.class);
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		final long t = SystemClock.elapsedRealtime() + DELAY;
		final AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.ELAPSED_REALTIME, t, pi);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "wakeup");
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String action = intent.getAction();
		if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			UpdaterData.checkPostboot(prefs);
		}
		// update roaming info
		roaming = ((TelephonyManager) context
				.getSystemService(Context.TELEPHONY_SERVICE))
				.isNetworkRoaming();
		// run update
		update(context.getContentResolver());
		// new Updater(context).execute((Void) null);
		// UpdaterData.updateTraffic(context, prefs);
		// schedule next update
		LogRunner.schedNext(context);
	}

	/**
	 * Returns true if the device is considered roaming on the current network,
	 * for GSM purposes. Availability: Only when user registered to a network.
	 * 
	 * @return true if the device is considered roaming
	 */
	public static boolean isRoaming() {
		return roaming;
	}

	/**
	 * Register Plans Activity.
	 * 
	 * @param activity
	 *            Plans Activity
	 */
	public static void registerMain(final Plans activity) {
		mainActivity = activity;
	}

	/**
	 * Unegister Plans Activity.
	 * 
	 * @param activity
	 *            Plans Activity
	 */
	public static void unregisterMain(final Plans activity) {
		mainActivity = null;
	}
}
