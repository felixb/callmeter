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

package de.ub0r.de.android.callMeterNG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import de.ub0r.android.lib.Log;

/**
 * {@link BroadcastReceiver} running updates and postboot checks.
 * 
 * @author Felix Bechstein
 */
public class CMBroadcastReceiver extends BroadcastReceiver {
	/** Tag for output. */
	private static final String TAG = "receiver";

	/** Time between to update checks. */
	private static final long DELAY = 30 * 60 * 1000; // 30min

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	static final void schedNext(final Context context) {
		final Intent i = new Intent(context, CMBroadcastReceiver.class);
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
	public final void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "wakeup");
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(context);
		final String action = intent.getAction();
		if (action != null && action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			UpdaterData.checkPostboot(prefs);
		}
		// run update
		new Updater(context).execute((Void) null);
		UpdaterData.updateTraffic(context, prefs);
		// schedule next update
		CMBroadcastReceiver.schedNext(context);
	}
}
