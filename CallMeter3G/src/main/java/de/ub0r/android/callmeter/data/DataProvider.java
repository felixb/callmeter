/*
 * Copyright (C) 2009-2013 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
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

import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.CallLog;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * @author flx
 */
public final class DataProvider extends ContentProvider {

    /**
     * Tag for output.
     */
    private static final String TAG = "DataProvider";

    /**
     * Pattern parsing {@link SQLException}.
     */
    private static final Pattern P = Pattern.compile(": *([^ :,]*)", 0);

    /**
     * Callmeter's package name.
     */
    public static final String PACKAGE = "de.ub0r.android.callmeter";

    /**
     * Authority.
     */
    public static final String AUTHORITY = PACKAGE + ".data";

    /**
     * Name of the {@link SQLiteDatabase}.
     */
    private static final String DATABASE_NAME = "callmeter.db";

    /**
     * Version of the {@link SQLiteDatabase}.
     */
    private static final int DATABASE_VERSION = 35;

    /**
     * Versions of {@link SQLiteDatabase}, which need no unmatch().
     */
    private static final int[] DATABASE_KNOWNGOOD = new int[]{30, 31, 32, 33, 34};

    /**
     * Version of the export file.
     */
    private static final int EXPORT_VERSION = 2;

    /**
     * Separator of values.
     */
    private static final String EXPORT_VALUESEPARATOR = ":#:";

    /**
     * Type of log: title.
     */
    public static final int TYPE_TITLE = 0;

    /**
     * Type of log: spacing.
     */
    public static final int TYPE_SPACING = 1;

    /**
     * Type of log: billmode.
     */
    public static final int TYPE_BILLPERIOD = 2;

    /**
     * Type of log: mixed.
     */
    public static final int TYPE_MIXED = 3;

    /**
     * Type of log: call.
     */
    public static final int TYPE_CALL = 4;

    /**
     * Type of log: sms.
     */
    public static final int TYPE_SMS = 5;

    /**
     * Type of log: mms.
     */
    public static final int TYPE_MMS = 6;

    /**
     * Type of log: data.
     */
    public static final int TYPE_DATA = 7;

    /**
     * Direction of log: in.
     */
    public static final int DIRECTION_IN = 0;

    /**
     * Direction of log: out.
     */
    public static final int DIRECTION_OUT = 1;

    /**
     * Type of limit: none.
     */
    public static final int LIMIT_TYPE_NONE = 0;

    /**
     * Type of limit: units.
     */
    public static final int LIMIT_TYPE_UNITS = 1;

    /**
     * Type of limit: cost.
     */
    public static final int LIMIT_TYPE_COST = 2;

    /**
     * Bill period: one day.
     */
    public static final int BILLPERIOD_DAY = 0;

    /**
     * Bill period: one week.
     */
    public static final int BILLPERIOD_WEEK = 1;

    /**
     * Bill period: two weeks.
     */
    public static final int BILLPERIOD_14D = 2;

    /**
     * Bill period: 15 days.
     */
    public static final int BILLPERIOD_15D = 3;

    /**
     * Bill period: 28 days.
     */
    public static final int BILLPERIOD_28D = 17;

    /**
     * Bill period: 30 days.
     */
    public static final int BILLPERIOD_30D = 4;

    /**
     * Bill period: 31 days.
     */
    public static final int BILLPERIOD_31D = 5;

    /**
     * Bill period: 60 days.
     */
    public static final int BILLPERIOD_60D = 6;

    /**
     * Bill period: 90 days.
     */
    public static final int BILLPERIOD_90D = 7;

    /**
     * Bill period: 1 month.
     */
    public static final int BILLPERIOD_1MONTH = 8;

    /**
     * Bill period: 1 month + 1 Day.
     */
    public static final int BILLPERIOD_1MONTH_1DAY = 9;

    /**
     * Bill period: 2 month.
     */
    public static final int BILLPERIOD_2MONTH = 10;

    /**
     * Bill period: 3 month.
     */
    public static final int BILLPERIOD_3MONTH = 11;

    /**
     * Bill period: 4 month.
     */
    public static final int BILLPERIOD_4MONTH = 12;

    /**
     * Bill period: 5 month.
     */
    public static final int BILLPERIOD_5MONTH = 13;

    /**
     * Bill period: 6 month.
     */
    public static final int BILLPERIOD_6MONTH = 14;

    /**
     * Bill period: 12 month.
     */
    public static final int BILLPERIOD_12MONTH = 15;

    /**
     * Bill period: infinite.
     */
    public static final int BILLPERIOD_INFINITE = 16;

    /**
     * Plan/rule id: not yet calculated.
     */
    public static final int NO_ID = -1;

    /**
     * Plan/rule id: no plan/rule found.
     */
    public static final int NOT_FOUND = -2;

    /**
     * Logs.
     *
     * @author flx
     */
    public static final class Logs {

        /**
         * Table name.
         */
        public static final String TABLE = "logs";

        /**
         * Index in projection: ID.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: ID of plan this log is billed in.
         */
        public static final int INDEX_PLAN_ID = 1;

        /**
         * Index in projection: ID of rule this log was matched.
         */
        public static final int INDEX_RULE_ID = 2;

        /**
         * Index in projection: Type of log.
         */
        public static final int INDEX_TYPE = 3;

        /**
         * Index in projection: Direction of log.
         */
        public static final int INDEX_DIRECTION = 4;

        /**
         * Index in projection: Date.
         */
        public static final int INDEX_DATE = 5;

        /**
         * Index in projection: Amount.
         */
        public static final int INDEX_AMOUNT = 6;

        /**
         * Index in projection: Billed amount.
         */
        public static final int INDEX_BILL_AMOUNT = 7;

        /**
         * Index in projection: Remote part.
         */
        public static final int INDEX_REMOTE = 8;

        /**
         * Index in projection: Roamed?
         */
        public static final int INDEX_ROAMED = 9;

        /**
         * Index in projection: Cost.
         */
        public static final int INDEX_COST = 10;

        /**
         * Index in projection: Cost (free).
         */
        public static final int INDEX_FREE = 11;

        /**
         * Index in projection: my own number.
         */
        public static final int INDEX_MYNUMBER = 12;

        /**
         * Index in projection: Plan name.
         */
        public static final int INDEX_PLAN_NAME = 13;

        /**
         * Index in projection: Rule name.
         */
        public static final int INDEX_RULE_NAME = 14;

        /**
         * Index in projection: Plan type.
         */
        public static final int INDEX_PLAN_TYPE = 15;

        /**
         * Index in projection - sum: Type of log.
         */
        public static final int INDEX_SUM_TYPE = 0;

        /**
         * Index in projection - sum: id of plan.
         */
        public static final int INDEX_SUM_PLAN_ID = 1;

        /**
         * Index in projection - sum: type of plan.
         */
        public static final int INDEX_SUM_PLAN_TYPE = 2;

        /**
         * Index in projection - sum: Amount.
         */
        public static final int INDEX_SUM_AMOUNT = 3;

        /**
         * Index in projection - sum: Billed amount.
         */
        public static final int INDEX_SUM_BILL_AMOUNT = 4;

        /**
         * Index in projection - sum: Cost.
         */
        public static final int INDEX_SUM_COST = 5;

        /**
         * Index in projection - sum: Cost (free).
         */
        public static final int INDEX_SUM_FREE = 6;

        /**
         * Index in projection - sum: count.
         */
        public static final int INDEX_SUM_COUNT = 7;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * ID of plan this log is billed in.
         */
        public static final String PLAN_ID = "_plan_id";

        /**
         * ID of rule this log was matched.
         */
        public static final String RULE_ID = "_rule_id";

        /**
         * Type of log.
         */
        public static final String TYPE = "_type";

        /**
         * Direction of log.
         */
        public static final String DIRECTION = "_direction";

        /**
         * Date of log.
         */
        public static final String DATE = "_date";

        /**
         * Amount.
         */
        public static final String AMOUNT = "_amount";

        /**
         * Billed amount.
         */
        public static final String BILL_AMOUNT = "_bill_amount";

        /**
         * Remote part.
         */
        public static final String REMOTE = "_remote";

        /**
         * Roamed?
         */
        public static final String ROAMED = "_roamed";

        /**
         * Cost.
         */
        public static final String COST = "_logs_cost";

        /**
         * Cost (free).
         */
        public static final String FREE = "_logs_cost_free";

        /**
         * Type of plan. Only available in sum query.
         */
        public static final String PLAN_TYPE = "_plan_type";

        /**
         * My own number.
         */
        public static final String MYNUMBER = "_mynumber";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, PLAN_ID, RULE_ID, TYPE,
                DIRECTION, DATE, AMOUNT, BILL_AMOUNT, REMOTE, ROAMED, COST, FREE, MYNUMBER};

        /**
         * Projection used for join query.
         */
        public static final String[] PROJECTION_JOIN;

        static {
            final int l = PROJECTION.length;
            PROJECTION_JOIN = new String[l + 3];
            for (int i = 0; i < l; i++) {
                PROJECTION_JOIN[i] = TABLE + "." + PROJECTION[i] + " as " + PROJECTION[i];
            }
            PROJECTION_JOIN[l] = Plans.TABLE + "." + Plans.NAME + " as " + Plans.NAME;
            PROJECTION_JOIN[l + 1] = Rules.TABLE + "." + Rules.NAME + " as " + Rules.NAME;
            PROJECTION_JOIN[l + 2] = Plans.TABLE + "." + Plans.TYPE + " as " + Plans.TYPE;
        }

        /**
         * Projection used for query - sum.
         */
        public static final String[] PROJECTION_SUM = new String[]{TYPE, PLAN_ID,
                Plans.TABLE + "." + Plans.TYPE + " AS " + PLAN_TYPE, "sum(" + AMOUNT + ")",
                "sum(" + BILL_AMOUNT + ")", "sum(" + COST + ")", "sum(" + FREE + ")",
                "count(" + PLAN_ID + ")"};

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/logs");

        /**
         * Content {@link Uri} logs joined with plans and rules.
         */
        public static final Uri CONTENT_URI_JOIN = Uri.parse("content://" + AUTHORITY
                + "/logs/join");

