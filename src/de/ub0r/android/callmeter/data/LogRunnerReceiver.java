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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * {@link BroadcastReceiver} running updates and postboot checks.
 * 
 * @author Felix Bechstein
 */
public final class LogRunnerReceiver extends BroadcastReceiver {
	/** Tag for output. */
	private static final String TAG = "lrr";

	/** Time between to update checks. */
	static final long DELAY = 30; // 30min
	/** Factor for time between update checks. */
	static final long DELAY_FACTOR = CallMeter.SECONDS_MINUTE
			* CallMeter.MILLIS;

	/** Force update. */
	public static final String ACTION_FORCE_UPDATE = // .
	"de.ub0r.android.callmeter.FORCE_UPDATE";

	/** ACTION for publishing information about sent websms. */
	private static final String ACTION_CM_WEBSMS = // .
	"de.ub0r.android.callmeter.SAVE_WEBSMS";
	/** Extra holding uri of sent sms. */
	private static final String EXTRA_WEBSMS_URI = "uri";
	/** Extra holding name of connector. */
	private static final String EXTRA_WEBSMS_CONNECTOR = "connector";

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void schedNext(final Context context) {
		final Intent i = new Intent(context, LogRunnerReceiver.class);
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		final long t = SystemClock.elapsedRealtime()
				+ Utils.parseLong(PreferenceManager
						.getDefaultSharedPreferences(context).getString(
								Preferences.PREFS_UPDATE_INTERVAL,
								String.valueOf(DELAY)), DELAY) * DELAY_FACTOR;
		final AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.ELAPSED_REALTIME, t, pi);
	}

	/**
	 * Save sent WebSMS to db.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param mid
	 *            message id
	 * @param connector
	 *            connector name
	 */
	private void saveWebSMS(final Context context, final long mid,
			final String connector) {
		final ContentValues cv = new ContentValues(2);
		cv.put(DataProvider.WebSMS.ID, mid);
		cv.put(DataProvider.WebSMS.CONNECTOR, connector);
		context.getContentResolver()
				.insert(DataProvider.WebSMS.CONTENT_URI, cv);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "wakeup");
		final String a = intent.getAction();
		Log.d(TAG, "action: " + a);
		if (a != null && a.equals(ACTION_CM_WEBSMS)) {
			final String su = intent.getStringExtra(EXTRA_WEBSMS_URI);
			if (su != null && su.length() > 0) {
				final long si = Utils.parseLong(su.replaceAll(".*/", ""), -1);
				final String sc = intent.getStringExtra(EXTRA_WEBSMS_CONNECTOR);
				Log.d(TAG, "websms id:  " + si);
				Log.d(TAG, "websms con: " + sc);
				if (si >= 0) {
					this.saveWebSMS(context, si, sc);
				}
			}
		}
		// run LogRunnerService
		LogRunnerService.update(context, intent.getAction());
		// schedule next update
		LogRunnerReceiver.schedNext(context);
	}
}
