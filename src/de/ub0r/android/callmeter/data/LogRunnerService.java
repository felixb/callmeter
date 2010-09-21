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

import java.io.IOException;
import java.util.ArrayList;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.preference.DatePreference;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.ui.Plans;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.callmeter.widget.StatsAppWidgetProvider;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.TelephonyWrapper;

/**
 * Run logs in background.
 * 
 * @author flx
 */
public final class LogRunnerService extends IntentService {
	/** Tag for output. */
	private static final String TAG = "lrs";

	/** Empty array of {@link ContentValues} to convert a list to array. */
	private static final ContentValues[] TO_ARRAY = new ContentValues[] {};

	/** Prefix for store of last data. */
	private static final String PREFS_LASTDATA_PREFIX = "last_data_";

	/** Type for mms. */
	private static final String MMS_TYPE = "m_type";
	/** Type for incoming mms. */
	private static final int MMS_IN = 132;
	/** Type for outgoing mms. */
	private static final int MMS_OUT = 128;

	/** Is phone roaming? */
	private static boolean roaming = false;

	/** Ignore logs before. */
	private static long dateStart = 0;

	/**
	 * Default Constructor.
	 */
	public LogRunnerService() {
		super("LogRunner");
	}

	/**
	 * Run {@link LogRunnerService}.
	 * 
	 * @param action
	 *            original action sent to {@link LogRunnerReceiver}
	 * @param context
	 *            {@link Context}
	 */
	public static void update(final Context context, final String action) {
		context.startService(new Intent(action, null, context,
				LogRunnerService.class));
	}

	/**
	 * Fix MMS date.
	 * 
	 * @param date
	 *            date in millis or seconds
	 * @return date in millis
	 */
	private long fixDate(final long date) {
		if (date < CallMeter.MIN_DATE) {
			return date * CallMeter.MILLIS;
		} else {
			return date;
		}
	}

	/**
	 * Get maximum date of logs type %.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param type
	 *            type
	 * @return maximum date found. -1 if nothing was found.
	 */
	private long getMaxDate(final ContentResolver cr, final int type) {
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
		if (maxdate > dateStart) {
			return maxdate;
		}
		return dateStart;
	}

	/**
	 * Get last amount from Logs.
	 * 
	 * @param p
	 *            {@link SharedPreferences}
	 * @param type
	 *            type
	 * @param direction
	 *            direction
	 * @return amount of last log entry
	 */
	private long getLastData(final SharedPreferences p, final int type,
			final int direction) {
		return p.getLong(PREFS_LASTDATA_PREFIX + type + "_" + direction, 0L);
	}

	/**
	 * Set last amount from Logs.
	 * 
	 * @param p
	 *            {@link SharedPreferences}
	 * @param type
	 *            type
	 * @param direction
	 *            direction
	 * @param amount
	 *            amount
	 */
	private void setLastData(final SharedPreferences p, final int type,
			final int direction, final long amount) {
		p
				.edit()
				.putLong(PREFS_LASTDATA_PREFIX + type + "_" + direction, amount)
				.commit();
	}

