/*
 * Copyright (C) 2009-2013 Felix Bechstein
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

import android.Manifest;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.DatePreference;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.SmsMessage;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import de.ub0r.android.callmeter.BuildConfig;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.ui.AskForPlan;
import de.ub0r.android.callmeter.ui.Common;
import de.ub0r.android.callmeter.ui.Plans;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.callmeter.widget.LogsAppWidgetProvider;
import de.ub0r.android.callmeter.widget.StatsAppWidgetProvider;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.Contact;
import de.ub0r.android.logg0r.Log;

/**
 * Run logs in background.
 *
 * @author flx
 */
// TODO extend JobIntentService
public final class LogRunnerService extends IntentService {

    /**
     * Tag for output.
     */
    private static final String TAG = "LogRunnerService";

    /**
     * Minimum amount of unmatched logs to start showing the dialog.
     */
    private static final int UNMATHCEDLOGS_TO_SHOW_DIALOG = 50;

    /**
     * {@link Uri} to all threads.
     */
    private static final Uri URI_THREADS = Uri.parse("content://mms-sms/conversations").buildUpon()
            .appendQueryParameter("simple", "true").build();

    /**
     * {@link Uri} to all sms.
     */
    public static final Uri URI_SMS = Uri.parse("content://sms/");

    /**
     * {@link Uri} to all mms.
     */
    private static final Uri URI_MMS = Uri.parse("content://mms/");

    /**
     * Projection for threads table.
     */
    private static final String[] THREADS_PROJ = new String[]{"recipient_ids"};

    /**
     * {@link HashMap} mapping threads to numbers.
     */
    private static final SparseArray<String> THREAD_TO_NUMBER = new SparseArray<String>();

    /**
     * {@link Intent}'s action for run matcher.
     */
    public static final String ACTION_RUN_MATCHER = "de.ub0r.android.callmeter.RUN_MATCHER";

    /**
     * {@link Intent}'s action for short run.
     */
    public static final String ACTION_SHORT_RUN = "de.ub0r.android.callmeter.SHORT_RUN";

    /**
     * {@link Intent}'s action for receiving SMS.
     */
    private static final String ACTION_SMS = "android.provider.Telephony.SMS_RECEIVED";

    /**
     * Prefix for store of last data.
     */
    private static final String PREFS_LASTDATA_PREFIX = "last_data_";

    /**
     * Thread Id.
     */
    private static final String THRADID = "thread_id";

    /**
     * Type for mms.
     */
    private static final String MMS_TYPE = "m_type";

    /**
     * Type for incoming mms.
     */
    private static final int MMS_IN = 132;

    /**
     * Type for outgoing mms.
     */
    private static final int MMS_OUT = 128;

    /**
     * Length of an SMS.
     */
    private static final int SMS_LENGTH = 160;

    /**
     * Is phone roaming?
     */
    private static boolean roaming = false;

    /**
     * My own number.
     */
    private static String mynumber = null;

    /**
     * Split messages at 160chars.
     */
    private static boolean splitAt160 = false;

    /**
     * Ignore logs before.
     */
    private static long dateStart = 0L;

    /**
     * Delete logs before that date.
     */
    private static long deleteBefore = -1L;

    /**
     * Minimal difference of traffic which will get saved.
     */
    private static final long DATA_MIN_DIFF = 1024L * 512L;

    /**
     * Time to wait for logs after hanging up.
     */
    private static final long WAIT_FOR_LOGS = 1500L;

    /**
     * Maximum gap for logs.
     */
    private static final long GAP_FOR_LOGS = 10000L;

    /**
     * Minimal time between updates.
     */
    private static final long MIN_DELAY = 5000L;

    /**
     * Action of last call.
     */
    private static String lastAction = null;

    /**
     * Time of last call.
     */
    private static long lastUpdate = 0L;

    private static boolean inUpdate = false;

    /**
     * Service's {@link Handler}.
     */
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
     * @param action  original action sent to {@link LogRunnerReceiver}
     * @param context {@link Context}
     */
    public static void update(final Context context, final String action) {
        Log.d(TAG, "update(", action, ")");
        long now = System.currentTimeMillis();
        if (inUpdate) {
            Log.i(TAG, "skip update(" + action + "): still updating");
        } else if ((action == null && lastAction != null)
                || (action != null && !action.equals(lastAction)) || lastUpdate == 0L
                || now > lastUpdate + MIN_DELAY) {
            context.startService(new Intent(action, null, context, LogRunnerService.class));
            lastAction = action;
            lastUpdate = now;
        } else {
            Log.i(TAG, "skip update(" + action + "): " + now + "<=" + (lastUpdate + MIN_DELAY));
        }
    }

