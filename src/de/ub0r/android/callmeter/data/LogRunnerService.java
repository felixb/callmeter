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
import java.util.HashMap;

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
import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.lib.apis.TelephonyWrapper;

/**
 * Run logs in background.
 * 
 * @author flx
 */
public final class LogRunnerService extends IntentService {
	/** Tag for output. */
	private static final String TAG = "lrs";

	/** {@link Uri} to all threads. */
	private static final Uri URI_THREADS = Uri.parse(
			"content://mms-sms/conversations").buildUpon()
			.appendQueryParameter("simple", "true").build();
	/** {@link Uri} to all sms. */
	private static final Uri URI_SMS = Uri.parse("content://sms/");
	/** {@link Uri} to all mms. */
	private static final Uri URI_MMS = Uri.parse("content://mms/");

	/** Projection for threads table. */
	private static final String[] THREADS_PROJ = // .
	new String[] { "recipient_ids" };

	/** {@link HashMap} mapping threads to numbers. */
	private static final HashMap<Long, String> THREAD_TO_NUMBER = // .
	new HashMap<Long, String>();

	/** Run matcher. */
	public static final String ACTION_RUN_MATCHER = // .
	"de.ub0r.android.callmeter.RUN_MATCHER";

	/** Empty array of {@link ContentValues} to convert a list to array. */
	private static final ContentValues[] TO_ARRAY = new ContentValues[] {};

	/** Prefix for store of last data. */
	private static final String PREFS_LASTDATA_PREFIX = "last_data_";

	/** Thread Id. */
	private static final String THRADID = "thread_id";
	/** Type for mms. */
	private static final String MMS_TYPE = "m_type";
	/** Type for incoming mms. */
	private static final int MMS_IN = 132;
	/** Type for outgoing mms. */
	private static final int MMS_OUT = 128;

	/** Length of an SMS. */
	private static final int SMS_LENGTH = 160;

