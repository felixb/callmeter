/*
 * Copyright (C) 2009-2010 Felix Bechstein
 * 
 * This file is part of CallMeter 3G.
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
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import de.ub0r.android.lib.Log;

/**
 * {@link BroadcastReceiver} running updates and postboot checks.
 * 
 * @author Felix Bechstein
 */
public final class LogRunnerReceiver extends BroadcastReceiver {
	/** Tag for output. */
	private static final String TAG = "lrr";

	/** Time between to update checks. */
	private static final long DELAY = 30 * 60 * 1000; // 30min

	/** Force update. */
	public static final String ACTION_FORCE_UPDATE = // .
	"de.ub0r.android.callmeter.FORCE_UPDATE";

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void schedNext(final Context context) {
		final Intent i = new Intent(context, LogRunnerReceiver.class);
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
		// run LogRunnerService
		LogRunnerService.update(context, intent.getAction());
		// schedule next update
		LogRunnerReceiver.schedNext(context);
	}
}