	/**
	 * Run logs: data.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param forceUpdate
	 *            force update
	 */
	private void updateData(final Context context, final boolean forceUpdate) {
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final ContentResolver cr = context.getContentResolver();

		if (!forceUpdate) {
			final long maxdate = this.getMaxDate(cr, DataProvider.TYPE_DATA);
			final long updateinterval = Utils.parseLong(p.getString(
					Preferences.PREFS_UPDATE_INTERVAL, String
							.valueOf(LogRunnerReceiver.DELAY)),
					LogRunnerReceiver.DELAY)
					* LogRunnerReceiver.DELAY_FACTOR;
			if (System.currentTimeMillis() < maxdate + (updateinterval / 2)) {
				return; // skip update
			}
		}

		final long lastRx = this.getLastData(p, DataProvider.TYPE_DATA,
				DataProvider.DIRECTION_IN);
		final long lastTx = this.getLastData(p, DataProvider.TYPE_DATA,
				DataProvider.DIRECTION_OUT);

		final Device d = Device.getDevice();
		final String inter = d.getCell();
		if (inter != null) {
			try {
				final long rx = SysClassNet.getRxBytes(inter);
				final long tx = SysClassNet.getTxBytes(inter);
				Log.d(TAG, "rx: " + rx);
				Log.d(TAG, "tx: " + tx);
				long rrx = rx;
				long rtx = tx;
				if (rx >= lastRx && tx >= lastTx) {
					rrx = rx - lastRx;
					rtx = tx - lastTx;
				}
				Log.d(TAG, "rrx: " + rrx);
				Log.d(TAG, "ttx: " + rtx);

				if (rrx > 0 || rtx > 0) { // save data to db
					final ContentValues baseCv = new ContentValues();
					baseCv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
					baseCv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
					baseCv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_DATA);
					baseCv.put(DataProvider.Logs.DATE, System
							.currentTimeMillis());
					if (roaming) {
						baseCv.put(DataProvider.Logs.ROAMED, 1);
					}

					ContentValues cv = new ContentValues(baseCv);
					cv.put(DataProvider.Logs.DIRECTION,
							DataProvider.DIRECTION_IN);
					cv.put(DataProvider.Logs.AMOUNT, rrx);
					cr.insert(DataProvider.Logs.CONTENT_URI, cv);
					cv = new ContentValues(baseCv);
					cv.put(DataProvider.Logs.DIRECTION,
							DataProvider.DIRECTION_OUT);
					cv.put(DataProvider.Logs.AMOUNT, rtx);
					cr.insert(DataProvider.Logs.CONTENT_URI, cv);
					this.setLastData(p, DataProvider.TYPE_DATA,
							DataProvider.DIRECTION_IN, rx);
					this.setLastData(p, DataProvider.TYPE_DATA,
							DataProvider.DIRECTION_OUT, tx);
				}
			} catch (IOException e) {
				Log.e(TAG, "I/O Error", e);
			}
		}
	}

	/**
	 * Run logs: calls.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private void updateCalls(final ContentResolver cr) {
		final long maxdate = this.getMaxDate(cr, DataProvider.TYPE_CALL);
		final String[] callsProjection = new String[] { Calls.TYPE,
				Calls.DURATION, Calls.DATE, Calls.NUMBER };
		final Cursor cursor = cr.query(Calls.CONTENT_URI, callsProjection,
				Calls.DATE + " > " + maxdate, null, Calls.DATE + " DESC");
		if (cursor != null && cursor.moveToFirst()) {
			final int idType = cursor.getColumnIndex(Calls.TYPE);
			final int idDuration = cursor.getColumnIndex(Calls.DURATION);
			final int idDate = cursor.getColumnIndex(Calls.DATE);
			final int idNumber = cursor.getColumnIndex(Calls.NUMBER);

			final ArrayList<ContentValues> cvalues = // .
			new ArrayList<ContentValues>(CallMeter.HUNDRET);
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
				final int d = cursor.getInt(idDuration);
				if (d == 0) {
					continue;
				}
				cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_CALL);
				cv.put(DataProvider.Logs.DATE, cursor.getLong(idDate));
				cv.put(DataProvider.Logs.REMOTE, DataProvider.Logs.cleanNumber(
						cursor.getString(idNumber), false));
				cv.put(DataProvider.Logs.AMOUNT, d);
				if (roaming) {
					cv.put(DataProvider.Logs.ROAMED, 1);
				}
				cvalues.add(cv);
				if (cvalues.size() >= CallMeter.HUNDRET) {
					cr.bulkInsert(DataProvider.Logs.CONTENT_URI, cvalues
							.toArray(TO_ARRAY));
					Log.d(TAG, "new calls: " + cvalues.size());
					cvalues.clear();
				}
			} while (cursor.moveToNext());
			if (cvalues.size() > 0) {
				cr.bulkInsert(DataProvider.Logs.CONTENT_URI, cvalues
						.toArray(TO_ARRAY));
				Log.d(TAG, "new calls: " + cvalues.size());
			}
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	/**
	 * Run logs: sms.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private void updateSMS(final ContentResolver cr) {
		final long maxdate = this.getMaxDate(cr, DataProvider.TYPE_SMS);
		final Uri smsUri = Uri.parse("content://sms/");
		final String[] smsProjection = new String[] { Calls.DATE, Calls.TYPE,
				"address", "body" };
		final Cursor cursor = cr.query(smsUri, smsProjection, Calls.DATE
				+ " > " + maxdate, null, Calls.DATE + " DESC");
		if (cursor != null && cursor.moveToFirst()) {
			final int idDate = cursor.getColumnIndex(Calls.DATE);
			final int idType = cursor.getColumnIndex(Calls.TYPE);
			final int idAddress = cursor.getColumnIndex("address");
			final int idBody = cursor.getColumnIndex("body");
			final TelephonyWrapper wrapper = TelephonyWrapper.getInstance();

			final ArrayList<ContentValues> cvalues = // .
			new ArrayList<ContentValues>(CallMeter.HUNDRET);
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
				cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_SMS);
				cv.put(DataProvider.Logs.DATE, cursor.getLong(idDate));
				cv.put(DataProvider.Logs.REMOTE, DataProvider.Logs.cleanNumber(
						cursor.getString(idAddress), false));
				final String body = cursor.getString(idBody);
				if (body != null && body.length() > 0) {
					try {
						cv.put(DataProvider.Logs.AMOUNT, wrapper
								.calculateLength(body, false)[0]);
					} catch (NullPointerException e) {
						Log.e(TAG, "error getting length for message: " + body,
								e);
						cv.put(DataProvider.Logs.AMOUNT, 1);
					}
				} else {
					cv.put(DataProvider.Logs.AMOUNT, 1);
				}
				if (roaming) {
					cv.put(DataProvider.Logs.ROAMED, 1);
				}
				cvalues.add(cv);
				if (cvalues.size() >= CallMeter.HUNDRET) {
					cr.bulkInsert(DataProvider.Logs.CONTENT_URI, cvalues
							.toArray(TO_ARRAY));
					Log.d(TAG, "new sms: " + cvalues.size());
					cvalues.clear();
				}
			} while (cursor.moveToNext());
			if (cvalues.size() > 0) {
				cr.bulkInsert(DataProvider.Logs.CONTENT_URI, cvalues
						.toArray(TO_ARRAY));
				Log.d(TAG, "new sms: " + cvalues.size());
			}
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
	private void updateMMS(final ContentResolver cr) {
		final long maxdate = this.getMaxDate(cr, DataProvider.TYPE_SMS);
		final Uri mmsUri = Uri.parse("content://mms/");
		final String[] mmsProjection = new String[] { Calls.DATE, MMS_TYPE, };
		Cursor cursor = cr.query(mmsUri, mmsProjection, Calls.DATE + " > "
				+ maxdate, null, Calls.DATE + " DESC");
		if (cursor == null || !cursor.moveToFirst()) {
			cursor = cr.query(mmsUri, mmsProjection, Calls.DATE + " > "
					+ (maxdate / CallMeter.MILLIS), null, Calls.DATE + " DESC");
		}
		if (cursor != null && cursor.moveToFirst()) {
			final int idDate = cursor.getColumnIndex(Calls.DATE);
			final int idType = cursor.getColumnIndex(MMS_TYPE);
			// FIXME: final int idAddress = cursor.getColumnIndex("address");

			final ArrayList<ContentValues> cvalues = // .
			new ArrayList<ContentValues>(CallMeter.HUNDRET);
			do {
				final ContentValues cv = new ContentValues();
				final int t = cursor.getInt(idType);
				if (t == MMS_IN) {
					cv.put(DataProvider.Logs.DIRECTION,
							DataProvider.DIRECTION_IN);
				} else if (t == MMS_OUT) {
					cv.put(DataProvider.Logs.DIRECTION,
							DataProvider.DIRECTION_OUT);
				} else {
					continue;
				}
				cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_MMS);
				cv.put(DataProvider.Logs.DATE, this.fixDate(cursor
						.getLong(idDate)));
				// FIXME: cv.put(DataProvider.Logs.REMOTE,
				// DataProvider.Logs.cleanNumber(
				// cursor.getLong(idAddress), false));
				cv.put(DataProvider.Logs.AMOUNT, 1);
				if (roaming) {
					cv.put(DataProvider.Logs.ROAMED, 1);
				}
				cvalues.add(cv);
				if (cvalues.size() >= CallMeter.HUNDRET) {
					cr.bulkInsert(DataProvider.Logs.CONTENT_URI, cvalues
							.toArray(TO_ARRAY));
					Log.d(TAG, "new mms: " + cvalues.size());
					cvalues.clear();
				}
			} while (cursor.moveToNext());
			if (cvalues.size() > 0) {
				cr.bulkInsert(DataProvider.Logs.CONTENT_URI, cvalues
						.toArray(TO_ARRAY));
				Log.d(TAG, "new mms: " + cvalues.size());
			}
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onHandleIntent(final Intent intent) {
		final String a = intent.getAction();
		Log.d(TAG, "onHandleIntent(" + a + ")");
		final Handler h = Plans.getHandler();
		if (h != null) {
			h.sendEmptyMessage(Plans.MSG_BACKGROUND_START_MATCHER);
		}

		// update roaming info
		roaming = ((TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE))
				.isNetworkRoaming();
		dateStart = PreferenceManager.getDefaultSharedPreferences(this)
				.getLong(Preferences.PREFS_DATE_BEGIN,
						DatePreference.DEFAULT_VALUE);

		boolean shortRun = a != null
				&& (a.equals(Intent.ACTION_BOOT_COMPLETED)
						|| a.equals(Intent.ACTION_SHUTDOWN) // .
				|| a.equals(Intent.ACTION_REBOOT));

		final ContentResolver cr = this.getContentResolver();
		boolean showDialog = false;
		if (!shortRun && h != null) {
			final Cursor c = cr.query(DataProvider.Logs.CONTENT_URI,
					new String[] { DataProvider.Logs.PLAN_ID },
					DataProvider.Logs.PLAN_ID + " != " + DataProvider.NO_ID
							+ " AND " + DataProvider.Logs.TYPE + " != "
							+ DataProvider.TYPE_DATA, null, null);
			if (c != null && c.getCount() <= 0) {
				// skip if no plan is set up
				Cursor c1 = cr.query(DataProvider.Plans.CONTENT_URI,
						new String[] { DataProvider.Plans.ID }, null, null,
						null);
				if (c1 == null || c1.getCount() <= 0) {
					shortRun = true;
				}
				if (!c1.isClosed()) {
					c1.close();
				}
				// skip if no rule is set up
				c1 = cr.query(DataProvider.Rules.CONTENT_URI,
						new String[] { DataProvider.Rules.ID }, null, null,
						null);
				if (c1 == null || c1.getCount() <= 0) {
					shortRun = true;
				}
				if (!c1.isClosed()) {
					c1.close();
				}

				showDialog = true;
				h.sendEmptyMessage(Plans.MSG_BACKGROUND_START_RUNNER);
			}
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}
		this.updateData(this, shortRun);
		if (!shortRun) {
			this.updateCalls(cr);
			this.updateSMS(cr);
			this.updateMMS(cr);
			if (RuleMatcher.match(this, showDialog)) {
				StatsAppWidgetProvider.updateWidgets(this);
			}
		}

		// schedule next update
		LogRunnerReceiver.schedNext(this);
		if (h != null) {
			h.sendEmptyMessage(Plans.MSG_BACKGROUND_STOP_MATCHER);
		}
	}
}