    /**
     * Fix MMS date.
     *
     * @param date date in millis or seconds
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
     * @param cr   {@link ContentResolver}
     * @param type type
     * @return maximum date found. -1 if nothing was found.
     */
    private static long getMaxDate(final ContentResolver cr, final int type) {
        Log.d(TAG, "getMaxDate(", type, ")");
        final Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI,
                new String[]{DataProvider.Logs.DATE}, DataProvider.Logs.TYPE + " = ?",
                new String[]{String.valueOf(type)}, DataProvider.Logs.DATE + " DESC LIMIT 1");
        long maxdate = deleteBefore;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxdate = cursor.getLong(0);
                Log.d(TAG, "maxdate=", maxdate);
            }
            cursor.close();
        }
        if (maxdate > dateStart) {
            return maxdate;
        }
        Log.d(TAG, "getMaxDate(): dateStart=", dateStart);
        return dateStart;
    }

    /**
     * Get maximum date of logs type %.
     *
     * @param cr        {@link ContentResolver}
     * @param type      type
     * @param direction direction
     * @return maximum date found. -1 if nothing was found.
     */
    private static long getMaxDate(final ContentResolver cr, final int type, final int direction) {
        Log.d(TAG, "getMaxDate(", type, ",", direction, ")");
        final Cursor cursor = cr.query(DataProvider.Logs.CONTENT_URI,
                new String[]{DataProvider.Logs.DATE}, DataProvider.Logs.TYPE + " = ? AND "
                        + DataProvider.Logs.DIRECTION + " = ?", new String[]{
                        String.valueOf(type), String.valueOf(direction)}, DataProvider.Logs.DATE
                        + " DESC LIMIT 1"
        );
        long maxdate = deleteBefore;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                maxdate = cursor.getLong(0);
            }
            cursor.close();
        }
        if (maxdate > dateStart) {
            Log.d(TAG, "getMaxDate(): ", maxdate);
            return maxdate;
        }
        Log.d(TAG, "getMaxDate(): ", dateStart);
        return dateStart;
    }

    /**
     * Get last amount from Logs.
     *
     * @param p         {@link SharedPreferences}
     * @param type      type
     * @param direction direction
     * @return amount of last log entry
     */
    private static long getLastData(final SharedPreferences p, final int type,
                                    final int direction) {
        Log.d(TAG, "getLastData(p,", type, ",", direction, ")");
        long l = p.getLong(PREFS_LASTDATA_PREFIX + type + "_" + direction, 0L);
        Log.d(TAG, "getLastData(): ", l);
        return l;
    }

    /**
     * Get the last record for some kind of data; only unmatched log records are returned!
     *
     * @param cr        {@link ContentResolver}
     * @param type      type
     * @param direction direction
     * @return {@link Cursor} of last log record; column 0=id,1=amount
     */
    private static Cursor getLastData(final ContentResolver cr, final int type,
                                      final int direction) {
        Cursor c = cr.query(DataProvider.Logs.CONTENT_URI, new String[]{DataProvider.Logs.ID,
                        DataProvider.Logs.AMOUNT, DataProvider.Logs.PLAN_ID,
                        DataProvider.Logs.ROAMED},
                DataProvider.Logs.TYPE + " = ? AND " + DataProvider.Logs.DIRECTION + " = ?",
                new String[]{String.valueOf(type), String.valueOf(direction)},
                DataProvider.Logs.DATE + " DESC LIMIT 1"
        );
        if (c == null) {
            return null;
        }
        if (c.moveToFirst()) {
            int pid = c.getInt(2);
            if (pid != DataProvider.NO_ID && pid != DataProvider.NOT_FOUND
                    || roaming != (c.getInt(3) == 1)) {
                c.close();
                return null;
            }

            return c;
        }
        if (!c.isClosed()) {
            c.close();
        }
        return null;
    }

    /**
     * Set last amount from Logs.
     *
     * @param e         {@link Editor}
     * @param type      type
     * @param direction direction
     * @param amount    amount
     */
    public static void setLastData(final Editor e, final int type, final int direction,
                                   final long amount) {
        if (amount < 0L) {
            e.remove(PREFS_LASTDATA_PREFIX + type + "_" + direction);
        } else {
            e.putLong(PREFS_LASTDATA_PREFIX + type + "_" + direction, amount);
        }
    }

    /**
     * Run logs: data.
     *
     * @param context     {@link Context}
     * @param forceUpdate force update
     */
    private static void updateData(final Context context, final boolean forceUpdate) {
        Log.d(TAG, "updateData(", forceUpdate, ")");
        boolean doUpdate = forceUpdate;
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final ContentResolver cr = context.getContentResolver();

        if (!doUpdate) {
            long dateLastRx = getMaxDate(cr, DataProvider.TYPE_DATA, DataProvider.DIRECTION_IN);
            long dateLastTx = getMaxDate(cr, DataProvider.TYPE_DATA, DataProvider.DIRECTION_OUT);
            final long updateinterval = Utils.parseLong(
                    p.getString(Preferences.PREFS_UPDATE_INTERVAL,
                            String.valueOf(LogRunnerReceiver.DELAY)), LogRunnerReceiver.DELAY
            )
                    * LogRunnerReceiver.DELAY_FACTOR;
            final long n = System.currentTimeMillis();
            if (n - dateLastRx > updateinterval / 2) {
                doUpdate = true;
            }
            if (n - dateLastTx > updateinterval / 2) {
                doUpdate = true;
            }
        }

        final long lastRx = getLastData(p, DataProvider.TYPE_DATA, DataProvider.DIRECTION_IN);
        final long lastTx = getLastData(p, DataProvider.TYPE_DATA, DataProvider.DIRECTION_OUT);

        final Device d = Device.getDevice();
        try {
            final long rx = d.getCellRxBytes();
            final long tx = d.getCellTxBytes();
            Log.d(TAG, "rx: ", rx);
            Log.d(TAG, "tx: ", tx);
            if (rx > 0L || tx > 0L) {
                long rrx = rx;
                long rtx = tx;
                if (rx >= lastRx && tx >= lastTx) {
                    rrx = rx - lastRx;
                    rtx = tx - lastTx;
                }
                Log.d(TAG, "rrx: ", rrx);
                Log.d(TAG, "ttx: ", rtx);

                if (doUpdate || rrx > DATA_MIN_DIFF || rtx > DATA_MIN_DIFF) {
                    // save data to db
                    final ContentValues baseCv = new ContentValues();
                    baseCv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
                    baseCv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
                    baseCv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_DATA);
                    baseCv.put(DataProvider.Logs.DATE, System.currentTimeMillis());
                    if (roaming) {
                        baseCv.put(DataProvider.Logs.ROAMED, 1);
                    }
                    if (!TextUtils.isEmpty(mynumber)) {
                        baseCv.put(DataProvider.Logs.MYNUMBER, mynumber);
                    }

                    ContentValues cv;
                    Editor e = p.edit();
                    boolean dirty = false;
                    boolean update = rrx > 0;
                    Cursor c = null;
                    if (update && forceUpdate && rrx <= DATA_MIN_DIFF) {
                        c = getLastData(cr, DataProvider.TYPE_DATA, DataProvider.DIRECTION_IN);
                        Log.d(TAG, "getLastData()=", c);
                    }
                    if (update && c == null) {
                        cv = new ContentValues(baseCv);
                        cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_IN);
                        cv.put(DataProvider.Logs.AMOUNT, rrx);
                        cr.insert(DataProvider.Logs.CONTENT_URI, cv);
                    } else if (c != null) {
                        Log.d(TAG, "force rx update: ", rrx);
                        cv = new ContentValues(baseCv);
                        cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_IN);
                        cv.put(DataProvider.Logs.AMOUNT, rrx + c.getLong(1));
                        cr.update(DataProvider.Logs.CONTENT_URI, cv, DataProvider.Logs.ID + "=?",
                                new String[]{c.getString(0)});
                        c.close();
                    } else {
                        Log.d(TAG, "skip rx: ", rrx);
                    }
                    if (update) {
                        setLastData(e, DataProvider.TYPE_DATA, DataProvider.DIRECTION_IN, rx);
                        dirty = true;
                    }

                    update = rtx > 0;
                    c = null;
                    if (update && forceUpdate && rtx <= DATA_MIN_DIFF) {
                        c = getLastData(cr, DataProvider.TYPE_DATA, DataProvider.DIRECTION_OUT);
                        Log.d(TAG, "getLastData()=", c);
                    }
                    if (update && c == null) {
                        cv = new ContentValues(baseCv);
                        cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_OUT);
                        cv.put(DataProvider.Logs.AMOUNT, rtx);
                        cr.insert(DataProvider.Logs.CONTENT_URI, cv);
                    } else if (c != null) {
                        Log.d(TAG, "force tx update: ", rtx);
                        cv = new ContentValues(baseCv);
                        cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_OUT);
                        cv.put(DataProvider.Logs.AMOUNT, rtx + c.getLong(1));
                        cr.update(DataProvider.Logs.CONTENT_URI, cv, DataProvider.Logs.ID + "=?",
                                new String[]{c.getString(0)});
                        c.close();
                    } else {
                        Log.d(TAG, "skip tx: ", rtx);
                    }
                    if (update) {
                        setLastData(e, DataProvider.TYPE_DATA, DataProvider.DIRECTION_OUT, tx);
                        dirty = true;
                    }
                    if (dirty) {
                        e.apply();
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "I/O Error", e);
        }
        Log.d(TAG, "updateData(): done");
    }

    private static void printColumn(final Cursor c, int n) {
        Log.i(TAG, "---------- column - start ----: ", n);
        int l = c.getColumnCount();
        for (int i = 0; i < l; ++i) {
            Log.i(TAG, c.getColumnName(i), ": ", c.getString(i));
        }
        Log.i(TAG, "---------- column - end ------: ", n);
    }

    /**
     * Check, if there is dual sim support.
     */
    public static boolean checkCallsSimIdColumn(final ContentResolver cr) {
        try {
            String where = BuildConfig.DEBUG_LOG ? null : "1=2";
            Cursor c = cr.query(Calls.CONTENT_URI, null, where, null, null);
            boolean check = false;
            if (c != null) {
                check = SimIdColumnFinder.getsInstance().getSimIdColumn(cr, Calls.CONTENT_URI, c) >= 0;
                c.close();
            }
            Log.i(TAG, "sim_id column found in calls database: " + check);
            return check;
        } catch (SQLiteException e) {
            Log.w(TAG, "sim_id check for calls failed", e);
            return false;
        }
    }

    /**
     * Check, if there is dual sim support.
     */
    public static boolean checkSmsSimIdColumn(final ContentResolver cr) {
        try {
            String where = BuildConfig.DEBUG_LOG ? null : "1=2";
            Cursor c = cr.query(URI_SMS, null, where, null, null);
            boolean check = false;
            if (c != null) {
                check = SimIdColumnFinder.getsInstance().getSimIdColumn(cr, URI_SMS, c) >= 0;
                c.close();
            }
            Log.i(TAG, "sim_id column found in sms database: " + check);
            return check;
        } catch (SQLiteException e) {
            Log.w(TAG, "sim_id check for sms failed", e);
            return false;
        }
    }

    /**
     * Run logs: calls.
     */
    private static void updateCalls(final Context context) {
        Log.d(TAG, "updateCalls()");
        final ContentResolver cr = context.getContentResolver();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        long maxdate = Math.max(getLastData(p, DataProvider.TYPE_CALL, 0),
                getMaxDate(cr, DataProvider.TYPE_CALL));
        Log.d(TAG, "maxdate: ", maxdate);
        Cursor cursor;
        try {
            cursor = cr.query(Calls.CONTENT_URI, null, Calls.DATE + " > ?",
                    new String[]{String.valueOf(maxdate)}, Calls.DATE + " DESC");
        } catch (SQLException e) {
            Log.e(TAG, "updateCalls(): SQLE", e);
            cursor = cr.query(Calls.CONTENT_URI, new String[]{Calls.TYPE, Calls.DURATION,
                            Calls.DATE, Calls.NUMBER}, Calls.DATE + " > ?",
                    new String[]{String.valueOf(maxdate)}, Calls.DATE + " DESC"
            );
        } catch (NullPointerException e) {
            Log.e(TAG, "updateCalls(): NPE", e);
            return;
        }
        if (cursor == null) {
            Log.d(TAG, "updateCalls(): null");
            return;
        }
        Log.d(TAG, "cursor: ", cursor.getCount());
        if (cursor.moveToFirst()) {
            final int idType = cursor.getColumnIndex(Calls.TYPE);
            final int idDuration = cursor.getColumnIndex(Calls.DURATION);
            final int idDate = cursor.getColumnIndex(Calls.DATE);
            final int idNumber = cursor.getColumnIndex(Calls.NUMBER);
            final int idSimId = SimIdColumnFinder.getsInstance().getSimIdColumn(cr, Calls.CONTENT_URI, cursor);

            final ArrayList<ContentValues> cvalues = new ArrayList<ContentValues>(
                    CallMeter.HUNDRED);
            int i = 0;
            do {
                if (BuildConfig.DEBUG_LOG && i < 30) {
                    printColumn(cursor, i);
                    i += 1;
                }
                final ContentValues cv = new ContentValues();
                final int t = cursor.getInt(idType);
                if (t == Calls.INCOMING_TYPE) {
                    cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_IN);
                } else if (t == Calls.OUTGOING_TYPE) {
                    cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_OUT);
                } else {
                    Log.w(TAG, "ignore unknown direction");
                    continue;
                }
                final int d = cursor.getInt(idDuration);
                if (d == 0) {
                    Log.i(TAG, "ignore duration=0");
                    continue;
                }
                long l = cursor.getLong(idDate);
                if (l > maxdate) {
                    maxdate = l;
                }
                cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
                cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
                cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_CALL);
                cv.put(DataProvider.Logs.DATE, l);
                cv.put(DataProvider.Logs.REMOTE,
                        DataProvider.Logs.cleanNumber(cursor.getString(idNumber), false));
                cv.put(DataProvider.Logs.AMOUNT, d);
                if (roaming) {
                    cv.put(DataProvider.Logs.ROAMED, 1);
                }
                if (idSimId >= 0) {
                    cv.put(DataProvider.Logs.MYNUMBER, cursor.getString(idSimId));
                } else if (!TextUtils.isEmpty(mynumber)) {
                    cv.put(DataProvider.Logs.MYNUMBER, mynumber);
                }
                cvalues.add(cv);
                if (cvalues.size() >= CallMeter.HUNDRED) {
                    cr.bulkInsert(DataProvider.Logs.CONTENT_URI,
                            cvalues.toArray(new ContentValues[cvalues.size()]));
                    Log.d(TAG, "new calls: ", cvalues.size());
                    cvalues.clear();
                }
            } while (cursor.moveToNext());
            if (cvalues.size() > 0) {
                cr.bulkInsert(DataProvider.Logs.CONTENT_URI,
                        cvalues.toArray(new ContentValues[cvalues.size()]));
                Log.d(TAG, "new calls: ", cvalues.size());
                Editor e = p.edit();
                setLastData(e, DataProvider.TYPE_CALL, 0, maxdate);
                e.apply();
            }
        }
        cursor.close();
        Log.d(TAG, "updateCalls(): done");
    }

    /**
     * Run logs: sms.
     *
     * @param cr        {@link ContentResolver}
     * @param direction direction
     */
    private static void updateSMS(final ContentResolver cr, final int direction) {
        Log.d(TAG, "updateSMS(cr,", direction, ")");
        int type = Calls.OUTGOING_TYPE;
        if (direction == DataProvider.DIRECTION_IN) {
            type = Calls.INCOMING_TYPE;
        }

        final long maxdate = getMaxDate(cr, DataProvider.TYPE_SMS, direction);
        final String[] smsProjection = new String[]{Calls.DATE, Calls.TYPE, "address", "body"};
        Cursor cursor;
        try {
            try {
                cursor = cr.query(URI_SMS, null, Calls.DATE + " > ? and " + Calls.TYPE + " = ?",
                        new String[]{String.valueOf(maxdate), String.valueOf(type)}, Calls.DATE
                                + " DESC"
                );
            } catch (SQLException e) {
                Log.e(TAG, "updateCalls(): SQLE", e);
                cursor = cr.query(URI_SMS, smsProjection, Calls.DATE + " > ? and " + Calls.TYPE
                                + " = ?",
                        new String[]{String.valueOf(maxdate), String.valueOf(type)},
                        Calls.DATE + " DESC"
                );
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "updateSMS(): NPE", e);
            return;
        }
        if (cursor == null) {
            Log.d(TAG, "updateSMS(): null");
            return;
        }
        Log.d(TAG, "cursor: ", cursor.getCount());
        if (cursor.moveToFirst()) {
            final int idDate = cursor.getColumnIndex(Calls.DATE);
            final int idAddress = cursor.getColumnIndex("address");
            final int idBody = cursor.getColumnIndex("body");
            final int idSimId = SimIdColumnFinder.getsInstance().getSimIdColumn(cr, URI_SMS, cursor);
            final ArrayList<ContentValues> cvalues = new ArrayList<ContentValues>(
                    CallMeter.HUNDRED);
            int i = 0;
            do {
                if (BuildConfig.DEBUG_LOG && i < 30) {
                    printColumn(cursor, i);
                    i += 1;
                }
                final ContentValues cv = new ContentValues();
                cv.put(DataProvider.Logs.DIRECTION, direction);
                cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
                cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
                cv.put(DataProvider.Logs.TYPE, DataProvider.TYPE_SMS);
                cv.put(DataProvider.Logs.DATE, cursor.getLong(idDate));
                Log.d(TAG, "date: ", cursor.getLong(idDate));
                cv.put(DataProvider.Logs.REMOTE,
                        DataProvider.Logs.cleanNumber(cursor.getString(idAddress), false));
                final String body = cursor.getString(idBody);
                int l = 1;
                if (!TextUtils.isEmpty(body)) {
                    Log.d(TAG, "body: ", body.replaceAll("[a-z]", "x").replaceAll("[A-Z]", "X"));
                    if (splitAt160) {
                        l = ((body.length() - 1) / SMS_LENGTH) + 1;
                    } else {
                        try {
                            l = SmsMessage.calculateLength(body, false)[0];
                        } catch (NullPointerException e) {
                            Log.e(TAG, "error getting length for message: " + body, e);
                            l = ((body.length() - 1) / SMS_LENGTH) + 1;
                        } catch (RuntimeException e) {
                            Log.e(TAG, "SMS app not available", e);
                            l = ((body.length() - 1) / SMS_LENGTH) + 1;
                        }
                        Log.d(TAG, "body length: ", l);
                    }
                }
                cv.put(DataProvider.Logs.AMOUNT, l);
                if (roaming) {
                    cv.put(DataProvider.Logs.ROAMED, 1);
                }
                if (idSimId >= 0) {
                    cv.put(DataProvider.Logs.MYNUMBER, cursor.getString(idSimId));
                } else if (!TextUtils.isEmpty(mynumber)) {
                    cv.put(DataProvider.Logs.MYNUMBER, mynumber);
                }
                cvalues.add(cv);
                if (cvalues.size() >= CallMeter.HUNDRED) {
                    cr.bulkInsert(DataProvider.Logs.CONTENT_URI,
                            cvalues.toArray(new ContentValues[cvalues.size()]));
                    Log.d(TAG, "new sms: ", cvalues.size());
                    cvalues.clear();
                }
            } while (cursor.moveToNext());
            if (cvalues.size() > 0) {
                cr.bulkInsert(DataProvider.Logs.CONTENT_URI,
                        cvalues.toArray(new ContentValues[cvalues.size()]));
                Log.d(TAG, "new sms: ", cvalues.size());
            }
        }
        cursor.close();
        Log.d(TAG, "updateSMS(): done");
    }

    /**
     * Run logs: mms.
     *
     * @param context {@link Context}
     */
    private static void updateMMS(final Context context) {
        Log.d(TAG, "updateMMS()");
        if (!CallMeter.hasPermission(context, Manifest.permission.READ_CONTACTS)) {
            Log.w(TAG, "skip MMS logs, permission READ_CONTACTS is denied");
            return;
        }

        final ContentResolver cr = context.getContentResolver();
        final long maxdate = getMaxDate(cr, DataProvider.TYPE_MMS);
        final String[] mmsProjection = new String[]{Calls.DATE, MMS_TYPE, THRADID};
        Cursor cursor;
        try {
            cursor = cr.query(URI_MMS, mmsProjection, Calls.DATE + " > ?",
                    new String[]{String.valueOf(maxdate)}, Calls.DATE + " DESC");
        } catch (NullPointerException e) {
            Log.e(TAG, "updateMMS(): NPE", e);
            return;
        }
        if (cursor == null) {
            Log.d(TAG, "updateMMS(): null");
            return;
        }
        if (!cursor.moveToFirst()) {
            cursor.close();
            cursor = cr.query(URI_MMS, mmsProjection, Calls.DATE + " > "
                    + (maxdate / CallMeter.MILLIS), null, Calls.DATE + " DESC");
        }
        if (cursor == null) {
            Log.d(TAG, "updateMMS(): null");
            return;
        }
        Log.d(TAG, "cursor: ", cursor.getCount());
        if (cursor.moveToFirst()) {
            final int idDate = cursor.getColumnIndex(Calls.DATE);
            final int idType = cursor.getColumnIndex(MMS_TYPE);
            final int idThId = cursor.getColumnIndex(THRADID);
            final int idSimId = cursor.getColumnIndex("sim_id");

            final ArrayList<ContentValues> cvalues = new ArrayList<ContentValues>(
                    CallMeter.HUNDRED);
            do {
                final ContentValues cv = new ContentValues();
                final int t = cursor.getInt(idType);
                final long d = cursor.getLong(idDate);
                Log.d(TAG, "mms date: ", d);
                Log.d(TAG, "mms type: ", t);
                if (t == MMS_IN) {
                    cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_IN);
                } else if (t == MMS_OUT) {
                    cv.put(DataProvider.Logs.DIRECTION, DataProvider.DIRECTION_OUT);
                } else {
                    continue;
                }
                final int tid = cursor.getInt(idThId);
                Log.d(TAG, "thread_id: ", tid);
                if (tid >= 0L) {
                    String n = THREAD_TO_NUMBER.get(tid);
                    if (n == null) {
                        final Cursor c = cr.query(URI_THREADS, THREADS_PROJ, "_id = ?",
                                new String[]{String.valueOf(tid)}, null);
                        assert c != null;
                        if (c.moveToFirst()) {
                            final String rid = c.getString(0);
                            Log.d(TAG, "recipient_ids: ", rid);
                            try {
                                final Contact con = new Contact(Utils.parseLong(rid, -1));
                                con.update(context, true, false);
                                n = DataProvider.Logs.cleanNumber(con.getNumber(), false);
                                THREAD_TO_NUMBER.put(tid, n);
                            } catch (NullPointerException e) {
                                Log.e(TAG, "error loading number from recipient_ids", rid, e);
                            }
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
                if (idSimId >= 0) {
                    cv.put(DataProvider.Logs.MYNUMBER, cursor.getString(idSimId));
                } else if (!TextUtils.isEmpty(mynumber)) {
                    cv.put(DataProvider.Logs.MYNUMBER, mynumber);
                }
                cvalues.add(cv);
                if (cvalues.size() >= CallMeter.HUNDRED) {
                    cr.bulkInsert(DataProvider.Logs.CONTENT_URI,
                            cvalues.toArray(new ContentValues[cvalues.size()]));
                    Log.d(TAG, "new mms: ", cvalues.size());
                    cvalues.clear();
                }
            } while (cursor.moveToNext());
            if (cvalues.size() > 0) {
                cr.bulkInsert(DataProvider.Logs.CONTENT_URI,
                        cvalues.toArray(new ContentValues[cvalues.size()]));
                Log.d(TAG, "new mms: ", cvalues.size());
            }
        }
        cursor.close();
        Log.d(TAG, "updateMMS(): done");
    }

    /**
     * Delete old logs to make this app fast.
     *
     * @param cr {@link ContentResolver}
     */
    private static void deleteOldLogs(final ContentResolver cr) {
        Log.d(TAG, "delete old logs: date < ", deleteBefore);
        try {
            final int ret = cr.delete(DataProvider.Logs.CONTENT_URI, DataProvider.Logs.DATE
                    + " < ?", new String[]{String.valueOf(deleteBefore)});
            Log.i(TAG, "deleted old logs from internal database: " + ret);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG, "WTF?", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler() {
            @Override
            public void handleMessage(final Message msg) {
                Log.i(TAG, "In handleMessage...");
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        inUpdate = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent == null) {
            Log.w(TAG, "handleIntent(null)");
            return;
        }

        if (CallMeter.hasPermissions(this, Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_SMS)) {
            inUpdate = true;
            handleIntent(intent);
            inUpdate = false;
        }
    }

    private void handleIntent(final Intent intent) {
        assert intent != null;
        long start = System.currentTimeMillis();
        final String a = intent.getAction();
        Log.d(TAG, "handleIntent(action=", a, ")");

        if (BuildConfig.DEBUG_LOG) {
            Log.i(TAG, "check call sim_id");
            checkCallsSimIdColumn(getContentResolver());
            Log.i(TAG, "check sms sim_id");
            checkSmsSimIdColumn(getContentResolver());
        }

        final WakeLock wakelock = acquire(a);

        final Handler h = Plans.getHandler();
        if (h != null) {
            h.sendEmptyMessage(Plans.MSG_BACKGROUND_START_MATCHER);
        }

        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        dateStart = p.getLong(Preferences.PREFS_DATE_BEGIN, DatePreference.DEFAULT_VALUE);
        deleteBefore = Preferences.getDeleteLogsBefore(p);
        splitAt160 = p.getBoolean(Preferences.PREFS_SPLIT_SMS_AT_160, false);
        final boolean showCallInfo = p.getBoolean(Preferences.PREFS_SHOWCALLINFO, false);
        final boolean askForPlan = p.getBoolean(Preferences.PREFS_ASK_FOR_PLAN, false);
        final String delimiter = p.getString(Preferences.PREFS_DELIMITER, " | ");

        final boolean runMatcher = a != null && a.equals(ACTION_RUN_MATCHER);
        boolean shortRun = runMatcher
                || a != null
                && (a.equals(ACTION_SHORT_RUN) || a.equals(ConnectivityManager.CONNECTIVITY_ACTION)
                || a.equals(Intent.ACTION_BOOT_COMPLETED)
                || a.equals(Intent.ACTION_SHUTDOWN) || a.equals(Intent.ACTION_REBOOT) || a
                .equals(Intent.ACTION_DATE_CHANGED));

        Log.d(TAG, "runMatcher: ", runMatcher);
        Log.d(TAG, "shortRun: ", shortRun);

        final ContentResolver cr = getContentResolver();
        boolean showDialog = false;
        if (!shortRun && h != null) {
            final Cursor c = cr.query(DataProvider.Logs.CONTENT_URI,
                    new String[]{DataProvider.Logs.PLAN_ID}, DataProvider.Logs.RULE_ID + " != "
                            + DataProvider.NO_ID + " AND " + DataProvider.Logs.TYPE + " != "
                            + DataProvider.TYPE_DATA, null, null
            );
            assert c != null;
            if (c.getCount() < UNMATHCEDLOGS_TO_SHOW_DIALOG) {
                showDialog = true;
                // skip if no plan is set up
                Cursor c1 = cr.query(DataProvider.Plans.CONTENT_URI,
                        new String[]{DataProvider.Plans.ID}, null, null, null);
                assert c1 != null;
                if (c1.getCount() <= 0) {
                    shortRun = true;
                    showDialog = false;
                }
                c1.close();
                // skip if no rule is set up
                c1 = cr.query(DataProvider.Rules.CONTENT_URI,
                        new String[]{DataProvider.Rules.ID}, null, null, null);
                assert c1 != null;
                if (c1.getCount() <= 0) {
                    shortRun = true;
                    showDialog = false;
                }
                c1.close();
                if (showDialog) {
                    h.sendEmptyMessage(Plans.MSG_BACKGROUND_START_RUNNER);
                }
            }
            c.close();
        }

        updateData(this, shortRun && !runMatcher);
        if (!shortRun || runMatcher) {
            if (deleteBefore > 0L) {
                deleteOldLogs(cr);
            }
            updateCalls(this);
            updateSMS(cr, DataProvider.DIRECTION_IN);
            updateSMS(cr, DataProvider.DIRECTION_OUT);
            updateMMS(this);
            if (RuleMatcher.match(this, showDialog)) {
                StatsAppWidgetProvider.updateWidgets(this);
                LogsAppWidgetProvider.updateWidgets(this);
            }
        } else if (roaming) {
            updateCalls(this);
            updateSMS(cr, DataProvider.DIRECTION_IN);
            updateSMS(cr, DataProvider.DIRECTION_OUT);
            updateMMS(this);
        }

        if (showDialog) {
            h.sendEmptyMessage(Plans.MSG_BACKGROUND_STOP_RUNNER);
        }

        if ((showCallInfo || askForPlan) && a != null
                && a.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            final Cursor c = cr.query(DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
                    DataProvider.Logs.TYPE + " = " + DataProvider.TYPE_CALL, null,
                    DataProvider.Logs.DATE + " DESC");
            if (c != null && c.moveToFirst()) {
                final long id = c.getLong(DataProvider.Logs.INDEX_ID);
                final long date = c.getLong(DataProvider.Logs.INDEX_DATE);
                final long amount = c.getLong(DataProvider.Logs.INDEX_AMOUNT);

                final long now = System.currentTimeMillis();
                if (amount > 0L && date + amount * CallMeter.MILLIS + GAP_FOR_LOGS >= now) {
                    // only show real calls
                    // only show calls made just now
                    final float cost = c.getFloat(DataProvider.Logs.INDEX_COST);
                    final String planname = DataProvider.Plans.getName(cr,
                            c.getLong(DataProvider.Logs.INDEX_PLAN_ID));
                    StringBuilder sb = new StringBuilder();
                    sb.append(Common.prettySeconds(amount, false));
                    if (cost > 0) {
                        String currencyFormat = Preferences.getCurrencyFormat(this);
                        sb.append(delimiter).append(String.format(currencyFormat, cost));
                    }
                    if (planname != null) {
                        sb.insert(0, planname + ": ");
                    } else if (askForPlan) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "launching ask for plan dialog");
                                final Intent i = new Intent(LogRunnerService.this,
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
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                final Toast toast = Toast.makeText(LogRunnerService.this, s,
                                        Toast.LENGTH_LONG);
                                toast.show();
                            }
                        });
                    }
                } else {
                    Log.i(TAG, "skip Toast: amount=" + amount);
                    Log.i(TAG, "skip Toast: date+amount+gap="
                            + (date + amount * CallMeter.MILLIS + GAP_FOR_LOGS));
                    Log.i(TAG, "skip Toast: now            =" + now);
                }
            }
            if (c != null && !c.isClosed()) {
                c.close();
            }
        }

        release(wakelock, h, a);
        long end = System.currentTimeMillis();
        Log.i(TAG, "onHandleIntent(", a, "): ", end - start, "ms");
    }

    /**
     * Acquire {@link WakeLock} and init service.
     *
     * @param a action
     * @return {@link WakeLock}
     */
    private WakeLock acquire(final String a) {
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakelock.acquire();
        Log.i(TAG, "got wakelock");

        if (a != null
                && (a.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED) || a
                .equals(ACTION_SMS))) {
            Log.i(TAG, "sleep for " + WAIT_FOR_LOGS + "ms");
            try {
                Thread.sleep(WAIT_FOR_LOGS);
            } catch (InterruptedException e) {
                Log.e(TAG, "interrupted while waiting for logs", e);
            }
        }

        // update roaming info
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        roaming = tm.isNetworkRoaming();
        Log.d(TAG, "roaming: ", roaming);
        mynumber = tm.getLine1Number();
        Log.d(TAG, "my number: ", mynumber);

        return wakelock;
    }

    /**
     * Release {@link WakeLock} and notify {@link Handler}.
     *
     * @param wakelock {@link WakeLock}
     * @param h        {@link Handler}
     * @param a        action
     */
    private void release(final WakeLock wakelock, final Handler h, final String a) {
        // schedule next update
        if (a == null || !a.equals(ACTION_SHORT_RUN)) {
            LogRunnerReceiver.schedNext(this, null);
        } else {
            LogRunnerReceiver.schedNext(this, a);
        }
        if (h != null) {
            h.sendEmptyMessage(Plans.MSG_BACKGROUND_STOP_MATCHER);
        }
        wakelock.release();
        Log.i(TAG, "wakelock released");
    }
}