        /**
         * Content {@link Uri} - sum.
         */
        public static final Uri SUM_URI = Uri.parse("content://" + AUTHORITY + "/logs/sum");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.log";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.log";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + PLAN_ID + " LONG, " + RULE_ID + " LONG, " + TYPE + " INTEGER, " + DIRECTION
                    + " INTEGER, " + DATE + " LONG, " + AMOUNT + " LONG, " + BILL_AMOUNT
                    + " FLOAT, " + REMOTE + " TEXT, " + ROAMED + " INTEGER, " + COST + " FLOAT, "
                    + FREE + " FLOAT," + MYNUMBER + " TEXT" + ");");
            db.execSQL("CREATE INDEX " + TABLE + "_idx on " + TABLE + " (" + ID + "," + PLAN_ID
                    + "," + DATE + ")");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private Logs() {
            // nothing here.
        }

        /**
         * Clean a number from all but [?*#+0-9].
         *
         * @param number  dirty number
         * @param pattern if true, '%' is added to list of allowed chars
         * @return cleaned number
         */
        public static String cleanNumber(final String number, final boolean pattern) {
            if (number == null) {
                return null;
            }
            String n;
            if (pattern) {
                n = number.replaceAll("[^?*#+0-9%]", "");
            } else {
                n = number.replaceAll("[^?*#+0-9]", "");
            }
            if (n.startsWith("00") && !n.startsWith("00800")) {
                return "+" + n.substring(2);
            }
            return n;
        }
    }

    /**
     * WebSMS.
     *
     * @author flx
     */
    public static final class WebSMS {

        /**
         * Table name.
         */
        private static final String TABLE = "websms";

        /**
         * Index in projection: ID.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: Connector's name.
         */
        public static final int INDEX_CONNECTOR = 1;

        /**
         * Index in projection: date.
         */
        public static final int INDEX_DATE = 2;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * Connector's name.
         */
        public static final String CONNECTOR = "_connector";

        /**
         * Date.
         */
        public static final String DATE = "_date";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, CONNECTOR, DATE};

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/websms");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.websms";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.websms";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " LONG, " + CONNECTOR + " TEXT,"
                    + DATE + " LONG" + ");");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private WebSMS() {
            // nothing here.
        }
    }

    /**
     * WebSMS.
     *
     * @author flx
     */
    public static final class SipCall {

        /**
         * Table name.
         */
        private static final String TABLE = "sipcall";

        /**
         * Index in projection: ID.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: Provider's name.
         */
        public static final int INDEX_PROVIDER = 1;

        /**
         * Index in projection: date.
         */
        public static final int INDEX_DATE = 2;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * Provider's name.
         */
        public static final String PROVIDER = "_connector";

        /**
         * Date.
         */
        public static final String DATE = "_date";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, PROVIDER, DATE};

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/sipcall");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.sipcall";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.sipcall";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " LONG, " + PROVIDER + " TEXT," + DATE
                    + " LONG" + ");");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Hide constructor.
         */
        private SipCall() {
            // nothing here.
        }
    }

    /**
     * Plans.
     *
     * @author flx
     */
    public static final class Plans {

        /**
         * A plan.
         */
        public static final class Plan {

            /**
             * Prefix for loading plans from {@link SharedPreferences}.
             */
            private static final String PREF_PREFIX = "_plans_";

            /**
             * Id of plan.
             */
            public final long id;

            /**
             * Type of plan.
             */
            public final int type;

            /**
             * Plans's name.
             */
            public final String name;

            /**
             * Plans's short name.
             */
            public final String sname;

            /**
             * Bill period.
             */
            public final int billperiod;

            /**
             * Bill day.
             */
            public final long billday;

            /**
             * Next bill day.
             */
            public final long nextbillday;

            /**
             * Current time used for query.
             */
            public final long now;

            /**
             * Type of limit.
             */
            public final int limittype;

            /**
             * Limit.
             */
            public final long limit;

            /**
             * Position in limit.
             */
            public final long limitPos;

            /**
             * Used limit.
             */
            public final float usage;

            /**
             * Cost per plan.
             */
            private final float cpp;

            /**
             * Sum of cost.
             */
            public final float cost;

            /**
             * Sum of free cost.
             */
            public final float free;

            /**
             * Sum of todays count.
             */
            public final int tdCount;

            /**
             * Sum of todays billed amount.
             */
            public final float tdBa;

            /**
             * Sum of this bill period's count.
             */
            public final int bpCount;

            /**
             * Sum of this bill period's billed amount.
             */
            public final float bpBa;

            /**
             * Sum of this all time's count.
             */
            public final int atCount;

            /**
             * Sum of this all time's billed amount.
             */
            public final float atBa;

            /**
             * Has billed amount.
             */
            public final boolean hasBa;

            /**
             * Default Constructor from {@link Cursor}. Use this method only with {@link Cursor}s
             * against {@link Plan}.CONTENT_URI_SUM with PROJECTION_SUM.
             *
             * @param cursor {@link Cursor}
             */
            public Plan(final Cursor cursor) {
                Log.d(TAG, "new Plan(", cursor, ")");
                id = cursor.getLong(INDEX_ID);
                type = cursor.getInt(INDEX_TYPE);
                name = cursor.getString(INDEX_NAME);
                sname = cursor.getString(INDEX_SHORTNAME);
                billperiod = cursor.getInt(INDEX_BILLPERIOD);
                if (type == TYPE_SPACING || type == TYPE_TITLE) {
                    billday = -1;
                    nextbillday = -1;
                    limittype = -1;
                    limit = -1;
                    limitPos = -1;
                    cpp = 0;
                    cost = 0f;
                    free = 0f;
                    tdCount = 0;
                    tdBa = 0f;
                    bpCount = 0;
                    bpBa = 0f;
                    atCount = 0;
                    atBa = 0f;
                    now = -1L;
                    hasBa = false;
                } else {
                    cost = cursor.getFloat(INDEX_SUM_COST);
                    free = cursor.getFloat(INDEX_SUM_FREE);
                    tdCount = cursor.getInt(INDEX_SUM_TD_COUNT);
                    tdBa = cursor.getFloat(INDEX_SUM_TD_BILLED_AMOUNT);
                    bpCount = cursor.getInt(INDEX_SUM_BP_COUNT);
                    bpBa = cursor.getFloat(INDEX_SUM_BP_BILLED_AMOUNT);
                    atCount = cursor.getInt(INDEX_SUM_AT_COUNT);
                    atBa = cursor.getFloat(INDEX_SUM_AT_BILLED_AMOUNT);
                    now = cursor.getLong(INDEX_SUM_NOW);
                    billday = cursor.getLong(INDEX_SUM_BILLDAY);
                    nextbillday = cursor.getLong(INDEX_SUM_NEXTBILLDAY);
                    cpp = cursor.getFloat(INDEX_SUM_CPP);
                    if (type == TYPE_BILLPERIOD) {
                        limittype = -1;
                        hasBa = true;
                        if (billperiod == DataProvider.BILLPERIOD_INFINITE) {
                            limitPos = 0;
                            limit = 0;
                        } else {
                            limit = (nextbillday - billday)
                                    / Utils.MINUTES_IN_MILLIS;
                            if (nextbillday - now == 1) {
                                // fix issue: #661, skip last millisecond to
                                // show 100% of billing period usage
                                limitPos = limit;
                            } else {
                                limitPos = (now - billday) / Utils.MINUTES_IN_MILLIS;
                            }
                        }
                    } else {
                        limittype = cursor.getInt(INDEX_LIMIT_TYPE);
                        limit = getLimit(type, limittype,
                                cursor.getFloat(INDEX_LIMIT));
                        limitPos = getUsed(type, limittype, bpBa, cost);
                        hasBa = type != TYPE_MIXED
                                || cursor.getInt(INDEX_SUM_MIXED_UNITS_CALL) != 0
                                || cursor.getInt(INDEX_SUM_MIXED_UNITS_DATA) != 0
                                || cursor.getInt(INDEX_SUM_MIXED_UNITS_MMS) != 0
                                || cursor.getInt(INDEX_SUM_MIXED_UNITS_SMS) != 0;
                    }
                }
                if (limitPos <= 0) {
                    usage = 0;
                } else {
                    usage = (float) limitPos / (float) limit;
                }
                Log.v(TAG, "new Plan(): pid=", id);
                Log.v(TAG, "new Plan(): count=", bpCount);
                Log.v(TAG, "new Plan(): ba=", bpBa);
                Log.v(TAG, "new Plan(): at.count=", atCount);
                Log.v(TAG, "new Plan(): at.ba=", atBa);
                Log.v(TAG, "new Plan(): cost=", cost);
                Log.v(TAG, "new Plan(): free=", free);
            }

            /**
             * Default Constructor from {@link Cursor} supported by cached values from {@link
             * SharedPreferences}.
             *
             * @param cursor {@link Cursor}
             * @param p      {@link SharedPreferences}
             */
            public Plan(final Cursor cursor, final SharedPreferences p) {
                Log.d(TAG, "new Plan(", cursor, ", ", p, ")");
                id = cursor.getLong(INDEX_ID);
                type = cursor.getInt(INDEX_TYPE);
                name = cursor.getString(INDEX_NAME);
                sname = cursor.getString(INDEX_SHORTNAME);
                billperiod = cursor.getInt(INDEX_BILLPERIOD);
                if (type == TYPE_SPACING || type == TYPE_TITLE) {
                    billday = -1;
                    nextbillday = -1;
                    limittype = -1;
                    limit = -1;
                    limitPos = -1;
                    cpp = 0;
                    cost = 0f;
                    free = 0f;
                    tdCount = 0;
                    tdBa = 0f;
                    bpCount = 0;
                    bpBa = 0f;
                    atCount = 0;
                    atBa = 0f;
                    now = -1L;
                    hasBa = false;
                } else {
                    cost = p.getFloat(PREF_PREFIX + SUM_COST + id, 0f);
                    free = p.getFloat(PREF_PREFIX + SUM_FREE + id, 0f);
                    tdCount = p.getInt(PREF_PREFIX + SUM_TD_COUNT + id, 0);
                    tdBa = p.getFloat(PREF_PREFIX + SUM_TD_BILLED_AMOUNT + id, 0f);
                    bpCount = p.getInt(PREF_PREFIX + SUM_BP_COUNT + id, 0);
                    bpBa = p.getFloat(PREF_PREFIX + SUM_BP_BILLED_AMOUNT + id, 0f);
                    atCount = p.getInt(PREF_PREFIX + SUM_AT_COUNT + id, 0);
                    atBa = p.getFloat(PREF_PREFIX + SUM_AT_BILLED_AMOUNT + id, 0f);
                    now = p.getLong(PREF_PREFIX + SUM_NOW + id, -1L);
                    billday = p.getLong(PREF_PREFIX + SUM_BILLDAY + id, 0L);
                    nextbillday = p.getLong(PREF_PREFIX + SUM_NEXTBILLDAY + id, 0L);
                    cpp = p.getFloat(PREF_PREFIX + SUM_CPP + id, 0f);

                    if (type == TYPE_BILLPERIOD) {
                        limittype = -1;
                        hasBa = true;
                        if (billperiod == DataProvider.BILLPERIOD_INFINITE) {
                            limitPos = 0;
                            limit = 0;
                        } else {
                            limit = (nextbillday - billday)
                                    / Utils.MINUTES_IN_MILLIS;
                            if (nextbillday - now == 1) {
                                // fix issue: #661, skip last millisecond to
                                // show 100% of billing period usage
                                limitPos = limit;
                            } else {
                                limitPos = (now - billday) / Utils.MINUTES_IN_MILLIS;
                            }
                        }
                    } else {
                        limittype = cursor.getInt(INDEX_LIMIT_TYPE);
                        limit = getLimit(type, limittype,
                                cursor.getFloat(INDEX_LIMIT));
                        limitPos = getUsed(type, limittype, bpBa, cost);
                        hasBa = type != TYPE_MIXED
                                || cursor.getInt(INDEX_BASIC_MIXED_UNITS_CALL) != 0
                                || cursor.getInt(INDEX_BASIC_MIXED_UNITS_DATA) != 0
                                || cursor.getInt(INDEX_BASIC_MIXED_UNITS_MMS) != 0
                                || cursor.getInt(INDEX_BASIC_MIXED_UNITS_SMS) != 0;
                    }
                }
                if (limitPos <= 0) {
                    usage = 0;
                } else {
                    usage = (float) limitPos / (float) limit;
                }
            }

            /**
             * Get a {@link Plan} from plan's id.
             *
             * @param cr          {@link ContentResolver}
             * @param planid      {@link Plan}'s id
             * @param now         time of query
             * @param needToday   need today stats?
             * @param needAllTime need all time stats?
             * @return {@link Plan}
             */
            public static Plan getPlan(final ContentResolver cr, final long planid, final long now,
                    final boolean needToday, final boolean needAllTime) {
                Uri uri = CONTENT_URI_SUM;
                if (now >= 0) {
                    uri = uri.buildUpon().appendQueryParameter(PARAM_DATE, String.valueOf(now))
                            .appendQueryParameter(PARAM_HIDE_TODAY, String.valueOf(needToday))
                            .appendQueryParameter(PARAM_HIDE_ALLTIME, String.valueOf(needAllTime))
                            .build();
                    assert uri != null;
                }
                Cursor c = cr.query(uri, PROJECTION_SUM, TABLE + "." + ID + "=?",
                        new String[]{String.valueOf(planid)}, null);
                Plan ret = null;
                if (c != null) {
                    if (c.moveToFirst()) {
                        ret = new Plan(c);
                    }
                    c.close();
                }
                return ret;
            }

            /**
             * Get a {@link Plan} from plan's id.
             *
             * @param cr          {@link ContentResolver}
             * @param planid      {@link Plan}'s id
             * @param now         time of query
             * @param needToday   need today stats?
             * @param needAllTime need all time stats?
             * @return {@link Plan}
             */
            public static Plan getPlan(final ContentResolver cr, final long planid,
                    final Calendar now, final boolean needToday, final boolean needAllTime) {
                if (now == null) {
                    return getPlan(cr, planid, -1L, needToday, needAllTime);
                } else {
                    return getPlan(cr, planid, now.getTimeInMillis(), needToday, needAllTime);
                }
            }

            /**
             * Save Plan to {@link SharedPreferences} for caching.
             *
             * @param e {@link Editor}
             * @return {@link Editor}
             */
            public Editor save(final Editor e) {
                Log.d(TAG, "save(): ", id);

                e.putFloat(PREF_PREFIX + SUM_COST + id, cost);
                e.putFloat(PREF_PREFIX + SUM_FREE + id, free);
                e.putInt(PREF_PREFIX + SUM_TD_COUNT + id, tdCount);
                e.putFloat(PREF_PREFIX + SUM_TD_BILLED_AMOUNT + id, tdBa);
                e.putInt(PREF_PREFIX + SUM_BP_COUNT + id, bpCount);
                e.putFloat(PREF_PREFIX + SUM_BP_BILLED_AMOUNT + id, bpBa);
                e.putInt(PREF_PREFIX + SUM_AT_COUNT + id, atCount);
                e.putFloat(PREF_PREFIX + SUM_AT_BILLED_AMOUNT + id, atBa);
                e.putLong(PREF_PREFIX + SUM_NOW + id, now);
                e.putLong(PREF_PREFIX + SUM_BILLDAY + id, billday);
                e.putLong(PREF_PREFIX + SUM_NEXTBILLDAY + id, nextbillday);
                e.putFloat(PREF_PREFIX + SUM_CPP + id, cpp);

                return e;
            }

            /**
             * Get last full day of this billing period.
             */
            public long getLastFullBillDay() {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(nextbillday);
                c.set(Calendar.MILLISECOND, 0);
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MINUTE, 0);
                c.set(Calendar.HOUR_OF_DAY, 0);
                c.add(Calendar.HOUR_OF_DAY, -1);
                return c.getTimeInMillis();
            }

            /**
             * Get bill day.
             *
             * @param last true to get the last full day of a bill period, else the first
             */
            public long getBillDay(final boolean last) {
                if (last) {
                    return getLastFullBillDay();
                } else {
                    return billday;
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public String toString() {
                return "plan:" + id + ":" + name;
            }

            /**
             * Get usage of {@link Plan}'s bill period.
             *
             * @return usage of bill period
             */
            public float getBillPlanUsage() {
                long blength = nextbillday - billday;
                if (blength <= 0L) {
                    return -1f;
                }
                return ((float) (now - billday)) / (float) blength;
            }

            /**
             * Get cost and cost per plan.
             *
             * @return cost + cpp - free
             */
            public float getAccumCost() {
                return cost + cpp - free;
            }

            /**
             * Get remaining balance.
             *
             * @return balance - cost for bill periods; else cost + cpp
             */
            public float getAccumCostPrepaid() {
                if (type == TYPE_BILLPERIOD) {
                    return cpp - cost;
                } else {
                    return cost + cpp;
                }
            }

            /**
             * Get free cost.
             *
             * @return free cost
             */
            public float getFree() {
                return free;
            }
        }

        /**
         * Table name.
         */
        public static final String TABLE = "plans";

        /**
         * Parameter for query: date.
         */
        public static final String PARAM_DATE = "date";

        /**
         * Parameter for query: hide zero plans.
         */
        public static final String PARAM_HIDE_ZERO = "hide_zero";

        /**
         * Parameter for query: hide zero cost plans.
         */
        public static final String PARAM_HIDE_NOCOST = "hide_nocost";

        /**
         * Parameter for query: hide today stats.
         */
        public static final String PARAM_HIDE_TODAY = "hide_today";

        /**
         * Parameter for query: hide all time stats.
         */
        public static final String PARAM_HIDE_ALLTIME = "hide_alltime";

        /**
         * Index in projection: id.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: name.
         */
        public static final int INDEX_NAME = 1;

        /**
         * Index in projection: short name.
         */
        public static final int INDEX_SHORTNAME = 2;

        /**
         * Index in projection: type.
         */
        public static final int INDEX_TYPE = 3;

        /**
         * Index in projection: Type of limit.
         */
        public static final int INDEX_LIMIT_TYPE = 4;

        /**
         * Index in projection: limit.
         */
        public static final int INDEX_LIMIT = 5;

        /**
         * Index in projection: type of billperiod.
         */
        public static final int INDEX_BILLPERIOD = 6;

        /**
         * Index in projection: Billmode.
         */
        public static final int INDEX_BILLMODE = 7;

        /**
         * Index in projection: Billday.
         */
        public static final int INDEX_BILLDAY = 8;

        /**
         * Index in projection: Cost per item.
         */
        public static final int INDEX_COST_PER_ITEM = 9;

        /**
         * Index in projection: Cost per amount1.
         */
        public static final int INDEX_COST_PER_AMOUNT1 = 10;

        /**
         * Index in projection: Cost per amount2.
         */
        public static final int INDEX_COST_PER_AMOUNT2 = 11;

        /**
         * Index in projection: Cost per item in limit.
         */
        public static final int INDEX_COST_PER_ITEM_IN_LIMIT = 12;

        /**
         * Index in projection: Cost per amount1 in limit.
         */
        public static final int INDEX_COST_PER_AMOUNT_IN_LIMIT1 = 13;

        /**
         * Index in projection: Cost per amount2 in limit.
         */
        public static final int INDEX_COST_PER_AMOUNT_IN_LIMIT2 = 14;

        /**
         * Index in projection: Cost per plan.
         */
        public static final int INDEX_COST_PER_PLAN = 15;

        /**
         * Index in projection: Mixed units for call.
         */
        public static final int INDEX_MIXED_UNITS_CALL = 16;

        /**
         * Index in projection: Mixed units for sms.
         */
        public static final int INDEX_MIXED_UNITS_SMS = 17;

        /**
         * Index in projection: Mixed units for mms.
         */
        public static final int INDEX_MIXED_UNITS_MMS = 18;

        /**
         * Index in projection: Mixed units for data.
         */
        public static final int INDEX_MIXED_UNITS_DATA = 19;

        /**
         * Index in projection: id of billperiod.
         */
        public static final int INDEX_BILLPERIOD_ID = 20;

        /**
         * Index in projection: next alert.
         */
        public static final int INDEX_NEXT_ALERT = 21;

        /**
         * Index in projection: strip first seconds.
         */
        public static final int INDEX_STRIP_SECONDS = 22;

        /**
         * Index in projection: strip all but first seconds.
         */
        public static final int INDEX_STRIP_PAST = 23;

        /**
         * Index in projection: merged plans.
         */
        public static final int INDEX_MERGED_PLANS = 24;

        /**
         * Index in projection: sum, now.
         */
        public static final int INDEX_SUM_NOW = 9;

        /**
         * Index in projection: sum, last bill day.
         */
        public static final int INDEX_SUM_BILLDAY = 10;

        /**
         * Index in projection: sum: next bill day.
         */
        public static final int INDEX_SUM_NEXTBILLDAY = 11;

        /**
         * Index in projection: sum count for today.
         */
        public static final int INDEX_SUM_TD_COUNT = 12;

        /**
         * Index in projection: sum billed amount for today.
         */
        public static final int INDEX_SUM_TD_BILLED_AMOUNT = 13;

        /**
         * Index in projection: sum count for this bill period.
         */
        public static final int INDEX_SUM_BP_COUNT = 14;

        /**
         * Index in projection: sum billed amount for this bill period.
         */
        public static final int INDEX_SUM_BP_BILLED_AMOUNT = 15;

        /**
         * Index in projection: sum count.
         */
        public static final int INDEX_SUM_AT_COUNT = 16;

        /**
         * Index in projection: sum billed amount.
         */
        public static final int INDEX_SUM_AT_BILLED_AMOUNT = 17;

        /**
         * Index in projection: sum cost for all plans.
         */
        public static final int INDEX_SUM_CPP = 18;

        /**
         * Index in projection: sum cost for this bill period.
         */
        public static final int INDEX_SUM_COST = 19;

        /**
         * Index in projection: sum free cost for this bill period.
         */
        public static final int INDEX_SUM_FREE = 20;

        /**
         * Index in projection: Mixed units for call.
         */
        public static final int INDEX_SUM_MIXED_UNITS_CALL = 21;

        /**
         * Index in projection: Mixed units for data.
         */
        public static final int INDEX_SUM_MIXED_UNITS_DATA = 22;

        /**
         * Index in projection: Mixed units for mms.
         */
        public static final int INDEX_SUM_MIXED_UNITS_MMS = 23;

        /**
         * Index in projection: Mixed units for sms.
         */
        public static final int INDEX_SUM_MIXED_UNITS_SMS = 24;

        /**
         * Index in projection: Mixed units for call.
         */
        public static final int INDEX_BASIC_MIXED_UNITS_CALL = INDEX_BILLPERIOD + 2;

        /**
         * Index in projection: Mixed units for data.
         */
        public static final int INDEX_BASIC_MIXED_UNITS_DATA = INDEX_BASIC_MIXED_UNITS_CALL + 1;

        /**
         * Index in projection: Mixed units for mms.
         */
        public static final int INDEX_BASIC_MIXED_UNITS_MMS = INDEX_BASIC_MIXED_UNITS_DATA + 1;

        /**
         * Index in projection: Mixed units for sms.
         */
        public static final int INDEX_BASIC_MIXED_UNITS_SMS = INDEX_BASIC_MIXED_UNITS_MMS + 1;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * Order.
         */
        public static final String ORDER = "_order";

        /**
         * Name.
         */
        public static final String NAME = "_plan_name";

        /**
         * Short name.
         */
        public static final String SHORTNAME = "_shortname";

        /**
         * Type of log.
         */
        public static final String TYPE = "_plan_type";

        /**
         * Type of limit.
         */
        public static final String LIMIT_TYPE = "_limit_type";

        /**
         * Limit.
         */
        public static final String LIMIT = "_limit";

        /**
         * Type of billperiod.
         */
        public static final String BILLPERIOD = "_billperiod";

        /**
         * Billmode.
         */
        public static final String BILLMODE = "_billmode";

        /**
         * Billday.
         */
        public static final String BILLDAY = "_billday";

        /**
         * Id of billperiod.
         */
        public static final String BILLPERIOD_ID = "_billperiod_id";

        /**
         * Cost per item.
         */
        public static final String COST_PER_ITEM = "_cost_per_item";

        /**
         * Cost per amount1.
         */
        public static final String COST_PER_AMOUNT1 = "_cost_per_amount1";

        /**
         * Cost per amount2.
         */
        public static final String COST_PER_AMOUNT2 = "_cost_per_amount2";

        /**
         * Cost per item in limit.
         */
        public static final String COST_PER_ITEM_IN_LIMIT = "_cost_per_item_in_limit";

        /**
         * Cost per amount1 in limit.
         */
        public static final String COST_PER_AMOUNT_IN_LIMIT1 = "_cost_per_amount_in_limit1";

        /**
         * Cost per amount2 in limit.
         */
        public static final String COST_PER_AMOUNT_IN_LIMIT2 = "_cost_per_amount_in_limit2";

        /**
         * Cost per plan.
         */
        public static final String COST_PER_PLAN = "_cost_per_plan";

        /**
         * Mixed units for call.
         */
        public static final String MIXED_UNITS_CALL = "_mixed_units_call";

        /**
         * Mixed units for sms.
         */
        public static final String MIXED_UNITS_SMS = "_mixed_units_sms";

        /**
         * Mixed units for mms.
         */
        public static final String MIXED_UNITS_MMS = "_mixed_units_mms";

        /**
         * Mixed units for data.
         */
        public static final String MIXED_UNITS_DATA = "_mixed_units_data";

        /**
         * Next alert.
         */
        public static final String NEXT_ALERT = "_next_alert";

        /**
         * Strip first seconds.
         */
        public static final String STRIP_SECONDS = "_strip_seconds";

        /**
         * Strip anything but first seconds.
         */
        public static final String STRIP_PAST = "_strip_past";

        /**
         * Merged plans.
         */
        public static final String MERGED_PLANS = "_merged_plans";

        /**
         * Sum: now.
         */
        public static final String SUM_NOW = "NOW";

        /**
         * Sum: last bill day.
         */
        public static final String SUM_BILLDAY = "BILLDAY";

        /**
         * Sum: next bill day.
         */
        public static final String SUM_NEXTBILLDAY = "NEXTBILLDAY";

        /**
         * Sum: TODAY.
         */
        public static final String SUM_TODAY = "TODAY";

        /**
         * Sum: count for this bill period.
         */
        public static final String SUM_BP_COUNT = "SUM_BP_COUNT";

        /**
         * Sum: billed amount for this bill period.
         */
        public static final String SUM_BP_BILLED_AMOUNT = "SUM_BP_BA";

        /**
         * Sum: count.
         */
        public static final String SUM_AT_COUNT = "SUM_AT_COUNT";

        /**
         * Sum: billed amount.
         */
        public static final String SUM_AT_BILLED_AMOUNT = "SUM_AT_BA";

        /**
         * Sum: count for today.
         */
        public static final String SUM_TD_COUNT = "SUM_TD_COUNT";

        /**
         * Sum: billed amount for today.
         */
        public static final String SUM_TD_BILLED_AMOUNT = "SUM_TD_BA";

        /**
         * Sum: cost for all plans.
         */
        public static final String SUM_CPP = "SUM_CPP";

        /**
         * Sum: cost for this bill period.
         */
        public static final String SUM_COST = "SUM_COST";

        /**
         * Sum: free cost for this bill period.
         */
        public static final String SUM_FREE = "SUM_FREE";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, NAME, SHORTNAME, TYPE,
                LIMIT_TYPE, LIMIT, BILLPERIOD, BILLMODE, BILLDAY, COST_PER_ITEM, COST_PER_AMOUNT1,
                COST_PER_AMOUNT2, COST_PER_ITEM_IN_LIMIT, COST_PER_AMOUNT_IN_LIMIT1,
                COST_PER_AMOUNT_IN_LIMIT2, COST_PER_PLAN, MIXED_UNITS_CALL, MIXED_UNITS_SMS,
                MIXED_UNITS_MMS, MIXED_UNITS_DATA, BILLPERIOD_ID, NEXT_ALERT, STRIP_SECONDS,
                STRIP_PAST, MERGED_PLANS, ORDER};

        /**
         * Projection used for basic query.
         */
        public static final String[] PROJECTION_BASIC = new String[]{ID, NAME, SHORTNAME, TYPE,
                LIMIT_TYPE, LIMIT, BILLPERIOD, ORDER, MIXED_UNITS_CALL, MIXED_UNITS_DATA,
                MIXED_UNITS_MMS, MIXED_UNITS_SMS};

        /**
         * Projection used for sum query.
         */
        public static final String[] PROJECTION_SUM = new String[]{
                TABLE + "." + ID + " AS " + ID,
                TABLE + "." + NAME + " AS " + NAME,
                TABLE + "." + SHORTNAME + " AS " + SHORTNAME,
                TABLE + "." + TYPE + " AS " + TYPE,
                TABLE + "." + LIMIT_TYPE + " AS " + LIMIT_TYPE,
                TABLE + "." + LIMIT + " AS " + LIMIT,
                TABLE + "." + BILLPERIOD + " AS " + BILLPERIOD,
                TABLE + "." + BILLMODE + " AS " + BILLMODE,
                TABLE + "." + BILLDAY + " AS " + BILLDAY,
                "{" + SUM_NOW + "} AS " + SUM_NOW,
                "{" + SUM_BILLDAY + "} AS " + SUM_BILLDAY,
                "{" + SUM_NEXTBILLDAY + "} AS " + SUM_NEXTBILLDAY,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + " is null or " + Logs.TABLE + "."
                        + Logs.DATE + "<{" + SUM_TODAY + "} or " + Logs.TABLE + "." + Logs.DATE
                        + ">{" + SUM_NOW + "} THEN 0 ELSE 1 END) as " + SUM_TD_COUNT,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + "<{" + SUM_TODAY + "} or "
                        + Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW + "} THEN 0 WHEN " + TABLE
                        + "." + MERGED_PLANS + " is null or " + TABLE + "." + TYPE + "!="
                        + TYPE_MIXED + " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT + " WHEN  "
                        + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_CALL + " THEN " + Logs.TABLE
                        + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_CALL + "/60"
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_SMS + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_SMS
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_MMS + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_MMS
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_DATA + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "."
                        + MIXED_UNITS_DATA + "/" + CallMeter.BYTE_MB + " ELSE " + Logs.TABLE + "."
                        + Logs.BILL_AMOUNT + " END) AS " + SUM_TD_BILLED_AMOUNT,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + " is null or " + Logs.TABLE + "."
                        + Logs.DATE + "<={" + SUM_BILLDAY + "} or " + Logs.TABLE + "." + Logs.DATE
                        + ">{" + SUM_NOW + "} THEN 0 ELSE 1 END) as " + SUM_BP_COUNT,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + "<={" + SUM_BILLDAY + "} or "
                        + Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW + "} THEN 0 WHEN " + TABLE
                        + "." + MERGED_PLANS + " is null or " + TABLE + "." + TYPE + "!="
                        + TYPE_MIXED + " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT + " WHEN  "
                        + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_CALL + " THEN " + Logs.TABLE
                        + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_CALL + "/60"
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_SMS + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_SMS
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_MMS + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_MMS
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_DATA + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "."
                        + MIXED_UNITS_DATA + "/" + CallMeter.BYTE_MB + " ELSE " + Logs.TABLE + "."
                        + Logs.BILL_AMOUNT + " END) AS " + SUM_BP_BILLED_AMOUNT,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + " is null or " + Logs.TABLE + "."
                        + Logs.DATE + ">{" + SUM_NOW + "} THEN 0 ELSE 1 END) as " + SUM_AT_COUNT,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW + "} THEN 0 WHEN "
                        + TABLE + "." + MERGED_PLANS + " is null or " + TABLE + "." + TYPE + "!="
                        + TYPE_MIXED + " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT + " WHEN  "
                        + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_CALL + " THEN " + Logs.TABLE
                        + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_CALL + "/60"
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_SMS + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_SMS
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_MMS + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_MMS
                        + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_DATA + " THEN "
                        + Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "."
                        + MIXED_UNITS_DATA + "/" + CallMeter.BYTE_MB + " ELSE " + Logs.TABLE + "."
                        + Logs.BILL_AMOUNT + " END) AS " + SUM_AT_BILLED_AMOUNT,
                "(CASE WHEN " + TABLE + "." + TYPE + "=" + TYPE_BILLPERIOD + " THEN (CASE WHEN "
                        + TABLE + "." + COST_PER_PLAN + " is null  THEN 0 ELSE " + TABLE + "."
                        + COST_PER_PLAN + " END) + (select sum(CASE WHEN p." + COST_PER_PLAN
                        + " is null THEN 0 ELSE p." + COST_PER_PLAN + " END) from " + TABLE
                        + " as p where p." + BILLPERIOD_ID + "=" + TABLE + "." + ID + ") ELSE "
                        + TABLE + "." + COST_PER_PLAN + " END) as " + SUM_CPP,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + "<{" + SUM_BILLDAY + "} or "
                        + Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW + "} THEN 0 ELSE "
                        + Logs.TABLE + "." + Logs.COST + " END) as " + SUM_COST,
                "sum(CASE WHEN " + Logs.TABLE + "." + Logs.DATE + "<{" + SUM_BILLDAY + "} or "
                        + Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW + "} THEN 0 ELSE "
                        + Logs.TABLE + "." + Logs.FREE + " END) as " + SUM_FREE,
                TABLE + "." + MIXED_UNITS_CALL + " AS " + MIXED_UNITS_CALL,
                TABLE + "." + MIXED_UNITS_DATA + " AS " + MIXED_UNITS_DATA,
                TABLE + "." + MIXED_UNITS_MMS + " AS " + MIXED_UNITS_MMS,
                TABLE + "." + MIXED_UNITS_SMS + " AS " + MIXED_UNITS_SMS};

        /**
         * Projection used for query id and (short)name.
         */
        public static final String[] PROJECTION_NAME = new String[]{ID, NAME, SHORTNAME};

        /**
         * Select only real plans.
         */
        public static final String WHERE_REALPLANS = TYPE + "!=" + TYPE_BILLPERIOD + " and " + TYPE
                + "!=" + TYPE_SPACING + " and " + TYPE + "!=" + TYPE_TITLE;

        /**
         * Select only bill periods.
         */
        public static final String WHERE_BILLPERIODS = TYPE + " = " + TYPE_BILLPERIOD;

        /**
         * Select only real plans and bill periods.
         */
        public static final String WHERE_PLANS = TYPE + "!=" + TYPE_SPACING + " and " + TYPE + "!="
                + TYPE_TITLE;

        /**
         * Default order.
         */
        public static final String DEFAULT_ORDER = ORDER + " ASC , " + ID + " ASC";

        /**
         * Reverse order.
         */
        public static final String REVERSE_ORDER = ORDER + " DESC , " + ID + " DESC";

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plans");

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI_SUM = Uri
                .parse("content://" + AUTHORITY + "/plans/sum");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.plan";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.plan";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ORDER + " INTEGER," + NAME + " TEXT," + SHORTNAME + " TEXT," + TYPE
                    + " TEXT, " + LIMIT_TYPE + " INTEGER," + LIMIT + " LONG," + BILLMODE + " TEXT,"
                    + BILLDAY + " LONG," + BILLPERIOD + " INTEGER," + BILLPERIOD_ID + " LONG,"
                    + COST_PER_ITEM + " FLOAT," + COST_PER_AMOUNT1 + " FLOAT," + COST_PER_AMOUNT2
                    + " FLOAT," + COST_PER_ITEM_IN_LIMIT + " FLOAT," + COST_PER_AMOUNT_IN_LIMIT1
                    + " FLOAT," + COST_PER_AMOUNT_IN_LIMIT2 + " FLOAT," + COST_PER_PLAN + " FLOAT,"
                    + MIXED_UNITS_CALL + " INTEGER," + MIXED_UNITS_SMS + " INTEGER,"
                    + MIXED_UNITS_MMS + " INTEGER," + MIXED_UNITS_DATA + " INTEGER," + NEXT_ALERT
                    + " LONG," + STRIP_SECONDS + " INTEGER," + STRIP_PAST + " INTEGER,"
                    + MERGED_PLANS + " TEXT" + ");");
            db.execSQL("CREATE INDEX " + TABLE + "_idx on " + TABLE + " (" + ID + "," + ORDER + ","
                    + TYPE + ")");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private Plans() {
            // nothing here.
        }

        /**
         * Get Name for id.
         *
         * @param cr {@link ContentResolver}
         * @param id id
         * @return name
         */
        public static String getName(final ContentResolver cr, final long id) {
            if (id < 0) {
                return null;
            }
            //noinspection ConstantConditions
            final Cursor cursor = cr.query(ContentUris.withAppendedId(CONTENT_URI, id),
                    PROJECTION_NAME, null, null, null);
            String ret = null;
            if (cursor != null && cursor.moveToFirst()) {
                ret = cursor.getString(INDEX_NAME);
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            return ret;
        }

        /**
         * Get parent's id for id.
         *
         * @param cr {@link ContentResolver}
         * @param id id
         * @return parent's id
         */
        public static int getParent(final ContentResolver cr, final int id) {
            if (id < 0) {
                return -1;
            }
            final Cursor cursor = cr.query(CONTENT_URI, PROJECTION_NAME,
                    DataProvider.Plans.MERGED_PLANS + " LIKE '%," + id + ",%'", null, null);
            int ret = -1;
            if (cursor != null && cursor.moveToFirst()) {
                ret = cursor.getInt(0);
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            return ret;
        }

        /**
         * Calculate limit set for given plan.
         *
         * @param pType  type of plan
         * @param lType  type of limit
         * @param amount billed amount
         * @param cost   billed cost
         * @return get used
         */
        public static int getUsed(final int pType, final int lType, final float amount,
                final float cost) {
            switch (lType) {
                case DataProvider.LIMIT_TYPE_COST:
                    return (int) (cost * CallMeter.HUNDRED);
                case DataProvider.LIMIT_TYPE_UNITS:
                    if (pType == DataProvider.TYPE_DATA) {
                        return (int) (amount / CallMeter.BYTE_KB);
                    } else {
                        return (int) amount;
                    }
                default:
                    if (amount > 0f) {
                        return -1;
                    } else {
                        return 0;
                    }
            }
        }

        /**
         * Calculate limit set for given plan.
         *
         * @param pType type of plan
         * @param lType type of limit
         * @param limit limit
         * @return get limit
         */
        public static long getLimit(final int pType, final int lType, final float limit) {
            if (limit == 0L) {
                return 0L;
            }
            switch (lType) {
                case DataProvider.LIMIT_TYPE_UNITS:
                    switch (pType) {
                        case DataProvider.TYPE_DATA:
                            return (long) (limit * CallMeter.BYTE_KB);
                        case DataProvider.TYPE_CALL:
                            return (long) (limit * CallMeter.SECONDS_MINUTE);
                        default:
                            return (long) limit;
                    }
                case DataProvider.LIMIT_TYPE_COST:
                    return (long) (limit * CallMeter.HUNDRED);
                default:
                    return 0L;
            }

        }

        /**
         * Get the SQL {@link String} selecting the bill period.
         *
         * @param period type of period
         * @param start  first bill day set.
         * @param now    move now to some other time, null == real now
         * @return SQL {@link String} selecting the bill period
         */
        public static String getBilldayWhere(final int period, final Calendar start,
                final Calendar now) {
            final Calendar bd = getBillDay(period, start, now, false);
            if (bd == null) {
                return null;
            }
            if (period == BILLPERIOD_INFINITE) {
                return DataProvider.Logs.DATE + " > " + bd.getTimeInMillis();
            }
            String next = "";
            if (now != null) {
                next = " AND " + DataProvider.Logs.DATE + " < " + now.getTimeInMillis();
            }
            // final Calendar nbd = getBillDay(period, start, now, true);
            return DataProvider.Logs.DATE + " > " + bd.getTimeInMillis() + next;
        }

        /**
         * Get the first bill day of this period.
         *
         * @param period type of period
         * @param start  first bill day set.
         * @param now    move now to some other time, null == real now
         * @param next   get the next, not the current one
         * @return {@link Calendar} with current first bill day
         */
        public static Calendar getBillDay(final int period, final long start, final Calendar now,
                final boolean next) {
            Calendar s = Calendar.getInstance();
            s.setTimeInMillis(start);
            return getBillDay(period, s, now, next);
        }

        /**
         * Get the first bill day of this period.
         *
         * @param period type of period
         * @param start  first bill day set.
         * @param now    move now to some other time, null == real now
         * @param next   get the next, not the current one
         * @return {@link Calendar} with current first bill day
         */
        public static Calendar getBillDay(final int period, final long start, final long now,
                final boolean next) {
            Calendar s = Calendar.getInstance();
            s.setTimeInMillis(start);
            Calendar n = Calendar.getInstance();
            n.setTimeInMillis(now);
            return getBillDay(period, s, n, next);
        }

        /**
         * Get length of bill period.
         *
         * @param period period type
         * @return array of [Calendar.FIELD, amount]
         */
        private static int[] getPeriodSettings(final int period) {
            int f; // Calendar.FIELD
            int v; // amount
            /*
             * The values j and k are used to add a different type of period to
			 * the value, eg. you can have a period of one month and for each
			 * month you can add one day so you would have the same day-number
			 * as start and end of the period. (01.01.2012 - 01.02.2012,
			 * 02.02.2012 - 02.03.2012 and so on...)
			 */
            int j = Calendar.MILLISECOND; // Additional Calendar.FIELD
            int k = 0; // Additional amount
            switch (period) {
                case BILLPERIOD_DAY:
                    f = Calendar.DAY_OF_MONTH;
                    v = 1;
                    break;
                case BILLPERIOD_28D:
                    f = Calendar.DAY_OF_MONTH;
                    v = 28;
                    break;
                case BILLPERIOD_30D:
                    f = Calendar.DAY_OF_MONTH;
                    v = 30;
                    break;
                case BILLPERIOD_31D:
                    f = Calendar.DAY_OF_MONTH;
                    v = 31;
                    break;
                case BILLPERIOD_60D:
                    f = Calendar.DAY_OF_MONTH;
                    v = 60;
                    break;
                case BILLPERIOD_90D:
                    f = Calendar.DAY_OF_MONTH;
                    v = 90;
                    break;
                case BILLPERIOD_1MONTH:
                    f = Calendar.MONTH;
                    v = 1;
                    break;
                case BILLPERIOD_2MONTH:
                    f = Calendar.MONTH;
                    v = 2;
                    break;
                case BILLPERIOD_3MONTH:
                    f = Calendar.MONTH;
                    v = 3;
                    break;
                case BILLPERIOD_4MONTH:
                    f = Calendar.MONTH;
                    v = 4;
                    break;
                case BILLPERIOD_5MONTH:
                    f = Calendar.MONTH;
                    v = 5;
                    break;
                case BILLPERIOD_6MONTH:
                    f = Calendar.MONTH;
                    v = 6;
                    break;
                case BILLPERIOD_12MONTH:
                    f = Calendar.YEAR;
                    v = 1;
                    break;
                case BILLPERIOD_1MONTH_1DAY:
                    f = Calendar.MONTH;
                    v = 1;
                    j = Calendar.DAY_OF_MONTH;
                    k = 1;
                    break;
                case BILLPERIOD_WEEK:
                    f = Calendar.DAY_OF_MONTH;
                    v = 7;
                    break;
                case BILLPERIOD_14D:
                    f = Calendar.DAY_OF_MONTH;
                    v = 14;
                    break;
                case BILLPERIOD_15D:
                    f = Calendar.DAY_OF_MONTH;
                    v = 15;
                    break;
                default:
                    f = Calendar.MONTH;
                    v = 1;
                    break;
            }

            return new int[]{f, v, j, k};
        }

        /**
         * Get the first bill day of this period.
         *
         * @param period type of period
         * @param start  first bill day set
         * @param now    move now to some other time, null == real now
         * @param next   get the next, not the current one
         * @return {@link Calendar} with current first bill day
         */
        public static Calendar getBillDay(final int period, final Calendar start,
                final Calendar now, final boolean next) {
            int f;
            int v;
            int j;
            int k;
            switch (period) {
                case BILLPERIOD_INFINITE:
                    return start;
                case BILLPERIOD_DAY:
                    Calendar ret;
                    if (now == null) {
                        ret = Calendar.getInstance();
                    } else {
                        ret = (Calendar) now.clone();
                    }
                    ret.set(Calendar.HOUR_OF_DAY, 0);
                    ret.set(Calendar.MINUTE, 0);
                    ret.set(Calendar.SECOND, 0);
                    ret.set(Calendar.MILLISECOND, 0);
                    if (next) {
                        ret.add(Calendar.DAY_OF_MONTH, 1);
                    }
                    return ret;
                default:
                    int[] i = getPeriodSettings(period);
                    if (i == null) {
                        throw new IllegalStateException("period settings == null");
                    } else {
                        f = i[0];
                        v = i[1];
                        j = i[2];
                        k = i[3];
                    }
            }

            Calendar ret;
            if (start != null) {
                ret = (Calendar) start.clone();
            } else {
                ret = Calendar.getInstance();
            }
            Calendar n = now;
            if (n == null) {
                n = Calendar.getInstance();
            }

            while (ret.after(n)) {
                //noinspection ResourceType
                ret.add(f, v * -1);
                //noinspection ResourceType
                ret.add(j, k * -1);
            }
            long time = ret.getTimeInMillis();
            while (!ret.after(n)) {
                time = ret.getTimeInMillis();
                //noinspection ResourceType
                ret.add(f, v);
                //noinspection ResourceType
                ret.add(j, k);
            }
            if (!next) {
                ret.setTimeInMillis(time);
            }
            return ret;
        }

        /**
         * Get all first bill days of this period.
         *
         * @param period  type of period
         * @param start   first bill day set
         * @param newerAs stop if bill day is older as this
         * @param offset  offset for each bill day
         * @return {@link ArrayList} of bill days
         */
        public static ArrayList<Long> getBillDays(final int period, final long start,
                final long newerAs, final long offset) {
            Log.d(TAG, "getBillDays()");
            ArrayList<Long> ret = new ArrayList<Long>();

            int f;
            int v;
            int j;
            int k;
            Calendar c = Calendar.getInstance();
            final long now = System.currentTimeMillis();
            switch (period) {
                case BILLPERIOD_INFINITE:
                    Log.d(TAG, "inifinite: return null");
                    return null;
                case BILLPERIOD_DAY:
                    c.set(Calendar.HOUR_OF_DAY, 0);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    long l = c.getTimeInMillis();
                    do {
                        Log.d(TAG, "day: return ", (l + offset));
                        ret.add(l + offset);
                        l -= Utils.DAY_IN_MILLIS;
                    } while (l > newerAs);
                    return ret;
                default:
                    int[] i = getPeriodSettings(period);
                    if (i == null) {
                        return null;
                    }
                    f = i[0];
                    v = i[1];
                    j = i[2];
                    k = i[3];
                    c.setTimeInMillis(start);
                    while (c.getTimeInMillis() < now) {
                        //noinspection ResourceType
                        c.add(f, v);
                        //noinspection ResourceType
                        c.add(j, k);
                    }
                    do {
                        //noinspection ResourceType
                        c.add(f, -1 * v);
                        //noinspection ResourceType
                        c.add(j, -1 * k);
                        Log.d(TAG, "return ", (c.getTimeInMillis() + offset));
                        ret.add(c.getTimeInMillis() + offset);
                    } while (c.getTimeInMillis() > newerAs);
            }

            return ret;
        }

        /**
         * Parse the MERGED_PLANS filed to a WHERE clause matching all merged plans.
         *
         * @param pid    the plan itself
         * @param merged merge plans
         * @return WHERE clause
         */
        public static String parseMergerWhere(final long pid, final String merged) {
            if (merged == null) {
                return DataProvider.Logs.TABLE + "." + DataProvider.Logs.PLAN_ID + " = " + pid;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(String.valueOf(pid));
            for (String ss : merged.split(",")) {
                if (ss.length() == 0) {
                    continue;
                }
                sb.append(",");
                sb.append(ss);
            }

            return DataProvider.Logs.TABLE + "." + DataProvider.Logs.PLAN_ID + " in ("
                    + sb.toString() + ")";
        }

        /**
         * Parse the MERGED_PLANS filed to a WHERE clause matching all merged plans.
         *
         * @param cr  {@link ContentResolver}
         * @param pid the plan itself
         * @return WHERE clause
         */
        public static String parseMergerWhere(final ContentResolver cr, final long pid) {
            Cursor c = cr.query(CONTENT_URI, new String[]{MERGED_PLANS}, ID + "=?",
                    new String[]{String.valueOf(pid)}, null);
            String ret = null;
            if (c != null && c.moveToFirst()) {
                ret = parseMergerWhere(pid, c.getString(0));
            }
            if (c != null) {
                c.close();
            }
            return ret;
        }
    }

    /**
     * Rules.
     *
     * @author flx
     */
    public static final class Rules {

        /**
         * Condition does not matter.
         */
        public static final int NO_MATTER = 2;

        /**
         * Condition type: match call.
         */
        public static final int WHAT_CALL = 0;

        /**
         * Condition type: match sms.
         */
        public static final int WHAT_SMS = 1;

        /**
         * Condition type: match mms.
         */
        public static final int WHAT_MMS = 2;

        /**
         * Condition type: match data.
         */
        public static final int WHAT_DATA = 3;

        /**
         * Table name.
         */
        private static final String TABLE = "rules";

        /**
         * Index in projection: id.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: active?
         */
        public static final int INDEX_ACTIVE = 1;

        /**
         * Index in projection: order.
         */
        public static final int INDEX_ORDER = 2;

        /**
         * Index in projection: ID of plan referred by this rule.
         */
        public static final int INDEX_PLAN_ID = 3;

        /**
         * Index in projection: Name.
         */
        public static final int INDEX_NAME = 4;

        /**
         * Index in projection: Kind of rule.
         */
        public static final int INDEX_WHAT = 5;

        /**
         * Index in projection: is roamed?
         */
        public static final int INDEX_ROAMED = 6;

        /**
         * Index in projection: is direction?
         */
        public static final int INDEX_DIRECTION = 7;

        /**
         * Index in projection: is hours?
         */
        public static final int INDEX_INHOURS_ID = 8;

        /**
         * Index in projection: is not hours?
         */
        public static final int INDEX_EXHOURS_ID = 9;

        /**
         * Index in projection: is number?
         */
        public static final int INDEX_INNUMBERS_ID = 10;

        /**
         * Index in projection: is not number?
         */
        public static final int INDEX_EXNUMBERS_ID = 11;

        /**
         * Index in projection: limit not reached?
         */
        public static final int INDEX_LIMIT_NOT_REACHED = 12;

        /**
         * Index in projection: is websms.
         */
        public static final int INDEX_IS_WEBSMS = 13;

        /**
         * Index in projection: is websms connector.
         */
        public static final int INDEX_IS_WEBSMS_CONNETOR = 14;

        /**
         * Index in projection: is sipcall.
         */
        public static final int INDEX_IS_SIPCALL = 15;

        /**
         * Index in projection: is sipcall provider.
         */
        public static final int INDEX_IS_SIPCALL_PROVIDER = 16;

        /**
         * Index in projection: my own number.
         */
        public static final int INDEX_MYNUMBER = 17;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * Active?
         */
        public static final String ACTIVE = "_active";

        /**
         * Order.
         */
        public static final String ORDER = "_order";

        /**
         * ID of plan referred by this rule.
         */
        public static final String PLAN_ID = "_plan_id";

        /**
         * Name.
         */
        public static final String NAME = "_rule_name";

        /**
         * Kind of rule.
         */
        public static final String WHAT = "_what";

        /**
         * Is roamed?
         */
        public static final String ROAMED = "_roamed";

        /**
         * Is direction?
         */
        public static final String DIRECTION = "_direction";

        /**
         * Is hours?
         */
        public static final String INHOURS_ID = "_inhourgroup_id";

        /**
         * Is not hours?
         */
        public static final String EXHOURS_ID = "_exhourgroup_id";

        /**
         * Is number?
         */
        public static final String INNUMBERS_ID = "_innumbergroup_id";

        /**
         * Is not number?
         */
        public static final String EXNUMBERS_ID = "_exnumbergroup_id";

        /**
         * Limit not reached?
         */
        public static final String LIMIT_NOT_REACHED = "_limit_not_reached";

        /**
         * Is websms.
         */
        public static final String IS_WEBSMS = "_is_websms";

        /**
         * Is websms connector.
         */
        public static final String IS_WEBSMS_CONNETOR = "_is_websms_connector";

        /**
         * Is sipcall.
         */
        public static final String IS_SIPCALL = "_is_sipcall";

        /**
         * Is sipcall provider.
         */
        public static final String IS_SIPCALL_PROVIDER = "_is_sipcall_provider";

        /**
         * My own number.
         */
        public static final String MYNUMBER = "_mynumber";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, ACTIVE, ORDER, PLAN_ID, NAME,
                WHAT, ROAMED, DIRECTION, INHOURS_ID, EXHOURS_ID, INNUMBERS_ID, EXNUMBERS_ID,
                LIMIT_NOT_REACHED, IS_WEBSMS, IS_WEBSMS_CONNETOR, IS_SIPCALL, IS_SIPCALL_PROVIDER,
                MYNUMBER};

        /**
         * Default order.
         */
        public static final String DEFAULT_ORDER = ORDER + " ASC , " + ID + " ASC";

        /**
         * Reverse order.
         */
        public static final String REVERSE_ORDER = ORDER + " DESC , " + ID + " DESC";

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/rules");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.rule";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.rule";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + ACTIVE + " INTEGER DEFAULT 1," + ORDER + " INTEGER," + NAME + " TEXT,"
                    + PLAN_ID + " INTEGER," + WHAT + " INTEGER," + ROAMED + " INTEGER," + DIRECTION
                    + " INTEGER," + INHOURS_ID + " TEXT," + EXHOURS_ID + " TEXT," + INNUMBERS_ID
                    + " TEXT," + EXNUMBERS_ID + " TEXT," + LIMIT_NOT_REACHED + " INTEGER,"
                    + IS_WEBSMS + " INTEGER," + IS_WEBSMS_CONNETOR + " TEXT," + IS_SIPCALL
                    + " INTEGER," + IS_SIPCALL_PROVIDER + " TEXT," + MYNUMBER + " TEXT" + ");");
            db.execSQL("CREATE INDEX " + TABLE + "_idx on " + TABLE + " (" + ID + "," + ACTIVE
                    + "," + ORDER + "," + PLAN_ID + "," + WHAT + ")");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private Rules() {
            // nothing here.
        }
    }

    /**
     * Numbers.
     *
     * @author flx
     */
    public static final class Numbers {

        /**
         * Table name.
         */
        private static final String TABLE = "numbers";

        /**
         * Index in projection: ID.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: ID for number block.
         */
        public static final int INDEX_GID = 1;

        /**
         * Index in projection: number.
         */
        public static final int INDEX_NUMBER = 2;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * ID for number block.
         */
        public static final String GID = "_gid";

        /**
         * Number.
         */
        public static final String NUMBER = "_number";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, GID, NUMBER};

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/numbers");

        /**
         * Content {@link Uri} for a group of numbers.
         */
        public static final Uri GROUP_URI = Uri.parse("content://" + AUTHORITY + "/numbers/group");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.number";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.number";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + GID + " LONG," + NUMBER + " TEXT" + ");");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private Numbers() {
            // nothing here.
        }
    }

    /**
     * Numbers.
     *
     * @author flx
     */
    public static final class NumbersGroup {

        /**
         * Table name.
         */
        private static final String TABLE = "numbersgroup";

        /**
         * Index in projection: ID.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: name of Numbers group.
         */
        public static final int INDEX_NAME = 1;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * Name of Numbers group.
         */
        public static final String NAME = "_name";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, NAME};

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
                + "/numbers/groups");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.numbergroup";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE
                = "vnd.android.cursor.item/vnd.ub0r.numbergroup";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + NAME + " TEXT" + ");");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private NumbersGroup() {
            // nothing here.
        }
    }

    /**
     * Hours.
     *
     * @author flx
     */
    public static final class Hours {

        /**
         * Table name.
         */
        private static final String TABLE = "hours";

        /**
         * Index in projection: ID.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: ID for block of hours.
         */
        public static final int INDEX_HOURS_ID = 1;

        /**
         * Index in projection: Day.
         */
        public static final int INDEX_DAY = 2;

        /**
         * Index in projection: Hour of day.
         */
        public static final int INDEX_HOUR = 3;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * ID for block of hours.
         */
        public static final String GID = "_gid";

        /**
         * Day.
         */
        public static final String DAY = "_day";

        /**
         * Hour of day.
         */
        public static final String HOUR = "_hour";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, GID, DAY, HOUR};

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/hours");

        /**
         * Content {@link Uri} for a group of numbers.
         */
        public static final Uri GROUP_URI = Uri.parse("content://" + AUTHORITY + "/hours/group");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.hour";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.hour";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + GID + " LONG," + DAY + " INTEGER," + HOUR + " INTEGER" + ");");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private Hours() {
            // nothing here.
        }
    }

    /**
     * Hours.
     *
     * @author flx
     */
    public static final class HoursGroup {

        /**
         * Table name.
         */
        private static final String TABLE = "hoursgroup";

        /**
         * Index in projection: ID.
         */
        public static final int INDEX_ID = 0;

        /**
         * Index in projection: name of hours group.
         */
        public static final int INDEX_NAME = 1;

        /**
         * ID.
         */
        public static final String ID = "_id";

        /**
         * Name of hours group.
         */
        public static final String NAME = "_name";

        /**
         * Projection used for query.
         */
        public static final String[] PROJECTION = new String[]{ID, NAME};

        /**
         * Content {@link Uri}.
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/hours/groups");

        /**
         * The MIME type of {@link #CONTENT_URI} providing a list.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ub0r.hourgroup";

        /**
         * The MIME type of a {@link #CONTENT_URI} single entry.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.ub0r.hourgroup";

        /**
         * Create table in {@link SQLiteDatabase}.
         *
         * @param db {@link SQLiteDatabase}
         */
        public static void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create table: " + TABLE);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE);
            db.execSQL("CREATE TABLE " + TABLE + " (" + ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + NAME + " TEXT" + ");");
        }

        /**
         * Upgrade table.
         *
         * @param db {@link SQLiteDatabase}
         * @throws IOException IOException
         */
        public static void onUpgrade(final Context context, final SQLiteDatabase db)
                throws IOException {
            Log.w(TAG, "Upgrading table: " + TABLE);

            String fn = TABLE + ".bak";
            context.deleteFile(fn);
            ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(fn,
                    Context.MODE_PRIVATE));
            backup(db, TABLE, PROJECTION, null, null, null, os);
            os.close();
            ObjectInputStream is = new ObjectInputStream(context.openFileInput(fn));
            onCreate(db);
            reload(db, TABLE, is);
            is.close();
        }

        /**
         * Default constructor.
         */
        private HoursGroup() {
            // nothing here.
        }
    }

    /**
     * XML im-/export meta data.
     *
     * @author flx
     */
    public final static class XmlMetaData {

        /**
         * Meta data.
         */
        public String version, country, provider, title;

        /**
         * @return true, if country, provider and title are set.
         */
        public boolean isSet() {
            return country != null && provider != null && title != null;
        }
    }

    /**
     * Internal id: logs.
     */
    private static final int LOGS = 1;

    /**
     * Internal id: single log entry.
     */
    private static final int LOGS_ID = 2;

    /**
     * Internal id: plans.
     */
    private static final int PLANS = 3;

    /**
     * Internal id: single plan.
     */
    private static final int PLANS_ID = 4;

    /**
     * Internal id: rules.
     */
    private static final int RULES = 5;

    /**
     * Internal id: single rule.
     */
    private static final int RULES_ID = 6;

    /**
     * Internal id: numbers.
     */
    private static final int NUMBERS = 7;

    /**
     * Internal id: single number.
     */
    private static final int NUMBERS_ID = 8;

    /**
     * Internal id: group of numbers.
     */
    private static final int NUMBERS_GID = 9;

    /**
     * Internal id: number group.
     */
    private static final int NUMBERS_GROUP = 10;

    /**
     * Internal id: singla number group.
     */
    private static final int NUMBERS_GROUP_ID = 11;

    /**
     * Internal id: hours.
     */
    private static final int HOURS = 12;

    /**
     * Internal id: single hour.
     */
    private static final int HOURS_ID = 13;

    /**
     * Internal id: group of hours.
     */
    private static final int HOURS_GID = 14;

    /**
     * Internal id: hours group.
     */
    private static final int HOURS_GROUP = 15;

    /**
     * Internal id: single hours group.
     */
    private static final int HOURS_GROUP_ID = 16;

    /**
     * Internal id: sum of logs.
     */
    private static final int LOGS_SUM = 17;

    /**
     * Internal id: logs joined with rules and plans.
     */
    private static final int LOGS_JOIN = 18;

    /**
     * Internal id: websms.
     */
    private static final int WEBSMS = 19;

    /**
     * Internal id: sipcall.
     */
    private static final int SIPCALL = 20;

    /**
     * Internal id: plans outer joined with its logs.
     */
    private static final int PLANS_SUM = 21;

    /**
     * Internal id: single plan outer joined with its logs.
     */
    private static final int PLANS_SUM_ID = 22;

    /**
     * {@link UriMatcher}.
     */
    private static final UriMatcher URI_MATCHER;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, "logs", LOGS);
        URI_MATCHER.addURI(AUTHORITY, "logs/#", LOGS_ID);
        URI_MATCHER.addURI(AUTHORITY, "logs/sum", LOGS_SUM);
        URI_MATCHER.addURI(AUTHORITY, "logs/join", LOGS_JOIN);
        URI_MATCHER.addURI(AUTHORITY, "plans", PLANS);
        URI_MATCHER.addURI(AUTHORITY, "plans/#", PLANS_ID);
        URI_MATCHER.addURI(AUTHORITY, "plans/sum", PLANS_SUM);
        URI_MATCHER.addURI(AUTHORITY, "plans/sum/#", PLANS_SUM_ID);
        URI_MATCHER.addURI(AUTHORITY, "rules", RULES);
        URI_MATCHER.addURI(AUTHORITY, "rules/#", RULES_ID);
        URI_MATCHER.addURI(AUTHORITY, "numbers", NUMBERS);
        URI_MATCHER.addURI(AUTHORITY, "numbers/#", NUMBERS_ID);
        URI_MATCHER.addURI(AUTHORITY, "numbers/group/#", NUMBERS_GID);
        URI_MATCHER.addURI(AUTHORITY, "numbers/groups/", NUMBERS_GROUP);
        URI_MATCHER.addURI(AUTHORITY, "numbers/groups/#", NUMBERS_GROUP_ID);
        URI_MATCHER.addURI(AUTHORITY, "hours", HOURS);
        URI_MATCHER.addURI(AUTHORITY, "hours/#", HOURS_ID);
        URI_MATCHER.addURI(AUTHORITY, "hours/group/#", HOURS_GID);
        URI_MATCHER.addURI(AUTHORITY, "hours/groups/", HOURS_GROUP);
        URI_MATCHER.addURI(AUTHORITY, "hours/groups/#", HOURS_GROUP_ID);
        URI_MATCHER.addURI(AUTHORITY, "websms", WEBSMS);
        URI_MATCHER.addURI(AUTHORITY, "sipcall", SIPCALL);
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        /**
         * {@link Context} .
         */
        private final Context ctx;

        /**
         * Default Constructor.
         *
         * @param context {@link Context}
         */
        DatabaseHelper(final Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            ctx = context;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onCreate(final SQLiteDatabase db) {
            Log.i(TAG, "create database");
            Logs.onCreate(db);
            WebSMS.onCreate(db);
            SipCall.onCreate(db);
            Plans.onCreate(db);
            Rules.onCreate(db);
            Numbers.onCreate(db);
            NumbersGroup.onCreate(db);
            Hours.onCreate(db);
            HoursGroup.onCreate(db);
            // import default rule set
            importDefault(ctx, db);
        }

        /**
         * Check, if wee need to unmatch() logs after updating the {@link SQLiteDatabase}.
         *
         * @param oldVersion old version
         * @param newVersion new version
         * @return true, if unmatch() is needed
         */
        private boolean needUnmatch(final int oldVersion, final int newVersion) {
            for (int v : DATABASE_KNOWNGOOD) {
                if (v == oldVersion) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
                    + ", which will destroy all old data");
            switch (oldVersion) {
                case 30:
                case 31:
                case 32:
                case 33:
                    try {
                        db.execSQL("ALTER TABLE " + Logs.TABLE + " ADD COLUMN " + Logs.MYNUMBER
                                + " TEXT");
                        db.execSQL("ALTER TABLE " + Rules.TABLE + " ADD COLUMN " + Rules.MYNUMBER
                                + " TEXT");
                    } catch (SQLiteException e) {
                        if (e.getMessage().contains("duplicate column name:")) {
                            Log.i(TAG, "ignore existing column", e);
                        } else {
                            Log.e(TAG, "error adding column", e);
                            throw e;
                        }
                    }
                case 34:
                    try {
                        db.execSQL("ALTER TABLE " + Plans.TABLE + " ADD COLUMN "
                                + Plans.MIXED_UNITS_DATA + " INTEGER");
                        db.execSQL("ALTER TABLE " + Plans.TABLE + " ADD COLUMN " + Plans.STRIP_PAST
                                + " INTEGER");
                    } catch (SQLiteException e) {
                        if (e.getMessage().contains("duplicate column name:")) {
                            Log.i(TAG, "ignore existing column", e);
                        } else {
                            Log.e(TAG, "error adding column", e);
                            throw e;
                        }
                    }
                    break;
                default:
                    try {
                        Plans.onUpgrade(ctx, db);
                        Rules.onUpgrade(ctx, db);
                        Numbers.onUpgrade(ctx, db);
                        NumbersGroup.onUpgrade(ctx, db);
                        Hours.onUpgrade(ctx, db);
                        HoursGroup.onUpgrade(ctx, db);
                        WebSMS.onUpgrade(ctx, db);
                        SipCall.onUpgrade(ctx, db);
                        Logs.onUpgrade(ctx, db);
                    } catch (IOException e) {
                        Log.e(TAG, "IOException on DB Upgrade!", e);
                        throw new IllegalStateException("IOException on DB Upgrade!", e);
                    }
                    break;
            }

            if (needUnmatch(oldVersion, newVersion)) {
                unmatch(db);
            }
        }
    }

    /**
     * {@link DatabaseHelper}.
     */
    private DatabaseHelper mOpenHelper;

    /**
     * Run RuleMatcher.unmatch locally.
     *
     * @param db {@link SQLiteDatabase}
     */
    private static void unmatch(final SQLiteDatabase db) {
        Log.d(TAG, "unmatch()");
        if (db.isReadOnly()) {
            Log.e(TAG, "Database is readonly, can not unmatch on upgrade!");
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
        cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
        // reset all but manually set plans
        db.update(DataProvider.Logs.TABLE, cv, DataProvider.Logs.RULE_ID + " is null or NOT ("
                + DataProvider.Logs.RULE_ID + " = " + DataProvider.NOT_FOUND + " AND "
                + DataProvider.Logs.PLAN_ID + " != " + DataProvider.NOT_FOUND + ")", null);
        cv.clear();
        cv.put(DataProvider.Plans.NEXT_ALERT, 0);
        db.update(DataProvider.Plans.TABLE, cv, null, null);
    }

    /**
     * Backup a single table.
     *
     * @param sb            {@link StringBuilder} for saving the data
     * @param db            {@link SQLiteDatabase}
     * @param table         table name
     * @param projection    projection
     * @param selection     selection
     * @param selectionArgs selection arguments
     * @param strip         strip column
     */
    private static void backupRuleSetSub(final StringBuilder sb, final SQLiteDatabase db,
            final String table, final String[] projection, final String selection,
            final String[] selectionArgs, final String strip) {
        ContentValues[] cvs;
        try {
            cvs = backup(db, table, projection, selection, selectionArgs, strip, null);
        } catch (IOException e) {
            throw new IllegalStateException("this can not be true!", e);
        }
        String e;
        String indent = "    ";
        if (table.endsWith("s")) {
            e = table.substring(0, table.length() - 1);
        } else {
            e = table;
        }
        if (table.equals(Hours.TABLE) || table.equals(Numbers.TABLE)) {
            indent += "    ";
        }
        for (ContentValues cv : cvs) {
            sb.append(indent).append("<").append(e).append(">\n");
            for (String k : projection) {
                String v = cv.getAsString(k);
                if (v != null) {
                    v = encodeString(v);
                    sb.append(indent).append("  <").append(k).append(">").append(v).append("</")
                            .append(k).append(">\n");
                }
            }
            if (table.equals(HoursGroup.TABLE)) {
                String gid = String.valueOf(cv.getAsInteger(HoursGroup.ID));
                sb.append(indent).append("  <hours>\n");
                backupRuleSetSub(sb, db, Hours.TABLE, Hours.PROJECTION, Hours.GID + "=?",
                        new String[]{gid}, null);
                sb.append(indent).append("  </hours>\n");
            } else if (table.equals(NumbersGroup.TABLE)) {
                String gid = String.valueOf(cv.getAsInteger(NumbersGroup.ID));
                sb.append(indent).append("  <numbers>\n");
                backupRuleSetSub(sb, db, Numbers.TABLE, Numbers.PROJECTION, Numbers.GID + "=?",
                        new String[]{gid}, null);
                sb.append(indent).append("  </numbers>\n");
            }
            sb.append(indent).append("</").append(e).append(">\n");
        }
    }

    /**
     * Encode {@link String} for XML output.
     *
     * @param s {@link String}
     * @return encoded {@link String}
     */
    private static String encodeString(final String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        } else {
            return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        }
    }

    /**
     * Decode {@link String} for XML input.
     *
     * @param s {@link String}
     * @return decoded {@link String}
     */
    private static String decodeString(final String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        } else {
            return s.replaceAll("&lt;", "<").replaceAll("&gt;", ">").replaceAll("&amp;", "&");
        }
    }

    /**
     * Backup rule set to String.
     *
     * @param context  {@link Context}
     * @param country  country
     * @param provider provider
     * @param title    description of the rule set
     * @return {@link String} representing {@link Rules} and {@link Plans}
     */
    public static String backupRuleSet(final Context context, final String country,
            final String provider, final String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<ruleset version=\"" + EXPORT_VERSION + "\">\n");
        sb.append("  <country>").append(encodeString(country)).append("</country>\n");
        sb.append("  <provider>").append(encodeString(provider)).append("</provider>\n");
        sb.append("  <title>").append(encodeString(title)).append("</title>\n");
        final SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        assert db != null;
        sb.append("  <plans>\n");
        backupRuleSetSub(sb, db, Plans.TABLE, Plans.PROJECTION, null, null, null);
        sb.append("  </plans>\n");
        sb.append("  <rules>\n");
        backupRuleSetSub(sb, db, Rules.TABLE, Rules.PROJECTION, null, null, null);
        sb.append("  </rules>\n");
        sb.append("  <hoursgroups>\n");
        backupRuleSetSub(sb, db, HoursGroup.TABLE, HoursGroup.PROJECTION, null, null, null);
        sb.append("  </hoursgroups>\n");
        sb.append("  <numbersgroups>\n");
        backupRuleSetSub(sb, db, NumbersGroup.TABLE, NumbersGroup.PROJECTION, null, null, null);
        sb.append("  </numbersgroups>\n");
        sb.append("</ruleset>\n");
        db.close();
        return sb.toString();
    }

    /**
     * Backup logs to String.
     *
     * @param context {@link Context}
     * @param title   description of the logs
     * @return {@link String} representing {@link Logs}
     */
    public static String backupLogs(final Context context, final String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<logs version=\"" + EXPORT_VERSION + "\">\n");
        sb.append("  <title>").append(encodeString(title)).append("</title>\n");
        final SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        assert db != null;
        sb.append("  <logs>\n");
        backupRuleSetSub(sb, db, Logs.TABLE, Logs.PROJECTION, null, null, null);
        sb.append("  </logs>\n");
        sb.append("  <websmss>\n");
        backupRuleSetSub(sb, db, WebSMS.TABLE, WebSMS.PROJECTION, null, null, null);
        sb.append("  </websmss>\n");
        sb.append("  <sipcalls>\n");
        backupRuleSetSub(sb, db, SipCall.TABLE, SipCall.PROJECTION, null, null, null);
        sb.append("  </sipcalls>\n");
        sb.append("</logs>\n");
        db.close();
        return sb.toString();
    }

    /**
     * Backup number groups to String.
     *
     * @param context {@link Context}
     * @param title   description of the number groups
     * @return {@link String} representing {@link NumbersGroup} and {@link Numbers}
     */
    public static String backupNumGroups(final Context context, final String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<numbersgroups version=\"" + EXPORT_VERSION + "\">\n");
        sb.append("  <title>").append(encodeString(title)).append("</title>\n");
        final SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        assert db != null;
        sb.append("  <numbersgroups>\n");
        backupRuleSetSub(sb, db, NumbersGroup.TABLE, NumbersGroup.PROJECTION, null, null, null);
        sb.append("  </numbersgroups>\n");
        sb.append("</numbersgroups>\n");
        db.close();
        return sb.toString();
    }

    /**
     * Backup hour groups to String.
     *
     * @param context {@link Context}
     * @param title   description of the hour groups
     * @return {@link String} representing {@link HoursGroup} and {@link Hours}
     */
    public static String backupHourGroups(final Context context, final String title) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        sb.append("<hoursgroups version=\"" + EXPORT_VERSION + "\">\n");
        sb.append("  <title>").append(encodeString(title)).append("</title>\n");
        final SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
        assert db != null;
        sb.append("  <hoursgroups>\n");
        backupRuleSetSub(sb, db, HoursGroup.TABLE, HoursGroup.PROJECTION, null, null, null);
        sb.append("  </hoursgroups>\n");
        sb.append("</hoursgroups>\n");
        db.close();
        return sb.toString();
    }

    /**
     * Parse all values of a given XML element to import it as {@link ContentValues} into {@link
     * SQLiteDatabase}.
     *
     * @param parser XmlPullParser
     * @param lists  list of {@link android.content.ContentValues} {@link java.util.ArrayList}s.
     * @param name   name of the holding element
     * @param list   current list
     * @throws XmlPullParserException XmlPullParserException
     * @throws IOException            IOException
     */
    private static void parseValues(final XmlPullParser parser,
            final HashMap<String, ArrayList<ContentValues>> lists, final String name,
            final ArrayList<ContentValues> list) throws XmlPullParserException, IOException {
        Log.d(TAG, "parseValues(..,", name, ", #", list.size(), ")");
        String element = name.substring(0, name.length() - 1);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            parser.require(XmlPullParser.START_TAG, null, element);
            ContentValues cv = new ContentValues();
            while (parser.next() != XmlPullParser.END_TAG || !element.equals(parser.getName())) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String k = parser.getName();
                if (k.equals("hours")) {
                    parser.next();
                    ArrayList<ContentValues> l = lists.get(DataProvider.Hours.TABLE);
                    if (l == null) {
                        l = new ArrayList<ContentValues>();
                        lists.put(DataProvider.Hours.TABLE, l);
                    }
                    parseValues(parser, lists, k, l);
                } else if (k.equals("numbers")) {
                    parser.next();
                    ArrayList<ContentValues> l = lists.get(DataProvider.Numbers.TABLE);
                    if (l == null) {
                        l = new ArrayList<ContentValues>();
                        lists.put(DataProvider.Numbers.TABLE, l);
                    }
                    parseValues(parser, lists, k, l);
                } else {
                    parser.next();
                    String v = parser.getText();
                    if (!TextUtils.isEmpty(v)) {
                        cv.put(k, decodeString(v));
                    }
                }
            }
            if (cv.size() > 0) {
                if (name.equals("plans") && !cv.containsKey(DataProvider.Plans.ORDER)) {
                    cv.put(DataProvider.Plans.ORDER, cv.getAsInteger(DataProvider.Plans.ID));
                } else if (name.equals("rules") && !cv.containsKey(DataProvider.Rules.ORDER)) {
                    cv.put(DataProvider.Rules.ORDER, cv.getAsInteger(DataProvider.Rules.ID));
                }
                list.add(cv);
            }
        }
    }

    /**
     * Parse XML file and return {@link XmlMetaData}.
     *
     * @param context {@link Context}
     * @param xml     XML as {@link String}
     * @return metadata
     */
    public static XmlMetaData parseXml(final Context context, final String xml) {
        Log.d(TAG, "parseXml(db, #", xml.length(), ")");
        XmlPullParser parser = Xml.newPullParser();
        XmlMetaData ret = new XmlMetaData();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xml));
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, null);
            ret.version = parser.getAttributeValue(null, "version");
            Log.d(TAG, "xml version: ", ret.version);
            String base = parser.getName();
            Log.d(TAG, "xml base element: ", base);
            while (parser.next() != XmlPullParser.END_TAG && !ret.isSet()) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                if (name.equals("country")) {
                    parser.next();
                    ret.country = decodeString(parser.getText());
                    Log.d(TAG, "xml country: ", ret.country);
                    parser.next();
                } else if (name.equals("provider")) {
                    parser.next();
                    ret.provider = decodeString(parser.getText());
                    Log.d(TAG, "xml provider: ", ret.provider);
                    parser.next();
                } else if (name.equals("title")) {
                    parser.next();
                    ret.title = decodeString(parser.getText());
                    Log.d(TAG, "xml title: ", ret.title);
                    parser.next();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing xml", e);
            Toast.makeText(context, R.string.err_export_read, Toast.LENGTH_LONG).show();
            return null;
        }
        return ret;
    }

    /**
     * Import data from XML into {@link SQLiteDatabase}.
     *
     * @param db  {@link android.database.sqlite.SQLiteDatabase}
     * @param xml XML
     * @return true, if import was successful
     */
    private static boolean importXml(final SQLiteDatabase db, final String xml) {
        Log.d(TAG, "importXml(db, #", xml.length(), ")");
        boolean ret = true;
        XmlPullParser parser = Xml.newPullParser();
        String version;
        String country;
        String provider;
        String title;
        HashMap<String, ArrayList<ContentValues>> lists
                = new HashMap<String, ArrayList<ContentValues>>();
        try {
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new StringReader(xml));
            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, null);
            version = parser.getAttributeValue(null, "version");
            Log.d(TAG, "xml version: ", version);
            String base = parser.getName();
            Log.d(TAG, "xml base element: ", base);
            while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals(base)) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String name = parser.getName();
                ArrayList<ContentValues> list = null;
                if (name.equals("country")) {
                    parser.next();
                    country = decodeString(parser.getText());
                    Log.d(TAG, "xml country: ", country);
                    parser.next();
                } else if (name.equals("provider")) {
                    parser.next();
                    provider = decodeString(parser.getText());
                    Log.d(TAG, "xml provider: ", provider);
                    parser.next();
                } else if (name.equals("title")) {
                    parser.next();
                    title = decodeString(parser.getText());
                    Log.d(TAG, "xml title: ", title);
                    parser.next();
                } else if (name.equals("plans")) {
                    list = new ArrayList<ContentValues>();
                    lists.put(DataProvider.Plans.TABLE, list);
                    parser.next();
                } else if (name.equals("rules")) {
                    list = new ArrayList<ContentValues>();
                    lists.put(DataProvider.Rules.TABLE, list);
                    parser.next();
                } else if (name.equals("hoursgroups")) {
                    list = new ArrayList<ContentValues>();
                    lists.put(DataProvider.HoursGroup.TABLE, list);
                    parser.next();
                } else if (name.equals("numbersgroups")) {
                    list = new ArrayList<ContentValues>();
                    lists.put(DataProvider.NumbersGroup.TABLE, list);
                    parser.next();
                } else if (name.equals("logs")) {
                    list = new ArrayList<ContentValues>();
                    lists.put(DataProvider.Logs.TABLE, list);
                    parser.next();
                } else if (name.equals("websmss")) {
                    list = new ArrayList<ContentValues>();
                    lists.put(DataProvider.WebSMS.TABLE, list);
                    parser.next();
                } else if (name.equals("sipcalls")) {
                    list = new ArrayList<ContentValues>();
                    lists.put(DataProvider.SipCall.TABLE, list);
                    parser.next();
                } else {
                    parser.next();
                }
                if (list != null) {
                    parseValues(parser, lists, name, list);
                }
            }
            // reload lists
            for (String table : lists.keySet()) {
                ArrayList<ContentValues> list = lists.get(table);
                if (list.size() > 0) {
                    db.delete(table, null, null);
                    reload(db, table, list);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "error parsing xml", e);
            ret = false;
        }
        return ret;
    }

    private static void importTable(final SQLiteDatabase db, final String table,
            final List<ContentValues> values) {
        // Add new tables here!
        if (Hours.TABLE.equals(table) || HoursGroup.TABLE.equals(table) || Logs.TABLE.equals(table)
                || Numbers.TABLE.equals(table) || NumbersGroup.TABLE.equals(table)
                || Plans.TABLE.equals(table) || Rules.TABLE.equals(table)
                || SipCall.TABLE.equals(table) || WebSMS.TABLE.equals(table)) {
            // reload cvs into table
            db.delete(table, null, null);
            reload(db, table, values.toArray(new ContentValues[values.size()]));
        }
        values.clear();
    }

    /**
     * Import data from lines into {@link SQLiteDatabase}.
     *
     * @param db    {@link android.database.sqlite.SQLiteDatabase}
     * @param lines data
     */
    private static void importData(final SQLiteDatabase db, final String[] lines) {
        final int l = lines.length;
        Log.d(TAG, "importData(db, #", l, ")");
        String table = null;
        ArrayList<ContentValues> cvs = new ArrayList<ContentValues>();
        for (int i = 2; i < l; i++) {
            final String s = lines[i];
            if (s == null || s.length() == 0) {
                continue;
            }
            final String[] ti = lines[i].split(" ", 2);
            if (ti[0].length() == 0) {
                continue;
            }
            if (table == null) {
                table = ti[0];
                cvs.clear();
            } else if (!table.equals(ti[0])) {
                importTable(db, table, cvs);
                // prepare new table/cvs
                table = ti[0];
            }
            if (ti.length < 2) {
                continue;
            }
            final String imp = ti[1];
            final String[] nvs = imp.split(EXPORT_VALUESEPARATOR);
            final ContentValues cv = new ContentValues();
            for (String nv : nvs) {
                final String[] nvv = nv.split(":", 2);
                if (nvv.length < 2) {
                    continue;
                }
                cv.put(nvv[0], nvv[1]);
            }
            if ("plans".equals(table) && !cv.containsKey(DataProvider.Plans.ORDER)) {
                cv.put(DataProvider.Plans.ORDER, cv.getAsInteger(DataProvider.Plans.ID));
            } else if ("rules".equals(table) && !cv.containsKey(DataProvider.Rules.ORDER)) {
                cv.put(DataProvider.Rules.ORDER, cv.getAsInteger(DataProvider.Rules.ID));
            }
            cvs.add(cv);
        }
        if (table != null && table.length() > 0) {
            importTable(db, table, cvs);
        }
    }

    /**
     * Import data from {@link String}.
     *
     * @param context {@link Context}
     * @param ruleSet data as {@link String}; "DEFAULT" will import default rule set
     * @return true, if import was successful
     */
    public static boolean importData(final Context context, final String ruleSet) {
        if (TextUtils.isEmpty(ruleSet)) {
            return false;
        }
        boolean ret = false;
        final SQLiteDatabase db = new DatabaseHelper(context).getWritableDatabase();
        assert db != null;
        if (ruleSet.trim().startsWith("<")) {
            ret = importXml(db, ruleSet);
            Preferences.setDefaultPlan(context, false);
            RuleMatcher.unmatch(context);
        } else if (ruleSet.equals("DEFAULT")) {
            importDefault(context, db);
            Preferences.setDefaultPlan(context, true);
            RuleMatcher.unmatch(context);
            ret = true;
        } else {
            String[] lines = ruleSet.split("\n");
            if (lines.length > 2) {
                importData(db, lines);
                Preferences.setDefaultPlan(context, false);
                RuleMatcher.unmatch(context);
                return true;
            }
        }
        db.close();
        return ret;
    }

    private static String[] ID = new String[1];

    private static String[] getIdMapping(final long id) {
        ID[0] = String.valueOf(id);
        return ID;
    }

    private static void updateTable(final SQLiteDatabase db, final String table,
            final ContentValues cv, final long id) {
        db.update(table, cv, "_id=?", getIdMapping(id));
    }

    private static void updatePlans(final SQLiteDatabase db, final ContentValues cv,
            final long id) {
        updateTable(db, Plans.TABLE, cv, id);
    }

    private static void updateRules(final SQLiteDatabase db, final ContentValues cv,
            final long id) {
        updateTable(db, Rules.TABLE, cv, id);
    }

    /**
     * Import default rule set.
     *
     * @param context {@link Context}
     * @param db      {@link SQLiteDatabase}
     */
    public static void importDefault(final Context context, final SQLiteDatabase db) {
        // import default
        BufferedReader reader = new BufferedReader(new InputStreamReader(context.getResources()
                .openRawResource(R.raw.default_setup)));
        StringBuilder sb = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append("\n");

            }
        } catch (IOException e) {
            Log.e(TAG, "error reading raw data", e);
        }
        importXml(db, sb.toString());

        // translate default rule set:
        ContentValues cv = new ContentValues();
        // bill period: 12
        cv.put(Plans.NAME,
                context.getResources().getStringArray(R.array.plans_type)[TYPE_BILLPERIOD]);
        cv.put(Plans.SHORTNAME, context.getString(R.string.billperiod_sn));
        // set 1st day of billing (including TZ)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 1);
        cal.set(Calendar.SECOND, 1);
        cal.set(Calendar.MILLISECOND, 0);
        cv.put(Plans.BILLDAY, cal.getTimeInMillis());
        updatePlans(db, cv, 12);
        cv.clear();
        // spacer: 13, 17, 21
        cv.put(Plans.NAME, context.getResources().getStringArray(R.array.plans_type)[TYPE_SPACING]);
        updatePlans(db, cv, 13);
        updatePlans(db, cv, 17);
        updatePlans(db, cv, 21);
        cv.clear();
        // calls: 14
        cv.put(Plans.NAME, context.getString(R.string.calls));
        cv.put(Plans.SHORTNAME, context.getString(R.string.calls));
        updatePlans(db, cv, 14);
        cv.clear();
        // calls in: 15
        cv.put(Plans.NAME, context.getString(R.string.calls_in));
        cv.put(Plans.SHORTNAME, context.getString(R.string.calls_in_));
        updatePlans(db, cv, 15);
        cv.clear();
        // calls out: 16
        cv.put(Plans.NAME, context.getString(R.string.calls_out));
        cv.put(Plans.SHORTNAME, context.getString(R.string.calls_out_));
        updatePlans(db, cv, 16);
        cv.clear();
        // messages: 18
        cv.put(Plans.NAME, context.getString(R.string.messages));
        cv.put(Plans.SHORTNAME, context.getString(R.string.messages_));
        updatePlans(db, cv, 18);
        cv.clear();
        // sms in: 19
        cv.put(Plans.NAME, context.getString(R.string.sms_in));
        cv.put(Plans.SHORTNAME, context.getString(R.string.sms_in_));
        updatePlans(db, cv, 19);
        cv.clear();
        // sms out: 20
        cv.put(Plans.NAME, context.getString(R.string.sms_out));
        cv.put(Plans.SHORTNAME, context.getString(R.string.sms_out_));
        updatePlans(db, cv, 20);
        cv.clear();
        // mms in: 27
        cv.put(Plans.NAME, context.getString(R.string.mms_in));
        cv.put(Plans.SHORTNAME, context.getString(R.string.mms_in_));
        updatePlans(db, cv, 27);
        cv.clear();
        // mms out: 28
        cv.put(Plans.NAME, context.getString(R.string.mms_out));
        cv.put(Plans.SHORTNAME, context.getString(R.string.mms_out_));
        updatePlans(db, cv, 28);
        cv.clear();
        // data: 22
        cv.put(Plans.NAME, context.getString(R.string.data_));
        cv.put(Plans.SHORTNAME, context.getString(R.string.data));
        updatePlans(db, cv, 22);
        cv.clear();
        // data in/out: 23
        cv.put(Plans.NAME, context.getString(R.string.data_inout));
        cv.put(Plans.SHORTNAME, context.getString(R.string.data_inout_));
        updatePlans(db, cv, 23);
        cv.clear();
        // rules
        // data
        cv.put(Rules.NAME, context.getString(R.string.data));
        updateRules(db, cv, 1);
        cv.clear();
        // calls in
        cv.put(Rules.NAME, context.getString(R.string.calls_in));
        updateRules(db, cv, 2);
        cv.clear();
        // calls out
        cv.put(Rules.NAME, context.getString(R.string.calls_out));
        updateRules(db, cv, 3);
        cv.clear();
        // sms in
        cv.put(Rules.NAME, context.getString(R.string.sms_in));
        updateRules(db, cv, 4);
        cv.clear();
        // sms out
        cv.put(Rules.NAME, context.getString(R.string.sms_out));
        updateRules(db, cv, 5);
        cv.clear();
        // mms in
        cv.put(Rules.NAME, context.getString(R.string.mms_in));
        updateRules(db, cv, 7);
        cv.clear();
        // mms out
        cv.put(Rules.NAME, context.getString(R.string.mms_out));
        updateRules(db, cv, 8);
        cv.clear();
        // exclude numbers from calls
        cv.put(NumbersGroup.NAME, context.getString(R.string.numbergroup_excalls));
        updateTable(db, NumbersGroup.TABLE, cv, 1);
        cv.clear();
        // exclude numbers from sms
        cv.put(NumbersGroup.NAME, context.getString(R.string.numbergroup_exsms));
        updateTable(db, NumbersGroup.TABLE, cv, 2);
        cv.clear();

        final ContentResolver cr = context.getContentResolver();
        final SimIdColumnFinder finder = SimIdColumnFinder.getsInstance();
        final String simIdCalls = finder.getSecondSimId(cr, CallLog.Calls.CONTENT_URI);
        final String simIdSMS = finder.getSecondSimId(cr, LogRunnerService.URI_SMS);

        // calls out 2: 31
        // calls in 2: 32
        if (simIdCalls != null) {
            // rename call * 2 plans/rules
            cv.put(Plans.NAME, context.getString(R.string.calls_out) + " 2");
            cv.put(Plans.SHORTNAME, context.getString(R.string.calls_out_) + "2");
            updatePlans(db, cv, 31);
            cv.put(Plans.NAME, context.getString(R.string.calls_in) + " 2");
            cv.put(Plans.SHORTNAME, context.getString(R.string.calls_in_) + "2");
            updatePlans(db, cv, 32);
            cv.clear();
            cv.put(Rules.NAME, context.getString(R.string.calls_out) + " 2");
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(31));
            cv.put(Rules.NAME, context.getString(R.string.calls_in) + " 2");
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(32));
            cv.clear();
            // if second sim is found, change rules
            cv.put(Rules.MYNUMBER, simIdCalls);
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(31));
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(32));
            cv.clear();
        } else {
            // remove unused call * 2 plans/rules
            db.delete(Plans.TABLE, Plans.ID + "=?", getIdMapping(31));
            db.delete(Plans.TABLE, Plans.ID + "=?", getIdMapping(32));
            db.delete(Rules.TABLE, Rules.PLAN_ID + "=?", getIdMapping(31));
            db.delete(Rules.TABLE, Rules.PLAN_ID + "=?", getIdMapping(32));
        }

        // sms out 2: 34
        // sms in 2: 33
        if (simIdSMS != null) {
            // rename sms * 2 plans/rules
            cv.put(Plans.NAME, context.getString(R.string.sms_out) + " 2");
            cv.put(Plans.SHORTNAME, context.getString(R.string.sms_out_) + "2");
            updatePlans(db, cv, 34);
            cv.put(Plans.NAME, context.getString(R.string.sms_in) + " 2");
            cv.put(Plans.SHORTNAME, context.getString(R.string.sms_in_) + "2");
            updatePlans(db, cv, 33);
            cv.clear();
            cv.put(Rules.NAME, context.getString(R.string.sms_out) + " 2");
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(34));
            cv.put(Rules.NAME, context.getString(R.string.sms_in) + " 2");
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(33));
            cv.clear();
            // if second sim is found, change rules
            cv.put(Rules.MYNUMBER, simIdSMS);
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(34));
            db.update(Rules.TABLE, cv, Rules.PLAN_ID + "=?", getIdMapping(33));
            cv.clear();
        } else {
            // remove unused sms * 2 plans/rules
            db.delete(Plans.TABLE, Plans.ID + "=?", getIdMapping(33));
            db.delete(Plans.TABLE, Plans.ID + "=?", getIdMapping(34));
            db.delete(Rules.TABLE, Rules.PLAN_ID + "=?", getIdMapping(33));
            db.delete(Rules.TABLE, Rules.PLAN_ID + "=?", getIdMapping(34));
        }

        PackageManager pm = context.getPackageManager();

        // websms: 29
        if (!isPackageExists(pm, "de.ub0r.android.websms")) {
            db.delete(Plans.TABLE, Plans.ID + "=?", getIdMapping(29));
            db.delete(Rules.TABLE, Rules.PLAN_ID + "=?", getIdMapping(29));
        }

        // voip: 30
        if (!isPackageExists(pm, "org.sipdroid.sipua", "com.csipsimple")) {
            db.delete(Plans.TABLE, Plans.ID + "=?", getIdMapping(30));
            db.delete(Rules.TABLE, Rules.PLAN_ID + "=?", getIdMapping(30));
        }

        Preferences.setDefaultPlan(context, true);
    }

    private static boolean isPackageExists(final PackageManager pm, final String... pkgs) {
        for (String pkg : pkgs) {
            try {
                if (pm.getPackageInfo(pkg, PackageManager.GET_META_DATA) != null) {
                    Log.d(TAG, "found package: ", pkg);
                    return true;
                }
            } catch (NameNotFoundException e) {
                Log.d(TAG, "package not found: ", pkg, e);
            }
        }
        return false;
    }

    /**
     * Try to backup fields from table.
     *
     * @param db            {@link SQLiteDatabase}
     * @param table         table
     * @param cols          columns
     * @param selection     selection
     * @param selectionArgs selection arguments
     * @param strip         column to forget on backup, eg. _id
     * @param os            optional {@link ObjectOutputStream} for saving {@link ContentValues} to
     * @return array of rows if os is not null
     * @throws IOException IOException
     */
    private static ContentValues[] backup(final SQLiteDatabase db, final String table,
            final String[] cols, final String selection, final String[] selectionArgs,
            final String strip, final ObjectOutputStream os) throws IOException {
        Log.d(TAG, "backup(db,", table, ",cols,sel,args,", strip, ")");
        ArrayList<ContentValues> ret = null;
        if (os == null) {
            ret = new ArrayList<ContentValues>();
        }
        String[] proj = cols;
        if (strip != null) {
            ArrayList<String> a = new ArrayList<String>(cols.length);
            for (String c : cols) {
                if (strip.equals(c)) {
                    Log.d(TAG, "ignore column: ", c);
                    continue;
                }
                a.add(c);
            }
            proj = a.toArray(new String[a.size()]);
        }
        final int l = proj.length;
        Cursor cursor;
        try {
            cursor = db.query(table, proj, selection, selectionArgs, null, null, null);
        } catch (SQLException e) {
            if (l == 1) {
                return null;
            }
            final String err = e.getMessage();
            if (!err.startsWith("no such column:")) {
                throw new IllegalStateException("Could not parse exeption message: " + err);
            }
            Matcher m = P.matcher(err);
            if (!m.find()) {
                throw new IllegalStateException("Could not parse exeption message: " + err);
            }
            final String str = m.group(1);
            return backup(db, table, proj, selection, selectionArgs, str, os);
        }
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int cnt = 0;
                ContentValues cv = os == null ? new ContentValues() : null;
                HashMap<String, String> map = os == null ? null : new HashMap<String, String>();
                for (int i = 0; i < l; i++) {
                    final String s = cursor.getString(i);
                    if (s != null) {
                        if (os == null) {
                            cv.put(proj[i], s);
                        } else {
                            map.put(proj[i], s);
                        }
                    }
                }
                if (os == null) {
                    ret.add(cv);
                } else {
                    os.writeObject(map);
                    if (cnt % 10 == 0) {
                        os.reset();
                    }
                }
                ++cnt;
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        if (ret == null) {
            return null;
        }
        return ret.toArray(new ContentValues[ret.size()]);
    }

    @SuppressWarnings("unchecked")
    private static void reload(final SQLiteDatabase db, final String table,
            final ObjectInputStream is) throws IOException {
        if (is == null) {
            return;
        }
        Log.d(TAG, "reload(db, ", table, ", stream])");
        db.beginTransaction();
        try {
            while (true) {
                HashMap<String, String> map;
                map = (HashMap<String, String>) is.readObject();
                if (map == null) {
                    break;
                }
                ContentValues cv = new ContentValues(map.size());
                for (String k : map.keySet()) {
                    cv.put(k, map.get(k));
                }
                Log.d(TAG, "reload: ", table, " insert: ", cv);
                db.insert(table, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "error reloading row: " + table, e);
        } catch (SQLException e) {
            Log.e(TAG, "error reloading row: " + table, e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Reload backup into table.
     *
     * @param db     {@link SQLiteDatabase}
     * @param table  table
     * @param values {@link ContentValues}[] backed up with backup()
     */
    private static void reload(final SQLiteDatabase db, final String table,
            final ContentValues[] values) {
        if (values == null || values.length == 0) {
            return;
        }
        Log.d(TAG, "reload(db, ", table, ", cv[", values.length, "])");
        db.beginTransaction();
        try {
            for (ContentValues cv : values) {
                Log.d(TAG, "reload: ", table, " insert: ", cv);
                db.insert(table, null, cv);
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "error reloading row: " + table, e);
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Reload backup into table.
     *
     * @param db     {@link SQLiteDatabase}
     * @param table  table
     * @param values {@link ContentValues}[] backed up with backup()
     */
    private static void reload(final SQLiteDatabase db, final String table,
            final ArrayList<ContentValues> values) {
        if (values == null || values.size() == 0) {
            Log.w(TAG, "skip reload empty values: " + table);
        } else {
            reload(db, table, values.toArray(new ContentValues[values.size()]));
        }
    }

    /**
     * Get type for "what".
     *
     * @param what rule's type
     * @return plan's type
     */
    public static int what2type(final int what) {
        switch (what) {
            case Rules.WHAT_DATA:
                return TYPE_DATA;
            case Rules.WHAT_MMS:
                return TYPE_MMS;
            case Rules.WHAT_SMS:
                return TYPE_SMS;
            default:
                return TYPE_CALL;
        }
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        Log.d(TAG, "delete(", uri, ",", selection, ")");
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        assert db != null;
        //noinspection ConstantConditions
        ContentResolver cr = getContext().getContentResolver();
        assert cr != null;
        int ret;
        long id;
        Cursor c;
        String w;
        switch (URI_MATCHER.match(uri)) {
            case LOGS:
                ret = db.delete(Logs.TABLE, selection, selectionArgs);
                break;
            case LOGS_ID:
                ret = db.delete(Logs.TABLE,
                        DbUtils.sqlAnd(Logs.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case PLANS_ID:
                ret = db.delete(Plans.TABLE,
                        DbUtils.sqlAnd(Plans.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case RULES_ID:
                ret = db.delete(Rules.TABLE,
                        DbUtils.sqlAnd(Rules.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case NUMBERS_ID:
                id = ContentUris.parseId(uri);
                w = DbUtils.sqlAnd(Numbers.ID + "=" + id, selection);
                c = db.query(Numbers.TABLE, new String[]{Numbers.GID}, w, selectionArgs, null, null,
                        null);
                if (c != null && c.moveToFirst()) {
                    final long gid = c.getLong(0);
                    cr.notifyChange(ContentUris.withAppendedId(Numbers.GROUP_URI, gid), null);
                }
                if (c != null && !c.isClosed()) {
                    c.close();
                }
                ret = db.delete(Numbers.TABLE, w, selectionArgs);
                break;
            case NUMBERS_GID:
            case NUMBERS_GROUP_ID:
                id = ContentUris.parseId(uri);
                ret = db.delete(Numbers.TABLE, DbUtils.sqlAnd(Numbers.GID + "=" + id, selection),
                        selectionArgs);
                ret += db.delete(NumbersGroup.TABLE, NumbersGroup.ID + " = " + id, null);
                break;
            case HOURS_ID:
                id = ContentUris.parseId(uri);
                w = DbUtils.sqlAnd(Hours.ID + "=" + id, selection);
                c = db.query(Hours.TABLE, new String[]{Hours.GID}, w, selectionArgs, null, null,
                        null);
                if (c != null && c.moveToFirst()) {
                    final long gid = c.getLong(0);
                    cr.notifyChange(ContentUris.withAppendedId(Hours.GROUP_URI, gid), null);
                }
                if (c != null && !c.isClosed()) {
                    c.close();
                }
                ret = db.delete(Hours.TABLE,
                        DbUtils.sqlAnd(Hours.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case HOURS_GID:
            case HOURS_GROUP_ID:
                id = ContentUris.parseId(uri);
                ret = db.delete(Hours.TABLE, DbUtils.sqlAnd(Hours.GID + "=" + id, selection),
                        selectionArgs);
                ret += db.delete(HoursGroup.TABLE, HoursGroup.ID + " = " + id, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri " + uri);
        }
        if (ret > 0) {
            cr.notifyChange(uri, null);
        }
        return ret;
    }

    @Override
    public String getType(final Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case LOGS:
                return Logs.CONTENT_TYPE;
            case LOGS_ID:
                return Logs.CONTENT_ITEM_TYPE;
            case WEBSMS:
                return WebSMS.CONTENT_TYPE;
            case SIPCALL:
                return SipCall.CONTENT_TYPE;
            case PLANS:
            case PLANS_SUM:
                return Plans.CONTENT_TYPE;
            case PLANS_ID:
            case PLANS_SUM_ID:
                return Plans.CONTENT_ITEM_TYPE;
            case RULES:
                return Rules.CONTENT_TYPE;
            case RULES_ID:
                return Rules.CONTENT_ITEM_TYPE;
            case NUMBERS:
            case NUMBERS_GID:
                return Numbers.CONTENT_TYPE;
            case NUMBERS_ID:
                return Numbers.CONTENT_ITEM_TYPE;
            case NUMBERS_GROUP:
                return NumbersGroup.CONTENT_TYPE;
            case NUMBERS_GROUP_ID:
                return NumbersGroup.CONTENT_ITEM_TYPE;
            case HOURS:
            case HOURS_GID:
                return Hours.CONTENT_TYPE;
            case HOURS_ID:
                return Hours.CONTENT_ITEM_TYPE;
            case HOURS_GROUP:
                return HoursGroup.CONTENT_TYPE;
            case HOURS_GROUP_ID:
                return HoursGroup.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public ContentProviderResult[] applyBatch(
            @NonNull final ArrayList<ContentProviderOperation> operations)
            throws OperationApplicationException {
        Log.d(TAG, "applyBatch(#", operations.size(), ")");
        ContentProviderResult[] ret = null;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        assert db != null;
        db.beginTransaction();
        try {
            ret = super.applyBatch(operations);
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "error applying batch");
            throw e;
        } finally {
            db.endTransaction();
        }
        return ret;
    }

    @Override
    public int bulkInsert(final Uri uri, @NonNull final ContentValues[] values) {
        Log.d(TAG, "bulkInsert(", uri, ", #", values.length, ")");
        if (values.length == 0) {
            return 0;
        }
        int ret = 0;
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        assert db != null;
        db.beginTransaction();
        try {
            for (ContentValues cv : values) {
                insert(uri, cv);
            }
            ret = values.length;
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "error inserting row: " + uri);
            throw e;
        } finally {
            db.endTransaction();
        }
        return ret;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        Log.d(TAG, "insert(", uri, ",", values, ")");
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        assert db != null;
        long ret;
        switch (URI_MATCHER.match(uri)) {
            case LOGS:
                ret = db.insert(Logs.TABLE, null, values);
                break;
            case WEBSMS:
                ret = db.insert(WebSMS.TABLE, null, values);
                break;
            case SIPCALL:
                ret = db.insert(SipCall.TABLE, null, values);
                break;
            case PLANS:
                if (!values.containsKey(Plans.ORDER)) {
                    final Cursor c = db.query(Plans.TABLE, new String[]{Plans.ORDER}, null, null,
                            null, null, Plans.ORDER + " DESC");
                    if (c != null && c.moveToFirst()) {
                        values.put(Plans.ORDER, c.getInt(0) + 1);
                    }
                    if (c != null && !c.isClosed()) {
                        c.close();
                    }
                }
                ret = db.insert(Plans.TABLE, null, values);
                break;
            case RULES:
                if (!values.containsKey(Rules.ORDER)) {
                    final Cursor c = db.query(Rules.TABLE, new String[]{Plans.ORDER}, null, null,
                            null, null, Rules.ORDER + " DESC");
                    if (c != null && c.moveToFirst()) {
                        values.put(Rules.ORDER, c.getInt(0) + 1);
                    }
                    if (c != null && !c.isClosed()) {
                        c.close();
                    }
                }
                ret = db.insert(Rules.TABLE, null, values);
                break;
            case NUMBERS:
                ret = db.insert(Numbers.TABLE, null, values);
                break;
            case NUMBERS_GROUP:
                ret = db.insert(NumbersGroup.TABLE, null, values);
                break;
            case HOURS:
                ret = db.insert(Hours.TABLE, null, values);
                break;
            case HOURS_GROUP:
                ret = db.insert(HoursGroup.TABLE, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri " + uri);
        }
        if (ret < 0) {
            Log.d(TAG, "insert(): null");
            return null;
        } else {
            //noinspection ConstantConditions
            getContext().getContentResolver().notifyChange(uri, null);
            final Uri u = ContentUris.withAppendedId(uri, ret);
            Log.d(TAG, "insert(): ", u);
            return u;
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        Log.d(TAG, "query(", uri, ",", selection, ")");
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        final int uid = URI_MATCHER.match(uri);
        String groupBy = null;
        String having = null;
        // If no sort order is specified use the default
        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            orderBy = null;
        } else {
            orderBy = sortOrder;
        }
        String[] proj = null;
        final int l = projection.length;

        Cursor c;

        switch (uid) {
            case LOGS_ID:
                qb.appendWhere(Logs.ID + "=" + ContentUris.parseId(uri));
            case LOGS:
                qb.setTables(Logs.TABLE);
                break;
            case LOGS_JOIN:
                qb.setTables(Logs.TABLE + " LEFT OUTER JOIN " + Plans.TABLE + " ON (" + Logs.TABLE
                        + "." + Logs.PLAN_ID + "=" + Plans.TABLE + "." + Plans.ID
                        + ") LEFT OUTER JOIN " + Rules.TABLE + " ON (" + Logs.TABLE + "."
                        + Logs.RULE_ID + "=" + Rules.TABLE + "." + Rules.ID + ")");
                break;
            case LOGS_SUM:
                qb.setTables(Logs.TABLE + " INNER JOIN " + Plans.TABLE + " ON (" + Logs.TABLE + "."
                        + Logs.PLAN_ID + "=" + Plans.TABLE + "." + Plans.ID + ")");
                groupBy = DataProvider.Logs.PLAN_ID;
                break;
            case WEBSMS:
                qb.setTables(WebSMS.TABLE);
                break;
            case SIPCALL:
                qb.setTables(SipCall.TABLE);
                break;
            case PLANS_ID:
                qb.appendWhere(Plans.ID + "=" + ContentUris.parseId(uri));
            case PLANS:
                qb.setTables(Plans.TABLE);
                if (orderBy == null) {
                    orderBy = Plans.DEFAULT_ORDER;
                }
                break;
            case PLANS_SUM_ID:
                qb.appendWhere(Plans.TABLE + "." + Plans.ID + "=" + ContentUris.parseId(uri));
            case PLANS_SUM:
                final boolean hideZero = Utils.parseBoolean(
                        uri.getQueryParameter(Plans.PARAM_HIDE_ZERO), false);
                final boolean hideNoCost = Utils.parseBoolean(
                        uri.getQueryParameter(Plans.PARAM_HIDE_NOCOST), false);
                final boolean hideToday = Utils.parseBoolean(
                        uri.getQueryParameter(Plans.PARAM_HIDE_TODAY), false);
                final boolean hideAllTime = Utils.parseBoolean(
                        uri.getQueryParameter(Plans.PARAM_HIDE_ALLTIME), false);
                long date = Utils.parseLong(uri.getQueryParameter(Plans.PARAM_DATE), -1L);
                if (date < 0L) {
                    // round up minutes for SQL caching
                    date = ((System.currentTimeMillis() / CallMeter.HUNDRED) + 1)
                            * CallMeter.HUNDRED;
                }
                final Calendar now = Calendar.getInstance();
                now.setTimeInMillis(date);
                final Calendar today = (Calendar) now.clone();
                today.set(Calendar.MILLISECOND, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.HOUR_OF_DAY, 0);
                String billps;
                String nbillps;
                long lowBp = -1L;
                long highBp = -1L;
                Cursor cursor = db.query(Plans.TABLE, new String[]{Plans.ID, Plans.BILLPERIOD,
                        Plans.BILLDAY}, Plans.WHERE_BILLPERIODS, null, null, null, null);
                if (cursor.moveToFirst()) {
                    billps = "(CASE ";
                    nbillps = "(CASE ";
                    do {
                        int period = cursor.getInt(1);
                        long bday = cursor.getLong(2);
                        Calendar bd = Plans.getBillDay(period, bday, now, false);
                        Calendar nbd = Plans.getBillDay(period, bd, now, true);
                        final long pid = cursor.getLong(0);
                        final long lbtime = bd.getTimeInMillis();
                        final long hbtime = nbd.getTimeInMillis();
                        if (hideAllTime && period != BILLPERIOD_INFINITE
                                && (lowBp < 0L || lowBp > lbtime)) {
                            lowBp = lbtime;
                        }
                        if (period != BILLPERIOD_INFINITE && (highBp < 0L || highBp < hbtime)) {
                            highBp = hbtime;
                        }
                        billps += " WHEN " + Plans.TABLE + "." + Plans.ID + "=" + pid + " or ("
                                + Plans.TABLE + "." + Plans.TYPE + "!=" + TYPE_BILLPERIOD + " and "
                                + Plans.TABLE + "." + Plans.BILLPERIOD_ID + "=" + pid + ") THEN "
                                + lbtime;
                        nbillps += " WHEN " + Plans.TABLE + "." + Plans.ID + "=" + pid + " or ("
                                + Plans.TABLE + "." + Plans.TYPE + "!=" + TYPE_BILLPERIOD + " and "
                                + Plans.TABLE + "." + Plans.BILLPERIOD_ID + "=" + pid + ") THEN "
                                + hbtime;
                    } while (cursor.moveToNext());
                    billps += " ELSE 0 END)";
                    nbillps += " ELSE 0 END)";
                } else {
                    billps = "0";
                    nbillps = "0";
                }
                cursor.close();
                String logDate = "";
                if (lowBp > 0L) {
                    logDate = Logs.TABLE + "." + Logs.DATE + ">" + lowBp + " and ";
                }
                if (highBp > 0L) {
                    logDate += Logs.TABLE + "." + Logs.DATE + "<" + highBp + " and ";
                }

                qb.setTables(
                        Plans.TABLE + " left outer join " + Logs.TABLE + " on (" + logDate + "("
                                + Logs.TABLE + "." + Logs.PLAN_ID + "=" + Plans.TABLE + "."
                                + Plans.ID + " or "
                                + Plans.TABLE + "." + Plans.MERGED_PLANS + " like '%,'||"
                                + Logs.TABLE + "."
                                + Logs.PLAN_ID + "||',%' or (" + Plans.TABLE + "." + Plans.TYPE
                                + "="
                                + TYPE_BILLPERIOD + " and (" + Logs.TABLE + "." + Logs.PLAN_ID
                                + " in (select "
                                + Plans.ID + " from " + Plans.TABLE + " as p where p."
                                + Plans.BILLPERIOD_ID
                                + "=" + Plans.TABLE + "." + Plans.ID + ") or 1 in (select 1 from "
                                + Plans.TABLE + " as pp where pp." + Plans.MERGED_PLANS
                                + " like '%,'||"
                                + Logs.TABLE + "." + Logs.PLAN_ID + "||'%,'" + " and pp."
                                + Plans.BILLPERIOD_ID
                                + "=" + Plans.TABLE + "." + Plans.ID + ")))))");
                groupBy = Plans.TABLE + "." + Plans.ID;
                if (hideZero || hideNoCost) {
                    having = Plans.TYPE + " in(" + TYPE_BILLPERIOD + "," + TYPE_SPACING + ","
                            + TYPE_TITLE + ")";

                    if (hideZero) {
                        having += " or " + Plans.SUM_BP_BILLED_AMOUNT + ">0";
                    }

                    if (hideNoCost) {
                        if (hideZero) {
                            having += " and ";
                        } else {
                            having += " or ";
                        }
                        having += Plans.SUM_COST + ">0";
                    }
                }

                if (orderBy == null) {
                    orderBy = Plans.DEFAULT_ORDER;
                }
                proj = new String[l];

                int s = 0;
                if (projection == Plans.PROJECTION_SUM) {
                    s = Plans.INDEX_SUM_NOW;
                }
                for (int i = 0; i < l; i++) {
                    if (i >= s) {
                        proj[i] = projection[i]
                                .replace("{" + Plans.SUM_BILLDAY + "}", billps)
                                .replace("{" + Plans.SUM_NEXTBILLDAY + "}", nbillps)
                                .replace("{" + Plans.SUM_NOW + "}", String.valueOf(date))
                                .//
                                        replace("{" + Plans.SUM_TODAY + "}",
                                        String.valueOf(today.getTimeInMillis()));
                    } else {
                        proj[i] = projection[i];
                    }
                    Log.d(TAG, "proj[", i, "]: ", proj[i]);
                }
                if (projection == Plans.PROJECTION_SUM) {
                    if (hideToday) {
                        proj[Plans.INDEX_SUM_TD_BILLED_AMOUNT] = "0 AS "
                                + Plans.SUM_TD_BILLED_AMOUNT;
                        proj[Plans.INDEX_SUM_TD_COUNT] = "0 AS " + Plans.SUM_TD_COUNT;
                    }
                    if (hideAllTime) {
                        proj[Plans.INDEX_SUM_AT_BILLED_AMOUNT] = "0 AS "
                                + Plans.SUM_AT_BILLED_AMOUNT;
                        proj[Plans.INDEX_SUM_AT_COUNT] = "0 AS " + Plans.SUM_AT_COUNT;
                    }
                }
                break;
            case RULES_ID:
                qb.appendWhere(Rules.ID + "=" + ContentUris.parseId(uri));
            case RULES:
                qb.setTables(Rules.TABLE);
                if (orderBy == null) {
                    orderBy = Rules.DEFAULT_ORDER;
                }
                break;
            case NUMBERS_GID:
                qb.appendWhere(Numbers.GID + "=" + ContentUris.parseId(uri));
                qb.setTables(Numbers.TABLE);
                break;
            case NUMBERS_ID:
                qb.appendWhere(Numbers.ID + "=" + ContentUris.parseId(uri));
            case NUMBERS:
                qb.setTables(Numbers.TABLE);
                break;
            case NUMBERS_GROUP_ID:
                qb.appendWhere(NumbersGroup.ID + "=" + ContentUris.parseId(uri));
            case NUMBERS_GROUP:
                qb.setTables(NumbersGroup.TABLE);
                break;
            case HOURS_GID:
                qb.appendWhere(Hours.GID + "=" + ContentUris.parseId(uri));
                qb.setTables(Hours.TABLE);
                break;
            case HOURS_ID:
                qb.appendWhere(Hours.ID + "=" + ContentUris.parseId(uri));
            case HOURS:
                qb.setTables(Hours.TABLE);
                break;
            case HOURS_GROUP_ID:
                qb.appendWhere(HoursGroup.ID + "=" + ContentUris.parseId(uri));
            case HOURS_GROUP:
                qb.setTables(HoursGroup.TABLE);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri " + uri);
        }

        if (proj == null) {
            proj = projection;
        }
        // Run the query
        Log.d(TAG, "qb.query() start: ", selection);
        c = qb.query(db, proj, selection, selectionArgs, groupBy, having, orderBy);
        Log.d(TAG, "qb.query() end: ", selection);

        // Tell the cursor what uri to watch, so it knows when its source data
        // changes
        c.setNotificationUri(getContext().getContentResolver(), uri);
        Log.d(TAG, "query(", uri, ", sel): ", c.getCount());
        return c;
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection,
            final String[] selectionArgs) {
        Log.d(TAG, "update(", uri, ",", selection, ", values)");
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        assert db != null;
        //noinspection ConstantConditions
        ContentResolver cr = getContext().getContentResolver();
        assert cr != null;
        long i;
        int ret;
        switch (URI_MATCHER.match(uri)) {
            case LOGS:
                ret = db.update(Logs.TABLE, values, selection, selectionArgs);
                break;
            case LOGS_ID:
                ret = db.update(Logs.TABLE, values,
                        DbUtils.sqlAnd(Logs.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case PLANS:
                ret = db.update(Plans.TABLE, values, selection, selectionArgs);
                break;
            case PLANS_ID:
                ret = db.update(Plans.TABLE, values,
                        DbUtils.sqlAnd(Plans.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case RULES_ID:
                ret = db.update(Rules.TABLE, values,
                        DbUtils.sqlAnd(Rules.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case NUMBERS_ID:
                ret = db.update(Numbers.TABLE, values,
                        DbUtils.sqlAnd(Numbers.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                if (ret > 0 && values != null) {
                    //noinspection ConstantConditions
                    i = values.getAsLong(Numbers.GID);
                    if (i >= 0) {
                        cr.notifyChange(ContentUris.withAppendedId(Numbers.GROUP_URI, i),
                                null);
                    }
                }
                break;
            case NUMBERS_GROUP_ID:
                ret = db.update(NumbersGroup.TABLE, values,
                        DbUtils.sqlAnd(NumbersGroup.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            case HOURS_ID:
                ret = db.update(Hours.TABLE, values,
                        DbUtils.sqlAnd(Hours.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                if (ret > 0 && values != null) {
                    //noinspection ConstantConditions
                    i = values.getAsLong(Numbers.GID);
                    if (i >= 0) {
                        cr.notifyChange(ContentUris.withAppendedId(Hours.GROUP_URI, i),
                                null);
                    }
                }
                break;
            case HOURS_GROUP_ID:
                ret = db.update(HoursGroup.TABLE, values,
                        DbUtils.sqlAnd(HoursGroup.ID + "=" + ContentUris.parseId(uri), selection),
                        selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown Uri " + uri);
        }
        if (ret > 0) {
            cr.notifyChange(uri, null);
        }
        Log.d(TAG, "update(): ", ret);
        return ret;
    }
}
