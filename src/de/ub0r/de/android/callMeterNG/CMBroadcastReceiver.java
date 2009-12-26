/*
 * Copyright (C) 2009 Felix Bechstein
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

package de.ub0r.de.android.callMeterNG;

import java.io.IOException;
import java.util.Calendar;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * ProxyStarter listens to any Broadcast. It'l start the Proxy Service on
 * receive.
 * 
 * @author Felix Bechstein
 */
public class CMBroadcastReceiver extends BroadcastReceiver {
	/** Tag for output. */
	private static final String TAG = "CallMeterNG.Receiver";

	/** Time between to update checks. */
	private static final long DELAY = 1 * 60 * 1000; // 1min

	// private static final long DELAY = 30 * 60 * 1000; // 30min

	/**
	 * Update traffic data.
	 * 
	 * @param context
	 *            Context
	 * @param prefs
	 *            preferences
	 */
	static final synchronized void updateTraffic(final Context context,
			final SharedPreferences prefs) {
		checkBillperiod(prefs);

		long runningIn = prefs.getLong(CallMeter.PREFS_DATA_RUNNING_IN, 0);
		long runningOut = prefs.getLong(CallMeter.PREFS_DATA_RUNNING_OUT, 0);

		final Device d = Device.getDevice();
		final String inter = d.getCell();
		if (inter != null) {
			try {
				final long rx = SysClassNet.getRxBytes(inter);
				final long tx = SysClassNet.getTxBytes(inter);
				runningIn = rx;
				runningOut = tx;
			} catch (IOException e) {
				Log.e(TAG, "I/O Error", e);
			}
		}

		final SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(CallMeter.PREFS_DATA_RUNNING_IN, runningIn);
		editor.putLong(CallMeter.PREFS_DATA_RUNNING_OUT, runningOut);
		editor.commit();

		// schedule next update
		final AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);

		final Intent i = new Intent(context, CMBroadcastReceiver.class);
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		final long t = SystemClock.elapsedRealtime() + DELAY;
		mgr.set(AlarmManager.ELAPSED_REALTIME, t, pi);
	}

	/**
	 * Check if billing period changed.
	 * 
	 * @param prefs
	 *            preferences
	 */
	static final void checkBillperiod(final SharedPreferences prefs) {
		final Calendar billDate = CallMeter.getBillDate(Integer.parseInt(prefs
				.getString(CallMeter.PREFS_BILLDAY, "0")));
		long lastBill = billDate.getTimeInMillis();
		long now = System.currentTimeMillis();
		long lastCheck = prefs.getLong(CallMeter.PREFS_DATA_LASTCHECK, 0);

		final SharedPreferences.Editor editor = prefs.edit();
		if (lastCheck < lastBill) {
			long preBootIn = prefs.getLong(CallMeter.PREFS_DATA_BOOT_IN, 0);
			long preBootOut = prefs.getLong(CallMeter.PREFS_DATA_BOOT_OUT, 0);
			long runningIn = prefs.getLong(CallMeter.PREFS_DATA_RUNNING_IN, 0);
			long runningOut = prefs
					.getLong(CallMeter.PREFS_DATA_RUNNING_OUT, 0);
			editor.putLong(CallMeter.PREFS_DATA_PREBILLING_IN, preBootIn
					+ runningIn);
			editor.putLong(CallMeter.PREFS_DATA_PREBILLING_OUT, preBootOut
					+ runningOut);
		}
		editor.putLong(CallMeter.PREFS_DATA_LASTCHECK, now);
		editor.commit();
	}

	/**
	 * Move traffic from thisboot to preboot.
	 * 
	 * @param prefs
	 *            preferences
	 */
	static final void checkPostboot(final SharedPreferences prefs) {
		long preBootIn = prefs.getLong(CallMeter.PREFS_DATA_BOOT_IN, 0);
		long preBootOut = prefs.getLong(CallMeter.PREFS_DATA_BOOT_OUT, 0);
		long runningIn = prefs.getLong(CallMeter.PREFS_DATA_RUNNING_IN, 0);
		long runningOut = prefs.getLong(CallMeter.PREFS_DATA_RUNNING_OUT, 0);

		if (runningIn == 0 && runningOut == 0) {
			return;
		}

		preBootIn += runningIn;
		runningIn = 0;
		preBootOut += runningOut;
		runningOut = 0;

		final SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(CallMeter.PREFS_DATA_BOOT_IN, preBootIn);
		editor.putLong(CallMeter.PREFS_DATA_BOOT_OUT, preBootOut);
		editor.putLong(CallMeter.PREFS_DATA_RUNNING_IN, runningIn);
		editor.putLong(CallMeter.PREFS_DATA_RUNNING_OUT, runningOut);
		editor.commit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onReceive(final Context context, final Intent intent) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String action = intent.getAction();
		if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			checkPostboot(prefs);
		}
		updateTraffic(context, prefs);
	}
}
