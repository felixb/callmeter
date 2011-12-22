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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
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
	static final long DELAY = 60; // 30min
	/** Factor for time between update checks. */
	static final long DELAY_FACTOR = CallMeter.SECONDS_MINUTE
			* CallMeter.MILLIS;

	/** ACTION for publishing information about sent websms. */
	private static final String ACTION_CM_WEBSMS = // .
	"de.ub0r.android.callmeter.SAVE_WEBSMS";
	/** Extra holding uri of sent sms. */
	private static final String EXTRA_WEBSMS_URI = "uri";
	/** Extra holding name of connector. */
	private static final String EXTRA_WEBSMS_CONNECTOR = "connector";

	/** ACTION for publishing information about calls. */
	private static final String ACTION_CM_SIP = // .
	"de.ub0r.android.callmeter.SAVE_SIPCALL";
	/** Extra holding uri of done call. */
	private static final String EXTRA_SIP_URI = "uri";
	/** Extra holding name of provider. */
	private static final String EXTRA_SIP_PROVIDER = "provider";

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void schedNext(final Context context) {
		Log.d(TAG, "schedNext(ctx)");
		final long delay = Utils.parseLong(
				PreferenceManager.getDefaultSharedPreferences(context)
						.getString(Preferences.PREFS_UPDATE_INTERVAL,
								String.valueOf(DELAY)), DELAY)
				* DELAY_FACTOR;
		Log.d(TAG, "schedNext(ctx): delay=" + delay);
		if (delay == 0L) {
			return;
		}
		schedNext(context, delay, null);
	}

	/**
	 * Schedule next update.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param delay
	 *            delay in milliseconds
	 * @param action
	 *            {@link Intent}'s action
	 */
	public static void schedNext(final Context context, final long delay,
			final String action) {
		Log.d(TAG, "schedNext(ctx, " + delay + "," + action + ")");
		final Intent i = new Intent(context, LogRunnerReceiver.class);
		if (action != null) {
			i.setAction(action);
		}
		final PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, 0);
		final long t = SystemClock.elapsedRealtime() + delay;
		final AlarmManager mgr = (AlarmManager) context
				.getSystemService(Context.ALARM_SERVICE);
		mgr.set(AlarmManager.ELAPSED_REALTIME, t, pi);
	}

	/**
	 * Save sent WebSMS to db.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri} to sent message
	 * @param mid
	 *            message id
	 * @param connector
	 *            connector name
	 */
	private static void saveWebSMS(final Context context, final String uri,
			final long mid, final String connector) {
		final ContentResolver cr = context.getContentResolver();
		final Cursor c = cr.query(Uri.parse(uri), new String[] { "date" },
				null, null, null);
		long date = -1;
		if (c != null && c.moveToFirst()) {
			date = c.getLong(0);
		}
		if (c != null && !c.isClosed()) {
			c.close();
		}
		final ContentValues cv = new ContentValues();
		cv.put(DataProvider.WebSMS.ID, mid);
		cv.put(DataProvider.WebSMS.CONNECTOR, connector);
		cv.put(DataProvider.WebSMS.DATE, date);
		cr.insert(DataProvider.WebSMS.CONTENT_URI, cv);
	}

	/**
	 * Save call via SIP to db.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param uri
	 *            {@link Uri} to call
	 * @param cid
	 *            call id
	 * @param provider
	 *            provider name
	 */
	private static void saveSipCall(final Context context, final String uri,
			final long cid, final String provider) {
		final ContentResolver cr = context.getContentResolver();
		final Cursor c = cr.query(Uri.parse(uri), new String[] { "date" },
				null, null, null);
		long date = -1;
		if (c != null && c.moveToFirst()) {
			date = c.getLong(0);
		}
		if (c != null && !c.isClosed()) {
			c.close();
		}
		final ContentValues cv = new ContentValues();
		cv.put(DataProvider.SipCall.ID, cid);
		cv.put(DataProvider.SipCall.PROVIDER, provider);
		cv.put(DataProvider.SipCall.DATE, date);
		cr.insert(DataProvider.SipCall.CONTENT_URI, cv);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onReceive(final Context context, final Intent intent) {
		Log.d(TAG, "wakeup");
		final String a = intent.getAction();
		Log.d(TAG, "action: " + a);
		if (a != null) {
			if (a.equals(ACTION_CM_WEBSMS)) {
				final String su = intent.getStringExtra(EXTRA_WEBSMS_URI);
				if (su != null && su.length() > 0) {
					final long si = Utils.parseLong(su.replaceAll(".*/", ""),
							-1);
					final String sc = intent
							.getStringExtra(EXTRA_WEBSMS_CONNECTOR);
					Log.d(TAG, "websms id:  " + si);
					Log.d(TAG, "websms con: " + sc);
					if (si >= 0L) {
						saveWebSMS(context, su, si, sc);
					}
				}
			} else if (a.equals(ACTION_CM_SIP)) {
				final String su = intent.getStringExtra(EXTRA_SIP_URI);
				if (su != null && su.length() > 0) {
					final long si = Utils.parseLong(su.replaceAll(".*/", ""),
							-1);
					final String sc = intent.getStringExtra(EXTRA_SIP_PROVIDER);
					Log.d(TAG, "sip call id:  " + si);
					Log.d(TAG, "sip call con: " + sc);
					if (si >= 0L) {
						saveSipCall(context, su, si, sc);
					}
				}
			} else if (a.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
				final String state = intent
						.getStringExtra(TelephonyManager.EXTRA_STATE);
				if (state != null
						&& !state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
					return;
				}
			}
		}
		// run LogRunnerService
		LogRunnerService.update(context, a);
		// schedule next update
		LogRunnerReceiver.schedNext(context);
	}
}