	/** Is phone roaming? */
	private static boolean roaming = false;
	/** Split messages at 160chars. */
	private static boolean splitAt160 = false;

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
		Log.d(TAG, "update(" + action + ")");
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
	 * Get maximum date of logs type %.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param type
	 *            type
	 * @param direction
	 *            direction
	 * @return maximum date found. -1 if nothing was found.
	 */
	private static long getMaxDate(final ContentResolver cr, final int type,
			final int direction) {
		final Cursor cursor = cr
				.query(DataProvider.Logs.CONTENT_URI,
						new String[] { DataProvider.Logs.DATE },
						DataProvider.Logs.TYPE + " = ? AND "
								+ DataProvider.Logs.DIRECTION + " = ?",
						new String[] { String.valueOf(type),
								String.valueOf(direction) },
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

		if (!doUpdate) {
			long dateLastRx = getMaxDate(cr, DataProvider.TYPE_DATA,
					DataProvider.DIRECTION_IN);
			long dateLastTx = getMaxDate(cr, DataProvider.TYPE_DATA,
					DataProvider.DIRECTION_OUT);
			final long updateinterval = Utils.parseLong(p.getString(
					Preferences.PREFS_UPDATE_INTERVAL, String
							.valueOf(LogRunnerReceiver.DELAY)),
					LogRunnerReceiver.DELAY)
					* LogRunnerReceiver.DELAY_FACTOR;
			final long n = System.currentTimeMillis();
			if (n - dateLastRx > updateinterval / 2) {
				doUpdate = true;
			}
			if (n - dateLastTx > updateinterval / 2) {
				doUpdate = true;
			}
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

				if (doUpdate || rrx > DATA_MIN_DIFF || rtx > DATA_MIN_DIFF) {
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
	 * @param direction
	 *            direction
	 */
	private static void updateSMS(final ContentResolver cr, // .
			final int direction) {
		int type = Calls.OUTGOING_TYPE;
		if (direction == DataProvider.DIRECTION_IN) {
			type = Calls.INCOMING_TYPE;
		}

		final long maxdate = getMaxDate(cr, DataProvider.TYPE_SMS, direction);
		final String[] smsProjection = new String[] { Calls.DATE, Calls.TYPE,
				"address", "body" };
		final Cursor cursor = cr.query(URI_SMS, smsProjection, Calls.DATE
				+ " > ? and " + Calls.TYPE + " = ?", new String[] {
				String.valueOf(maxdate), String.valueOf(type) }, Calls.DATE
				+ " DESC");
		if (cursor.moveToFirst()) {
			final int idDate = cursor.getColumnIndex(Calls.DATE);
			final int idAddress = cursor.getColumnIndex("address");
			final int idBody = cursor.getColumnIndex("body");
			final TelephonyWrapper wrapper = TelephonyWrapper.getInstance();

			final ArrayList<ContentValues> cvalues = // .
			new ArrayList<ContentValues>(CallMeter.HUNDRET);
			do {
				final ContentValues cv = new ContentValues();
				cv.put(DataProvider.Logs.DIRECTION, direction);
				cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_SMS);
				cv.put(DataProvider.Logs.DATE, cursor.getLong(idDate));
				cv.put(DataProvider.Logs.REMOTE, DataProvider.Logs.cleanNumber(
						cursor.getString(idAddress), false));
				final String body = cursor.getString(idBody);
				if (body != null && body.length() > 0) {
					if (splitAt160) {
						int l = ((body.length() - 1) / SMS_LENGTH) + 1;
						cv.put(DataProvider.Logs.AMOUNT, l);
					} else {
						try {
							cv.put(DataProvider.Logs.AMOUNT, wrapper
									.calculateLength(body, false)[0]);
						} catch (NullPointerException e) {
							Log.e(TAG, "error getting length for message: "
									+ body, e);
							cv.put(DataProvider.Logs.AMOUNT, 1);
						}
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
	 * @param context
	 *            {@link Context}
	 */
	private static void updateMMS(final Context context) {
		final ContentResolver cr = context.getContentResolver();
		final long maxdate = getMaxDate(cr, DataProvider.TYPE_MMS);
		final String[] mmsProjection = new String[] { Calls.DATE, MMS_TYPE,
				THRADID };
		Cursor cursor = cr.query(URI_MMS, mmsProjection, Calls.DATE + " > ?",
				new String[] { String.valueOf(maxdate) }, Calls.DATE + " DESC");
		if (!cursor.moveToFirst()) {
			cursor.close();
			cursor = cr.query(URI_MMS, mmsProjection, Calls.DATE + " > "
					+ (maxdate / CallMeter.MILLIS), null, Calls.DATE + " DESC");
		}
		if (cursor.moveToFirst()) {
			final int idDate = cursor.getColumnIndex(Calls.DATE);
			final int idType = cursor.getColumnIndex(MMS_TYPE);
			final int idThId = cursor.getColumnIndex(THRADID);

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
				final long tid = cursor.getLong(idThId);
				Log.d(TAG, "thread_id: " + tid);
				if (tid >= 0L) {
					String n = THREAD_TO_NUMBER.get(tid);
					if (n == null) {
						final Cursor c = cr.query(URI_THREADS, THREADS_PROJ,
								"_id = ?",
								new String[] { String.valueOf(tid) }, null);
						if (c.moveToFirst()) {
							final String rid = c.getString(0);
							Log.d(TAG, "recipient_ids: " + rid);
							final Contact con = new Contact(Utils.parseLong(
									rid, -1));
							con.update(context, true, false);
							n = DataProvider.Logs.cleanNumber(con.getNumber(),
									false);
							THREAD_TO_NUMBER.put(tid, n);
						}
						c.close();
					}
					if (n != null) {
						cv.put(DataProvider.Logs.REMOTE, n);
					}
				}

				cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
				cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_MMS);
				cv.put(DataProvider.Logs.DATE, fixDate(d));
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
		splitAt160 = p.getBoolean(Preferences.PREFS_SPLIT_SMS_AT_160, false);
		final boolean showCallInfo = p.getBoolean(
				Preferences.PREFS_SHOWCALLINFO, false);
		final boolean askForPlan = p.getBoolean(Preferences.PREFS_ASK_FOR_PLAN,
				false);

		final boolean runMatcher = a == ACTION_RUN_MATCHER;
		boolean shortRun = runMatcher
				|| a != null
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
		updateData(this, shortRun && !runMatcher);
		if (!shortRun || runMatcher) {
			if (deleteBefore > 0L) {
				deleteOldLogs(cr);
			}
			updateCalls(cr);
			updateSMS(cr, DataProvider.DIRECTION_IN);
			updateSMS(cr, DataProvider.DIRECTION_OUT);
			updateMMS(this);
			if (RuleMatcher.match(this, showDialog)) {
				StatsAppWidgetProvider.updateWidgets(this);
			}
		} else if (roaming) {
			updateCalls(cr);
			updateSMS(cr, DataProvider.DIRECTION_IN);
			updateSMS(cr, DataProvider.DIRECTION_OUT);
			updateMMS(this);
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
