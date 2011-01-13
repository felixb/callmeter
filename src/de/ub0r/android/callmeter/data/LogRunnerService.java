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
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.DatePreference;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.widget.Toast;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.ui.AskForPlan;
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
	private static long dateStart = 0L;

	/** Delete logs before that date. */
	private static long deleteBefore = -1L;

	/** Minimal difference of traffic which will get saved. */
	private static final long DATA_MIN_DIFF = 1024L * 512L;

	/** Time to wait for logs after hanging up. */
	private static final long WAIT_FOR_LOGS = 1500L;
	/** Maximum gap for logs. */
	private static final long GAP_FOR_LOGS = 10000L;

	/** Service's {@link Handler}. */
	private Handler handler = null;

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
	private static long fixDate(final long date) {
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
	private static long getMaxDate(final ContentResolver cr, final int type) {
		final Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI,
				new String[] { DataProvider.Logs.DATE }, DataProvider.Logs.TYPE
						+ " = ?", new String[] { String.valueOf(type) },
				DataProvider.Logs.DATE + " DESC");
		long maxdate = deleteBefore;
		if (cursor.moveToFirst()) {
			maxdate = cursor.getLong(0);
		}
		cursor.close();
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
	private static long getLastData(final SharedPreferences p, final int type,
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
	private static void setLastData(final SharedPreferences p, final int type,
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
	private static void updateData(final Context context,
			final boolean forceUpdate) {
		boolean doUpdate = forceUpdate;
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(context);
		final ContentResolver cr = context.getContentResolver();

		long dateLastRx = -1L;
		long dateLastTx = -1L;
		long amountLastRx = 0L;
		long amountLastTx = 0L;

		if (!doUpdate) {
			final long updateinterval = Utils.parseLong(p.getString(
					Preferences.PREFS_UPDATE_INTERVAL, String
							.valueOf(LogRunnerReceiver.DELAY)),
					LogRunnerReceiver.DELAY)
					* LogRunnerReceiver.DELAY_FACTOR;

			final String[] sel = new String[] {
					String.valueOf(DataProvider.TYPE_DATA),
					String.valueOf(DataProvider.DIRECTION_IN) };

			Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI,
					DataProvider.Logs.PROJECTION, DataProvider.Logs.TYPE
							+ " = ? AND " + DataProvider.Logs.DIRECTION
							+ " = ?", sel, DataProvider.Logs.DATE + " DESC");
			if (cursor.moveToFirst()) {
				final int roamed = cursor
						.getInt(DataProvider.Logs.INDEX_ROAMED);
				dateLastRx = cursor.getLong(DataProvider.Logs.INDEX_DATE);
				amountLastRx = cursor.getLong(DataProvider.Logs.INDEX_AMOUNT);
				if (roamed != 0) {
					doUpdate = true;
				}
				if (cursor.moveToNext()) {
					final long d = cursor.getLong(DataProvider.Logs.INDEX_DATE);
					if (dateLastRx - d > updateinterval / 2) {
						doUpdate = true;
					}
				} else {
					doUpdate = true;
				}
			} else {
				doUpdate = true;
			}
			cursor.close();

			sel[1] = String.valueOf(DataProvider.DIRECTION_OUT);
			cursor = cr.query(DataProvider.Logs.CONTENT_URI,
					DataProvider.Logs.PROJECTION, DataProvider.Logs.TYPE
							+ " = ? AND " + DataProvider.Logs.DIRECTION
							+ " = ?", sel, DataProvider.Logs.DATE + " DESC");
			if (cursor.moveToFirst()) {
				final int roamed = cursor
						.getInt(DataProvider.Logs.INDEX_ROAMED);
				dateLastTx = cursor.getLong(DataProvider.Logs.INDEX_DATE);
				amountLastTx = cursor.getLong(DataProvider.Logs.INDEX_AMOUNT);
				if (roamed != 0) {
					doUpdate = true;
				}
				if (cursor.moveToNext()) {
					final long d = cursor.getLong(DataProvider.Logs.INDEX_DATE);
					if (dateLastTx - d > updateinterval / 2) {
						doUpdate = true;
					}
				} else {
					doUpdate = true;
				}
			} else {
				doUpdate = true;
			}
			cursor.close();
		}

		final long lastRx = getLastData(p, DataProvider.TYPE_DATA,
				DataProvider.DIRECTION_IN);
		final long lastTx = getLastData(p, DataProvider.TYPE_DATA,
				DataProvider.DIRECTION_OUT);

		final Device d = Device.getDevice();
		final String inter = d.getCell();
		if (inter != null) {
			Log.d(TAG, "interface: " + inter);
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

				if (doUpdate || rrx + amountLastRx > DATA_MIN_DIFF
						|| rtx + amountLastTx > DATA_MIN_DIFF) {
					// save data to db
					final ContentValues baseCv = new ContentValues();
					baseCv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
					baseCv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
					baseCv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_DATA);
					baseCv.put(DataProvider.Logs.DATE, System
							.currentTimeMillis());
					if (roaming) {
						baseCv.put(DataProvider.Logs.ROAMED, 1);
					}

					ContentValues cv;
					if (rrx > 0) {
						cv = new ContentValues(baseCv);
						cv.put(DataProvider.Logs.DIRECTION,
								DataProvider.DIRECTION_IN);
						cv.put(DataProvider.Logs.AMOUNT, rrx);
						cr.insert(DataProvider.Logs.CONTENT_URI, cv);
						setLastData(p, DataProvider.TYPE_DATA,
								DataProvider.DIRECTION_IN, rx);
					} else {
						Log.d(TAG, "skip rx: " + rrx);
					}
					if (rtx > 0) {
						cv = new ContentValues(baseCv);
						cv.put(DataProvider.Logs.DIRECTION,
								DataProvider.DIRECTION_OUT);
						cv.put(DataProvider.Logs.AMOUNT, rtx);
						cr.insert(DataProvider.Logs.CONTENT_URI, cv);
						setLastData(p, DataProvider.TYPE_DATA,
								DataProvider.DIRECTION_OUT, tx);
					} else {
						Log.d(TAG, "skip tx: " + rtx);
					}
				} else if (rrx > 0 || rtx > 0) {
					final long cd = System.currentTimeMillis();
					final ContentValues values = new ContentValues(2);
					values.put(DataProvider.Logs.DATE, cd);
					final String[] sel = new String[] {
							String.valueOf(dateLastRx),
							String.valueOf(DataProvider.DIRECTION_IN) };
					if (rrx > 0) {
						values
								.put(DataProvider.Logs.AMOUNT, amountLastRx
										+ rrx);
						final int i = cr.update(DataProvider.Logs.CONTENT_URI,
								values, DataProvider.Logs.DATE + " = ? AND "
										+ DataProvider.Logs.TYPE + " = "
										+ DataProvider.TYPE_DATA + " AND "
										+ DataProvider.Logs.DIRECTION + " = ?",
								sel);
						if (i > 0) {
							LogRunnerService.setLastData(p,
									DataProvider.TYPE_DATA,
									DataProvider.DIRECTION_IN, rx);
						} else {
							Log.d(TAG, "update rrx failed: " + rrx);
						}
					}
					if (rtx > 0) {
						values
								.put(DataProvider.Logs.AMOUNT, amountLastTx
										+ rtx);
						sel[0] = String.valueOf(dateLastTx);
						sel[1] = String.valueOf(DataProvider.DIRECTION_OUT);
						final int i = cr.update(DataProvider.Logs.CONTENT_URI,
								values, DataProvider.Logs.DATE + " = ? AND "
										+ DataProvider.Logs.TYPE + " = "
										+ DataProvider.TYPE_DATA + " AND "
										+ DataProvider.Logs.DIRECTION + " = ?",
								sel);
						if (i > 0) {
							LogRunnerService.setLastData(p,
									DataProvider.TYPE_DATA,
									DataProvider.DIRECTION_OUT, tx);
						} else {
							Log.d(TAG, "update rtx failed: " + rtx);
						}
					}
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
	private static void updateCalls(final ContentResolver cr) {
		final long maxdate = getMaxDate(cr, DataProvider.TYPE_CALL);
		final String[] callsProjection = new String[] { Calls.TYPE,
				Calls.DURATION, Calls.DATE, Calls.NUMBER };
		final Cursor cursor = cr.query(Calls.CONTENT_URI, callsProjection,
				Calls.DATE + " > ?", new String[] { String.valueOf(maxdate) },
				Calls.DATE + " DESC");
		if (cursor.moveToFirst()) {
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
		cursor.close();
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
				+ " > ?", new String[] { String.valueOf(maxdate) }, Calls.DATE
				+ " DESC");
		if (cursor.moveToFirst()) {
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
		cursor.close();
	}

	/**
	 * Run logs: mms.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private static void updateMMS(final ContentResolver cr) {
		final long maxdate = getMaxDate(cr, DataProvider.TYPE_MMS);
		final Uri mmsUri = Uri.parse("content://mms/");
		final String[] mmsProjection = new String[] { Calls.DATE, MMS_TYPE, };
		Cursor cursor = cr.query(mmsUri, mmsProjection, Calls.DATE + " > ?",
				new String[] { String.valueOf(maxdate) }, Calls.DATE + " DESC");
		if (cursor == null || !cursor.moveToFirst()) {
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			cursor = cr.query(mmsUri, mmsProjection, Calls.DATE + " > "
					+ (maxdate / CallMeter.MILLIS), null, Calls.DATE + " DESC");
		}
		if (cursor.moveToFirst()) {
			final int idDate = cursor.getColumnIndex(Calls.DATE);
			final int idType = cursor.getColumnIndex(MMS_TYPE);
			// FIXME: final int idAddress = cursor.getColumnIndex("address");

			final ArrayList<ContentValues> cvalues = // .
			new ArrayList<ContentValues>(CallMeter.HUNDRET);
			do {
				final ContentValues cv = new ContentValues();
				final int t = cursor.getInt(idType);
				final long d = cursor.getLong(idDate);
				Log.d(TAG, "mms date: " + d);
				Log.d(TAG, "mms type: " + t);
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
				cv.put(DataProvider.Logs.DATE, fixDate(d));
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
		cursor.close();
	}

	/**
	 * Delete old logs to make this app fast.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 */
	private static void deleteOldLogs(final ContentResolver cr) {
		Log.d(TAG, "delete old logs: date < " + deleteBefore);
		final int ret = cr.delete(DataProvider.Logs.CONTENT_URI,
				DataProvider.Logs.DATE + " < ?", new String[] { String
						.valueOf(deleteBefore) });
		Log.i(TAG, "deleted old logs from internal database: " + ret);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		this.handler = new Handler() {
			@Override
			public void handleMessage(final Message msg) {
				Log.i(TAG, "In handleMessage...");
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onHandleIntent(final Intent intent) {
		final String a = intent.getAction();
		Log.d(TAG, "onHandleIntent(" + a + ")");

		if (a != null && a.equals(// .
				TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
			Log.i(TAG, "sleep for " + WAIT_FOR_LOGS + "ms");
			try {
				Thread.sleep(WAIT_FOR_LOGS);
			} catch (InterruptedException e) {
				Log.e(TAG, "interrupted while waiting for logs", e);
			}
		}

		final Handler h = Plans.getHandler();
		if (h != null) {
			h.sendEmptyMessage(Plans.MSG_BACKGROUND_START_MATCHER);
		}

		// update roaming info
		roaming = ((TelephonyManager) this
				.getSystemService(Context.TELEPHONY_SERVICE))
				.isNetworkRoaming();
		Log.d(TAG, "roaming: " + roaming);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		dateStart = p.getLong(Preferences.PREFS_DATE_BEGIN,
				DatePreference.DEFAULT_VALUE);
		deleteBefore = Preferences.getDeleteLogsBefore(p);
		final boolean showCallInfo = p.getBoolean(
				Preferences.PREFS_SHOWCALLINFO, false);
		final boolean askForPlan = p.getBoolean(Preferences.PREFS_ASK_FOR_PLAN,
				false);

		boolean shortRun = a != null
				&& (a.equals(Intent.ACTION_BOOT_COMPLETED)
						|| a.equals(Intent.ACTION_SHUTDOWN) // .
						|| a.equals(Intent.ACTION_REBOOT) // .
				|| a.equals(Intent.ACTION_DATE_CHANGED));

		if (!shortRun && a != null
				&& a.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
			if (intent.getBooleanExtra(ConnectivityManager.EXTRA_IS_FAILOVER,
					false)) {
				return;
			}
			shortRun = true;
		}

		final ContentResolver cr = this.getContentResolver();
		boolean showDialog = false;
		if (!shortRun && h != null) {
			final Cursor c = cr.query(DataProvider.Logs.CONTENT_URI,
					new String[] { DataProvider.Logs.PLAN_ID },
					DataProvider.Logs.RULE_ID + " != " + DataProvider.NO_ID
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
		updateData(this, shortRun);
		if (!shortRun) {
			if (deleteBefore > 0L) {
				deleteOldLogs(cr);
			}
			updateCalls(cr);
			updateSMS(cr);
			updateMMS(cr);
			if (RuleMatcher.match(this, showDialog)) {
				StatsAppWidgetProvider.updateWidgets(this);
			}
		} else if (roaming) {
			updateCalls(cr);
			updateSMS(cr);
			updateMMS(cr);
		}

		if ((showCallInfo || askForPlan) && a != null && a.equals(// .
				TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
			final Cursor c = cr.query(DataProvider.Logs.CONTENT_URI,
					DataProvider.Logs.PROJECTION, DataProvider.Logs.TYPE
							+ " = " + DataProvider.TYPE_CALL, null,
					DataProvider.Logs.DATE + " DESC");
			if (c != null && c.moveToFirst()) {
				final long id = c.getLong(DataProvider.Logs.INDEX_ID);
				final long date = c.getLong(DataProvider.Logs.INDEX_DATE);
				final long amount = c.getLong(DataProvider.Logs.INDEX_AMOUNT);

				final long now = System.currentTimeMillis();
				if (amount > 0L
						&& date + amount * CallMeter.MILLIS + GAP_FOR_LOGS // .
						>= now) {
					// only show real calls
					// only show calls made just now
					final float cost = c.getFloat(DataProvider.Logs.INDEX_COST);
					final String planname = DataProvider.Plans.getName(cr, c
							.getLong(DataProvider.Logs.INDEX_PLAN_ID));
					StringBuffer sb = new StringBuffer();
					sb.append(Plans.prettySeconds(amount, false));
					if (cost > 0) {
						String currencyFormat = Preferences
								.getCurrencyFormat(this);
						sb.append(Plans.delimiter
								+ String.format(currencyFormat, cost));
					}
					if (planname != null) {
						sb.insert(0, planname + ": ");
					} else if (askForPlan) {
						this.handler.post(new Runnable() {
							@Override
							public void run() {
								Log.i(TAG, "launching ask for plan dialog");
								final Intent i = new Intent(
										LogRunnerService.this, // .
										AskForPlan.class);
								i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								i.putExtra(AskForPlan.EXTRA_ID, id);
								i.putExtra(AskForPlan.EXTRA_DATE, date);
								i.putExtra(AskForPlan.EXTRA_AMOUNT, amount);
								LogRunnerService.this.startActivity(i);
							}
						});
					}
					if (showCallInfo) {
						final String s = sb.toString();
						Log.i(TAG, "Toast: " + s);
						this.handler.post(new Runnable() {
							@Override
							public void run() {
								final Toast toast = Toast.makeText(
										LogRunnerService.this, s,
										Toast.LENGTH_LONG);
								toast.show();
							}
						});
					}
				} else {
					Log.i(TAG, "skip Toast: amount=" + amount);
					Log.i(TAG, "skip Toast: date+amount+gap=" + (// .
							date + amount * CallMeter.MILLIS + GAP_FOR_LOGS));
					Log.i(TAG, "skip Toast: now            =" + now);
				}
			}
			if (c != null && !c.isClosed()) {
				c.close();
			}
		}

		// schedule next update
		LogRunnerReceiver.schedNext(this);
		if (h != null) {
			h.sendEmptyMessage(Plans.MSG_BACKGROUND_STOP_MATCHER);
		}
	}
}
