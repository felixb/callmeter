/*
 * Copyright (C) 2009-2010 Felix Bechstein, The Android Open Source Project
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;

/**
 * @author flx
 */
public final class DataProvider extends ContentProvider {
	/** Tag for output. */
	private static final String TAG = "dp";

	/** Callmeter's package name. */
	public static final String PACKAGE = "de.ub0r.android.callmeter";

	/** Authority. */
	public static final String AUTHORITY = PACKAGE + ".provider";

	/** Name of the {@link SQLiteDatabase}. */
	private static final String DATABASE_NAME = "callmeter.db";
	/** Version of the {@link SQLiteDatabase}. */
	private static final int DATABASE_VERSION = 16;

	/** Version of the export file. */
	private static final int EXPORT_VERSION = 0;
	/** Separator of values. */
	private static final String EXPORT_VALUESEPARATOR = ":#:";
	/** Mime type for export. */
	public static final String EXPORT_MIMETYPE = // .
	"application/android.callmeter.export";
	/** {@link Uri} for export Content. */
	public static final Uri EXPORT_URI = Uri.parse("content://" + AUTHORITY
			+ "/export");
	/** {@link Uri} for the actual export file. */
	public static final String EXPORT_FILE = "ruleset.export";

	/** Type of log: title. */
	public static final int TYPE_TITLE = 0;
	/** Type of log: spacing. */
	public static final int TYPE_SPACING = 1;
	/** Type of log: billmode. */
	public static final int TYPE_BILLPERIOD = 2;
	/** Type of log: mixed. */
	public static final int TYPE_MIXED = 3;
	/** Type of log: call. */
	public static final int TYPE_CALL = 4;
	/** Type of log: sms. */
	public static final int TYPE_SMS = 5;
	/** Type of log: mms. */
	public static final int TYPE_MMS = 6;
	/** Type of log: data. */
	public static final int TYPE_DATA = 7;

	/** Direction of log: in. */
	public static final int DIRECTION_IN = 0;
	/** Direction of log: out. */
	public static final int DIRECTION_OUT = 1;

	/** Type of limit: none. */
	public static final int LIMIT_TYPE_NONE = 0;
	/** Type of limit: units. */
	public static final int LIMIT_TYPE_UNITS = 1;
	/** Type of limit: cost. */
	public static final int LIMIT_TYPE_COST = 2;

	/** Bill period: one day. */
	public static final int BILLPERIOD_DAY = 0;
	/** Bill period: one week. */
	public static final int BILLPERIOD_WEEK = 1;
	/** Bill period: 30 days. */
	public static final int BILLPERIOD_30D = 2;
	/** Bill period: 60 days. */
	public static final int BILLPERIOD_60D = 3;
	/** Bill period: 90 days. */
	public static final int BILLPERIOD_90D = 4;
	/** Bill period: 1 month. */
	public static final int BILLPERIOD_1MONTH = 5;
	/** Bill period: 2 month. */
	public static final int BILLPERIOD_2MONTH = 6;
	/** Bill period: 3 month. */
	public static final int BILLPERIOD_3MONTH = 7;
	/** Bill period: infinite. */
	public static final int BILLPERIOD_INFINITE = 8;

	/** Plan/rule id: not yet calculated. */
	public static final int NO_ID = -1;
	/** Plan/rule id: no plan/rule found. */
	public static final int NOT_FOUND = -2;

	/**
	 * Logs.
	 * 
	 * @author flx
	 */
	public static final class Logs {
		/** Table name. */
		private static final String TABLE = "logs";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Index in projection: ID. */
		public static final int INDEX_ID = 0;
		/** Index in projection: ID of plan this log is billed in. */
		public static final int INDEX_PLAN_ID = 1;
		/** Index in projection: ID of rule this log was matched. */
		public static final int INDEX_RULE_ID = 2;
		/** Index in projection: Type of log. */
		public static final int INDEX_TYPE = 3;
		/** Index in projection: Direction of log. */
		public static final int INDEX_DIRECTION = 4;
		/** Index in projection: Date. */
		public static final int INDEX_DATE = 5;
		/** Index in projection: Amount. */
		public static final int INDEX_AMOUNT = 6;
		/** Index in projection: Billed amount. */
		public static final int INDEX_BILL_AMOUNT = 7;
		/** Index in projection: Remote part. */
		public static final int INDEX_REMOTE = 8;
		/** Index in projection: Roamed? */
		public static final int INDEX_ROAMED = 9;
		/** Index in projection: Cost. */
		public static final int INDEX_COST = 10;

		/** Index in projection - sum: ID of plan this log is billed in. */
		public static final int INDEX_SUM_PLAN_ID = 0;
		/** Index in projection - sum: Amount. */
		public static final int INDEX_SUM_AMOUNT = 1;
		/** Index in projection - sum: Billed amount. */
		public static final int INDEX_SUM_BILL_AMOUNT = 2;
		/** Index in projection - sum: Cost. */
		public static final int INDEX_SUM_COST = 3;
		/** Index in projection - count. */
		public static final int INDEX_SUM_COUNT = 4;

		/** ID. */
		public static final String ID = "_id";
		/** ID of plan this log is billed in. */
		public static final String PLAN_ID = "_plan_id";
		/** ID of rule this log was matched. */
		public static final String RULE_ID = "_rule_id";
		/** Type of log. */
		public static final String TYPE = "_type";
		/** Direction of log. */
		public static final String DIRECTION = "_direction";
		/** Date of log. */
		public static final String DATE = "_date";
		/** Amount. */
		public static final String AMOUNT = "_amount";
		/** Billed amount. */
		public static final String BILL_AMOUNT = "_bill_amount";
		/** Remote part. */
		public static final String REMOTE = "_remote";
		/** Roamed? */
		public static final String ROAMED = "_roamed";
		/** Cost. */
		public static final String COST = "_logs_cost";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, PLAN_ID,
				RULE_ID, TYPE, DIRECTION, DATE, AMOUNT, BILL_AMOUNT, REMOTE,
				ROAMED, COST };
		/** Projection used for query - sum. */
		public static final String[] PROJECTION_SUM = new String[] { PLAN_ID,
				"sum(" + AMOUNT + ")", "sum(" + BILL_AMOUNT + ")",
				"sum(" + COST + ")", "count(" + PLAN_ID + ")" };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/logs");
		/** Content {@link Uri} - sum. */
		public static final Uri SUM_URI = Uri.parse("content://" + AUTHORITY
				+ "/logs/sum");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.log";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.log";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			PROJECTION_MAP.put(ID, ID);
			PROJECTION_MAP.put(PLAN_ID, PLAN_ID);
			PROJECTION_MAP.put(RULE_ID, RULE_ID);
			PROJECTION_MAP.put(TYPE, TYPE);
			PROJECTION_MAP.put(DIRECTION, DIRECTION);
			PROJECTION_MAP.put(DATE, DATE);
			PROJECTION_MAP.put(AMOUNT, AMOUNT);
			PROJECTION_MAP.put(BILL_AMOUNT, BILL_AMOUNT);
			PROJECTION_MAP.put(REMOTE, REMOTE);
			PROJECTION_MAP.put(ROAMED, ROAMED);
			PROJECTION_MAP.put(COST, COST);
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ PLAN_ID + " LONG," // .
					+ RULE_ID + " LONG," // .
					+ TYPE + " INTEGER," // .
					+ DIRECTION + " INTEGER," // .
					+ DATE + " LONG," // .
					+ AMOUNT + " LONG," // .
					+ BILL_AMOUNT + " LONG," // .
					+ REMOTE + " TEXT,"// .
					+ ROAMED + " INTEGER," // .
					+ COST + " FLOAT"// .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, new String[] {
					AMOUNT, DATE, DIRECTION, REMOTE, ROAMED, TYPE }, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private Logs() {
			// nothing here.
		}
	}

	/**
	 * Plans.
	 * 
	 * @author flx
	 */
	public static final class Plans {
		/** Table name. */
		private static final String TABLE = "plans";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Index in projection: id. */
		public static final int INDEX_ID = 0;
		/** Index in projection: order. */
		public static final int INDEX_ORDER = 1;
		/** Index in projection: name. */
		public static final int INDEX_NAME = 2;
		/** Index in projection: short name. */
		public static final int INDEX_SHORTNAME = 3;
		/** Index in projection: type. */
		public static final int INDEX_TYPE = 4;
		/** Index in projection: Type of limit. */
		public static final int INDEX_LIMIT_TYPE = 5;
		/** Index in projection: limit. */
		public static final int INDEX_LIMIT = 6;
		/** Index in projection: Billmode. */
		public static final int INDEX_BILLMODE = 7;
		/** Index in projection: Billday. */
		public static final int INDEX_BILLDAY = 8;
		/** Index in projection: type of billperiod. */
		public static final int INDEX_BILLPERIOD = 9;
		/** Index in projection: Cost per item. */
		public static final int INDEX_COST_PER_ITEM = 10;
		/** Index in projection: Cost per amount1. */
		public static final int INDEX_COST_PER_AMOUNT1 = 11;
		/** Index in projection: Cost per amount2. */
		public static final int INDEX_COST_PER_AMOUNT2 = 12;
		/** Index in projection: Cost per item in limit. */
		public static final int INDEX_COST_PER_ITEM_IN_LIMIT = 13;
		/** Index in projection: Cost per amount1 in limit. */
		public static final int INDEX_COST_PER_AMOUNT_IN_LIMIT1 = 14;
		/** Index in projection: Cost per amount2 in limit. */
		public static final int INDEX_COST_PER_AMOUNT_IN_LIMIT2 = 15;
		/** Index in projection: Cost per plan. */
		public static final int INDEX_COST_PER_PLAN = 16;
		/** Index in projection: Mixed units for call. */
		public static final int INDEX_MIXED_UNITS_CALL = 17;
		/** Index in projection: Mixed units for sms. */
		public static final int INDEX_MIXED_UNITS_SMS = 18;
		/** Index in projection: Mixed units for mms. */
		public static final int INDEX_MIXED_UNITS_MMS = 19;
		/** Index in projection: cache row for main: string. */
		public static final int INDEX_CACHE_STRING = 20;
		/** Index in projection: cache row for main: progressbar maximum. */
		public static final int INDEX_CACHE_PROGRESS_MAX = 21;
		/** Index in projection: cache row for main: progressbar position. */
		public static final int INDEX_CACHE_PROGRESS_POS = 22;
		/** Index in projection: cache row for main: cost. */
		public static final int INDEX_CACHE_COST = 23;
		/** Index in projection: id of billperiod. */
		public static final int INDEX_BILLPERIOD_ID = 24;
		/** Index in projection: next alert. */
		public static final int INDEX_NEXT_ALERT = 25;

		/** ID. */
		public static final String ID = "_id";
		/** Order. */
		public static final String ORDER = "_order";
		/** Name. */
		public static final String NAME = "_plan_name";
		/** Short name. */
		public static final String SHORTNAME = "_shortname";
		/** Type of log. */
		public static final String TYPE = "_plan_type";
		/** Type of limit. */
		public static final String LIMIT_TYPE = "_limit_type";
		/** Limit. */
		public static final String LIMIT = "_limit";
		/** Billmode. */
		public static final String BILLMODE = "_billmode";
		/** Billday. */
		public static final String BILLDAY = "_billday";
		/** Type of billperiod. */
		public static final String BILLPERIOD = "_billperiod";
		/** Id of billperiod. */
		public static final String BILLPERIOD_ID = "_billperiod_id";
		/** Cost per item. */
		public static final String COST_PER_ITEM = "_cost_per_item";
		/** Cost per amount1. */
		public static final String COST_PER_AMOUNT1 = "_cost_per_amount1";
		/** Cost per amount2. */
		public static final String COST_PER_AMOUNT2 = "_cost_per_amount2";
		/** Cost per item in limit. */
		public static final String COST_PER_ITEM_IN_LIMIT = // .
		"_cost_per_item_in_limit";
		/** Cost per amount1 in limit. */
		public static final String COST_PER_AMOUNT_IN_LIMIT1 = // .
		"_cost_per_amount_in_limit1";
		/** Cost per amount2 in limit. */
		public static final String COST_PER_AMOUNT_IN_LIMIT2 = // .
		"_cost_per_amount_in_limit2";
		/** Cost per plan. */
		public static final String COST_PER_PLAN = "_cost_per_plan";
		/** Mixed units for call. */
		public static final String MIXED_UNITS_CALL = "_mixed_units_call";
		/** Mixed units for sms. */
		public static final String MIXED_UNITS_SMS = "_mixed_units_sms";
		/** Mixed units for mms. */
		public static final String MIXED_UNITS_MMS = "_mixed_units_mms";
		/** Cache row for main: string. */
		public static final String CACHE_STRING = "_cache_str";
		/** Cache row for main: progressbar maximum. */
		public static final String CACHE_PROGRESS_MAX = "_cache_prg_max";
		/** Cache row for main: progressbar position. */
		public static final String CACHE_PROGRESS_POS = "_cache_prg_pos";
		/** Cache row for main: cost. */
		public static final String CACHE_COST = "_cache_cost";
		/** Next alert. */
		public static final String NEXT_ALERT = "_next_alert";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, ORDER,
				NAME, SHORTNAME, TYPE, LIMIT_TYPE, LIMIT, BILLMODE, BILLDAY,
				BILLPERIOD, COST_PER_ITEM, COST_PER_AMOUNT1, COST_PER_AMOUNT2,
				COST_PER_ITEM_IN_LIMIT, COST_PER_AMOUNT_IN_LIMIT1,
				COST_PER_AMOUNT_IN_LIMIT2, COST_PER_PLAN, MIXED_UNITS_CALL,
				MIXED_UNITS_SMS, MIXED_UNITS_MMS, CACHE_STRING,
				CACHE_PROGRESS_MAX, CACHE_PROGRESS_POS, CACHE_COST,
				BILLPERIOD_ID, NEXT_ALERT };

		/** Select only real plans. */
		public static final String WHERE_REALPLANS = TYPE + " != "
				+ TYPE_BILLPERIOD + " AND " + TYPE + " != " + TYPE_SPACING
				+ " AND " + TYPE + " != " + TYPE_TITLE;

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/plans");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.plan";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.plan";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			PROJECTION_MAP.put(ID, ID);
			PROJECTION_MAP.put(ORDER, ORDER);
			PROJECTION_MAP.put(NAME, NAME);
			PROJECTION_MAP.put(SHORTNAME, SHORTNAME);
			PROJECTION_MAP.put(TYPE, TYPE);
			PROJECTION_MAP.put(LIMIT_TYPE, LIMIT_TYPE);
			PROJECTION_MAP.put(LIMIT, LIMIT);
			PROJECTION_MAP.put(BILLMODE, BILLMODE);
			PROJECTION_MAP.put(BILLDAY, BILLDAY);
			PROJECTION_MAP.put(BILLPERIOD, BILLPERIOD);
			PROJECTION_MAP.put(BILLPERIOD_ID, BILLPERIOD_ID);
			PROJECTION_MAP.put(COST_PER_ITEM, COST_PER_ITEM);
			PROJECTION_MAP.put(COST_PER_AMOUNT1, COST_PER_AMOUNT1);
			PROJECTION_MAP.put(COST_PER_AMOUNT2, COST_PER_AMOUNT2);
			PROJECTION_MAP.put(COST_PER_ITEM_IN_LIMIT, COST_PER_ITEM_IN_LIMIT);
			PROJECTION_MAP.put(COST_PER_AMOUNT_IN_LIMIT1,
					COST_PER_AMOUNT_IN_LIMIT1);
			PROJECTION_MAP.put(COST_PER_AMOUNT_IN_LIMIT2,
					COST_PER_AMOUNT_IN_LIMIT2);
			PROJECTION_MAP.put(COST_PER_PLAN, COST_PER_PLAN);
			PROJECTION_MAP.put(MIXED_UNITS_CALL, MIXED_UNITS_CALL);
			PROJECTION_MAP.put(MIXED_UNITS_SMS, MIXED_UNITS_SMS);
			PROJECTION_MAP.put(MIXED_UNITS_MMS, MIXED_UNITS_MMS);
			PROJECTION_MAP.put(CACHE_STRING, CACHE_STRING);
			PROJECTION_MAP.put(CACHE_PROGRESS_MAX, CACHE_PROGRESS_MAX);
			PROJECTION_MAP.put(CACHE_PROGRESS_POS, CACHE_PROGRESS_POS);
			PROJECTION_MAP.put(CACHE_COST, CACHE_COST);
			PROJECTION_MAP.put(NEXT_ALERT, NEXT_ALERT);
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ ORDER + " INTEGER," // .
					+ NAME + " TEXT,"// .
					+ SHORTNAME + " TEXT,"// .
					+ TYPE + " TEXT, " // .
					+ LIMIT_TYPE + " INTEGER,"// .
					+ LIMIT + " LONG,"// .
					+ BILLMODE + " TEXT,"// .
					+ BILLDAY + " LONG,"// .
					+ BILLPERIOD + " INTEGER,"// .
					+ BILLPERIOD_ID + " LONG,"// .
					+ COST_PER_ITEM + " FLOAT,"// .
					+ COST_PER_AMOUNT1 + " FLOAT,"// .
					+ COST_PER_AMOUNT2 + " FLOAT,"// .
					+ COST_PER_ITEM_IN_LIMIT + " FLOAT,"// .
					+ COST_PER_AMOUNT_IN_LIMIT1 + " FLOAT,"// .
					+ COST_PER_AMOUNT_IN_LIMIT2 + " FLOAT,"// .
					+ COST_PER_PLAN + " FLOAT," // .
					+ MIXED_UNITS_CALL + " INTEGER,"// .
					+ MIXED_UNITS_SMS + " INTEGER,"// .
					+ MIXED_UNITS_MMS + " INTEGER,"// .
					+ CACHE_STRING + " TEXT," // .
					+ CACHE_PROGRESS_MAX + " INTEGER," // .
					+ CACHE_PROGRESS_POS + " INTEGER," // .
					+ CACHE_COST + " FLOAT," // .
					+ NEXT_ALERT + " LONG" // .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private Plans() {
			// nothing here.
		}

		/**
		 * Get Name for id.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param id
		 *            id
		 * @return name
		 */
		public static String getName(final ContentResolver cr, final long id) {
			if (id < 0) {
				return null;
			}
			final Cursor cursor = cr.query(ContentUris.withAppendedId(
					CONTENT_URI, id), new String[] { NAME }, null, null, null);
			String ret = null;
			if (cursor != null && cursor.moveToFirst()) {
				ret = cursor.getString(0);
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			return ret;
		}

		/**
		 * Calculate limit set for given plan.
		 * 
		 * @param pType
		 *            type of plan
		 * @param lType
		 *            type of limit
		 * @param amount
		 *            billed amount
		 * @param cost
		 *            billed cost
		 * @return get used
		 */
		public static int getUsed(final int pType, final int lType,
				final long amount, final float cost) {
			switch (lType) {
			case DataProvider.LIMIT_TYPE_COST:
				return (int) (cost * CallMeter.HUNDRET);
			case DataProvider.LIMIT_TYPE_UNITS:
				switch (pType) {
				case DataProvider.TYPE_DATA:
					return (int) (amount / CallMeter.BYTE_KB);
				default:
					return (int) amount;
				}
			default:
				return 0;
			}
		}

		/**
		 * Calculate limit set for given plan.
		 * 
		 * @param pType
		 *            type of plan
		 * @param lType
		 *            type of limit
		 * @param limit
		 *            limit
		 * @return get limit
		 */
		public static long getLimit(final int pType, final int lType,
				final long limit) {
			if (limit == 0L) {
				return 0L;
			}
			switch (lType) {
			case DataProvider.LIMIT_TYPE_UNITS:
				switch (pType) {
				case DataProvider.TYPE_DATA:
					return limit * CallMeter.BYTE_KB;
				case DataProvider.TYPE_CALL:
					return limit * CallMeter.SECONDS_MINUTE;
				default:
					return limit;
				}
			case DataProvider.LIMIT_TYPE_COST:
				return limit * CallMeter.HUNDRET;
			default:
				return 0L;
			}

		}

		/**
		 * Get the SQL {@link String} selecting the bill period.
		 * 
		 * @param period
		 *            type of period
		 * @param start
		 *            first bill day set.
		 * @param now
		 *            move now to some other time, null == real now
		 * @return SQL {@link String} selecting the bill period
		 */
		public static String getBilldayWhere(final int period,
				final long start, final Calendar now) {
			Calendar s = Calendar.getInstance();
			s.setTimeInMillis(start);
			return getBilldayWhere(period, s, now);
		}

		/**
		 * Get the SQL {@link String} selecting the bill period.
		 * 
		 * @param period
		 *            type of period
		 * @param start
		 *            first bill day set.
		 * @param now
		 *            move now to some other time, null == real now
		 * @return SQL {@link String} selecting the bill period
		 */
		public static String getBilldayWhere(final int period,
				final Calendar start, final Calendar now) {
			final Calendar bd = getBillDay(period, start, now, false);
			if (bd == null) {
				return null;
			}
			final Calendar nbd = getBillDay(period, start, now, true);
			return DataProvider.Logs.DATE + " > " + bd.getTimeInMillis()
					+ " AND " + DataProvider.Logs.DATE + " < "
					+ nbd.getTimeInMillis();
		}

		/**
		 * Get the first bill day of this period.
		 * 
		 * @param period
		 *            type of period
		 * @param start
		 *            first bill day set.
		 * @param now
		 *            move now to some other time, null == real now
		 * @param next
		 *            get the next, not the current one
		 * @return {@link Calendar} with current first bill day
		 */
		public static Calendar getBillDay(final int period, // .
				final long start, final Calendar now, final boolean next) {
			Calendar s = Calendar.getInstance();
			s.setTimeInMillis(start);
			return getBillDay(period, s, now, next);
		}

		/**
		 * Get the first bill day of this period.
		 * 
		 * @param period
		 *            type of period
		 * @param start
		 *            first bill day set.
		 * @param now
		 *            move now to some other time, null == real now
		 * @param next
		 *            get the next, not the current one
		 * @return {@link Calendar} with current first bill day
		 */
		public static Calendar getBillDay(final int period, // .
				final Calendar start, final Calendar now, final boolean next) {
			int f;
			int v;
			switch (period) {
			case BILLPERIOD_INFINITE:
				return null;
			case BILLPERIOD_DAY:
				Calendar ret;
				if (now == null) {
					ret = Calendar.getInstance();
				} else {
					ret = (Calendar) now.clone();
				}
				ret.set(ret.get(Calendar.YEAR), ret.get(Calendar.MONTH), ret
						.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
				if (next) {
					ret.add(Calendar.DAY_OF_MONTH, 1);
				}
				return ret;
			case BILLPERIOD_30D:
				f = Calendar.DAY_OF_MONTH;
				v = 30;
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
			case BILLPERIOD_WEEK:
				f = Calendar.DAY_OF_MONTH;
				v = 7;
				break;
			default:
				return null;
			}

			final Calendar ret = (Calendar) start.clone();
			Calendar n = now;
			if (n == null) {
				n = Calendar.getInstance();
			}

			while (ret.after(n)) {
				ret.add(f, v * -1);
			}
			while (ret.before(n)) {
				ret.add(f, v);
			}
			if (!next) {
				ret.add(f, v * -1);
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
		/** Condition type: match call. */
		public static final int WHAT_CALL = 0;
		/** Condition type: match sms. */
		public static final int WHAT_SMS = 1;
		/** Condition type: match mms. */
		public static final int WHAT_MMS = 2;
		/** Condition type: match data. */
		public static final int WHAT_DATA = 3;
		/** Condition type: is incomming. */
		public static final int WHAT_INCOMMING = 4;
		/** Condition type: is roaming. */
		public static final int WHAT_ROAMING = 5;
		/** Condition type: match numbers. */
		public static final int WHAT_NUMBERS = 6;
		/** Condition type: match hours. */
		public static final int WHAT_HOURS = 7;
		/** Condition type: is limit reached. */
		public static final int WHAT_LIMIT_REACHED = 8;

		/** Table name. */
		private static final String TABLE = "rules";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Index in projection: id. */
		public static final int INDEX_ID = 0;
		/** Index in projection: order. */
		public static final int INDEX_ORDER = 1;
		/** Index in projection: ID of plan referred by this rule. */
		public static final int INDEX_PLAN_ID = 2;
		/** Index in projection: Name. */
		public static final int INDEX_NAME = 3;
		/** Index in projection: Negate rule? */
		public static final int INDEX_NOT = 4;
		/** Index in projection: Kind of rule. */
		public static final int INDEX_WHAT = 5;
		/** Index in projection: Target 0. */
		public static final int INDEX_WHAT0 = 6;
		/** Index in projection: Target 1. */
		public static final int INDEX_WHAT1 = 7;
		/** Index in projection: is child? */
		public static final int INDEX_ISCHILD = 8;

		/** ID. */
		public static final String ID = "_id";
		/** Order. */
		public static final String ORDER = "_order";
		/** ID of plan referred by this rule. */
		public static final String PLAN_ID = "_plan_id";
		/** Name. */
		public static final String NAME = "_rule_name";
		/** Negate rule? */
		public static final String NOT = "_not";
		/** Kind of rule. */
		public static final String WHAT = "_what";
		/** Target 0. */
		public static final String WHAT0 = "_what0";
		/** Target 1. */
		public static final String WHAT1 = "_what1";
		/** Is child? */
		public static final String ISCHILD = "_ischild";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, ORDER,
				PLAN_ID, NAME, NOT, WHAT, WHAT0, WHAT1, ISCHILD };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/rules");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.rule";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.rule";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			PROJECTION_MAP.put(ID, ID);
			PROJECTION_MAP.put(ORDER, ORDER);
			PROJECTION_MAP.put(NAME, NAME);
			PROJECTION_MAP.put(PLAN_ID, PLAN_ID);
			PROJECTION_MAP.put(NOT, NOT);
			PROJECTION_MAP.put(WHAT, WHAT);
			PROJECTION_MAP.put(WHAT0, WHAT0);
			PROJECTION_MAP.put(WHAT1, WHAT1);
			PROJECTION_MAP.put(ISCHILD, ISCHILD);
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ ORDER + " INTEGER," // .
					+ NAME + " TEXT,"// .
					+ PLAN_ID + " INTEGER,"// .
					+ NOT + " INTEGER,"// .
					+ WHAT + " INTEGER,"// .
					+ WHAT0 + " LONG,"// .
					+ WHAT1 + " LONG,"// .
					+ ISCHILD + " INTEGER" // .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private Rules() {
			// nothing here.
		}

		/**
		 * Get Name for id.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param id
		 *            id
		 * @return name
		 */
		public static String getName(final ContentResolver cr, final long id) {
			if (id < 0) {
				return null;
			}
			final Cursor cursor = cr.query(ContentUris.withAppendedId(
					CONTENT_URI, id), new String[] { NAME }, null, null, null);
			String ret = null;
			if (cursor != null && cursor.moveToFirst()) {
				ret = cursor.getString(0);
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			return ret;
		}
	}

	/**
	 * Numbers.
	 * 
	 * @author flx
	 */
	public static final class Numbers {
		/** Table name. */
		private static final String TABLE = "numbers";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Index in projection: ID. */
		public static final int INDEX_ID = 0;
		/** Index in projection: ID for number block. */
		public static final int INDEX_GID = 1;
		/** Index in projection: number. */
		public static final int INDEX_NUMBER = 2;

		/** ID. */
		public static final String ID = "_id";
		/** ID for number block. */
		public static final String GID = "_gid";
		/** Number. */
		public static final String NUMBER = "_number";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, GID,
				NUMBER };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/numbers");
		/** Content {@link Uri} for a group of numbers. */
		public static final Uri GROUP_URI = Uri.parse("content://" + AUTHORITY
				+ "/numbers/group");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.number";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.number";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			PROJECTION_MAP.put(ID, ID);
			PROJECTION_MAP.put(GID, GID);
			PROJECTION_MAP.put(NUMBER, NUMBER);
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ GID + " LONG," // .
					+ NUMBER + " TEXT"// .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
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
		/** Table name. */
		private static final String TABLE = "numbersgroup";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Index in projection: ID. */
		public static final int INDEX_ID = 0;
		/** Index in projection: name of Numbers group. */
		public static final int INDEX_NAME = 1;

		/** ID. */
		public static final String ID = "_id";
		/** Name of Numbers group. */
		public static final String NAME = "_name";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, NAME };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/numbers/groups");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.numbergroup";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.numbergroup";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			PROJECTION_MAP.put(ID, ID);
			PROJECTION_MAP.put(NAME, NAME);
		}

		/**
		 * Get Name for id.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param id
		 *            id
		 * @return name
		 */
		public static String getName(final ContentResolver cr, final long id) {
			final Cursor cursor = cr.query(ContentUris.withAppendedId(
					CONTENT_URI, id), new String[] { NAME }, null, null, null);
			String ret = null;
			if (cursor != null && cursor.moveToFirst()) {
				ret = cursor.getString(0);
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			return ret;
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ NAME + " TEXT" // .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
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
		/** Table name. */
		private static final String TABLE = "hours";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Index in projection: ID. */
		public static final int INDEX_ID = 0;
		/** Index in projection: ID for block of hours. */
		public static final int INDEX_HOURS_ID = 1;
		/** Index in projection: Day. */
		public static final int INDEX_DAY = 2;
		/** Index in projection: Hour of day. */
		public static final int INDEX_HOUR = 3;

		/** ID. */
		public static final String ID = "_id";
		/** ID for block of hours. */
		public static final String GID = "_gid";
		/** Day. */
		public static final String DAY = "_day";
		/** Hour of day. */
		public static final String HOUR = "_hour";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, GID, DAY,
				HOUR };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/hours");
		/** Content {@link Uri} for a group of numbers. */
		public static final Uri GROUP_URI = Uri.parse("content://" + AUTHORITY
				+ "/hours/group");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.hour";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.hour";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			PROJECTION_MAP.put(ID, ID);
			PROJECTION_MAP.put(GID, GID);
			PROJECTION_MAP.put(DAY, DAY);
			PROJECTION_MAP.put(HOUR, HOUR);
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ GID + " LONG," // .
					+ DAY + " INTEGER," // .
					+ HOUR + " INTEGER" // .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
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
		/** Table name. */
		private static final String TABLE = "hoursgroup";
		/** {@link HashMap} for projection. */
		private static final HashMap<String, String> PROJECTION_MAP;

		/** Index in projection: ID. */
		public static final int INDEX_ID = 0;
		/** Index in projection: name of hours group. */
		public static final int INDEX_NAME = 1;

		/** ID. */
		public static final String ID = "_id";
		/** Name of hours group. */
		public static final String NAME = "_name";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, NAME };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/hours/groups");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.hourgroup";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.hourgroup";

		static {
			PROJECTION_MAP = new HashMap<String, String>();
			PROJECTION_MAP.put(ID, ID);
			PROJECTION_MAP.put(NAME, NAME);
		}

		/**
		 * Get Name for id.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param id
		 *            id
		 * @return name
		 */
		public static String getName(final ContentResolver cr, final long id) {
			final Cursor cursor = cr.query(ContentUris.withAppendedId(
					CONTENT_URI, id), new String[] { NAME }, null, null, null);
			String ret = null;
			if (cursor != null && cursor.moveToFirst()) {
				ret = cursor.getString(0);
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			return ret;
		}

		/**
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ NAME + " TEXT" // .
					+ ");");
		}

		/**
		 * Upgrade table.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 * @param oldVersion
		 *            old version
		 * @param newVersion
		 *            new version
		 */
		public static void onUpgrade(final SQLiteDatabase db,
				final int oldVersion, final int newVersion) {
			Log.w(TAG, "Upgrading table: " + TABLE);
			final ContentValues[] values = backup(db, TABLE, PROJECTION, null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private HoursGroup() {
			// nothing here.
		}
	}

	/** Internal id: logs. */
	private static final int LOGS = 1;
	/** Internal id: single log entry. */
	private static final int LOGS_ID = 2;
	/** Internal id: plans. */
	private static final int PLANS = 3;
	/** Internal id: single plan. */
	private static final int PLANS_ID = 4;
	/** Internal id: rules. */
	private static final int RULES = 5;
	/** Internal id: single rule. */
	private static final int RULES_ID = 6;
	/** Internal id: numbers. */
	private static final int NUMBERS = 7;
	/** Internal id: single number. */
	private static final int NUMBERS_ID = 8;
	/** Internal id: group of numbers. */
	private static final int NUMBERS_GID = 9;
	/** Internal id: number group. */
	private static final int NUMBERS_GROUP = 10;
	/** Internal id: singla number group. */
	private static final int NUMBERS_GROUP_ID = 11;
	/** Internal id: hours. */
	private static final int HOURS = 12;
	/** Internal id: single hour. */
	private static final int HOURS_ID = 13;
	/** Internal id: group of hours. */
	private static final int HOURS_GID = 14;
	/** Internal id: hours group. */
	private static final int HOURS_GROUP = 15;
	/** Internal id: single hours group. */
	private static final int HOURS_GROUP_ID = 16;
	/** Internal id: sum of logs. */
	private static final int LOGS_SUM = 17;
	/** Internal id: export. */
	private static final int EXPORT = 200;

	/** {@link UriMatcher}. */
	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "logs", LOGS);
		URI_MATCHER.addURI(AUTHORITY, "logs/#", LOGS_ID);
		URI_MATCHER.addURI(AUTHORITY, "logs/sum", LOGS_SUM);
		URI_MATCHER.addURI(AUTHORITY, "plans", PLANS);
		URI_MATCHER.addURI(AUTHORITY, "plans/#", PLANS_ID);
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
		URI_MATCHER.addURI(AUTHORITY, "export", EXPORT);
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		/** {@link Context} . */
		private final Context ctx;

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			this.ctx = context;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create database");
			Logs.onCreate(db);
			Plans.onCreate(db);
			Rules.onCreate(db);
			Numbers.onCreate(db);
			NumbersGroup.onCreate(db);
			Hours.onCreate(db);
			HoursGroup.onCreate(db);
			// import default
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					this.ctx.getResources()
							.openRawResource(R.raw.default_setup)));
			final ArrayList<String> sb = new ArrayList<String>();
			try {
				String line = reader.readLine();
				while (line != null) {
					sb.add(line);
					line = reader.readLine();
				}
			} catch (IOException e) {
				Log.e(TAG, "error reading raw data", e);
			}
			importData(db, sb.toArray(new String[] {}));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onUpgrade(final SQLiteDatabase db, final int oldVersion,
				final int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
					+ newVersion + ", which will destroy all old data");
			Logs.onUpgrade(db, oldVersion, newVersion);
			Plans.onUpgrade(db, oldVersion, newVersion);
			Rules.onUpgrade(db, oldVersion, newVersion);
			Numbers.onUpgrade(db, oldVersion, newVersion);
			NumbersGroup.onUpgrade(db, oldVersion, newVersion);
			Hours.onUpgrade(db, oldVersion, newVersion);
			HoursGroup.onUpgrade(db, oldVersion, newVersion);
			unmatch(db);
		}
	}

	/** {@link DatabaseHelper}. */
	private DatabaseHelper mOpenHelper;

	/**
	 * Run RuleMatcher.unmatch locally.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 */
	private static void unmatch(final SQLiteDatabase db) {
		Log.d(TAG, "unmatch()");
		if (db.isReadOnly()) {
			Log.e(TAG, "Database is readonly, cann not unmatch on upgrade!");
			return;
		}
		ContentValues cv = new ContentValues();
		cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
		cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
		db.update(DataProvider.Logs.TABLE, cv, null, null);
		cv.clear();
		cv.put(DataProvider.Plans.NEXT_ALERT, 0);
		db.update(DataProvider.Plans.TABLE, cv, null, null);
	}

	/**
	 * Backup a single table.
	 * 
	 * @param sb
	 *            {@link StringBuilder} for saving the data
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param table
	 *            table name
	 * @param projection
	 *            projection
	 * @param strip
	 *            strip column
	 */
	private static void backupRuleSetSub(final StringBuilder sb,
			final SQLiteDatabase db, final String table,
			final String[] projection, final String strip) {
		ContentValues[] cvs = backup(db, table, projection, strip);
		for (ContentValues cv : cvs) {
			sb.append(table);
			sb.append(" ");
			for (String k : projection) {
				final String v = cv.getAsString(k);
				if (v != null) {
					sb.append(k + ":" + v + EXPORT_VALUESEPARATOR);
				}
			}
			sb.append("\n");
		}
	}

	/**
	 * Backup rule set to String.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param descr
	 *            description of the rule set
	 * @return {@link String} representing {@link Rules} and {@link Plans}
	 */
	public static String backupRuleSet(final Context context, // .
			final String descr) {
		StringBuilder sb = new StringBuilder();
		sb.append(EXPORT_VERSION + "\n");
		sb.append(URLEncoder.encode(descr) + "\n");
		final SQLiteDatabase db = new DatabaseHelper(context)
				.getReadableDatabase();

		backupRuleSetSub(sb, db, Plans.TABLE, Plans.PROJECTION, null);
		backupRuleSetSub(sb, db, Rules.TABLE, Rules.PROJECTION, null);
		backupRuleSetSub(sb, db, Hours.TABLE, Hours.PROJECTION, null);
		backupRuleSetSub(sb, db, HoursGroup.TABLE, HoursGroup.PROJECTION, null);
		backupRuleSetSub(sb, db, Numbers.TABLE, Numbers.PROJECTION, null);
		backupRuleSetSub(sb, db, NumbersGroup.TABLE, NumbersGroup.PROJECTION,
				null);
		db.close();
		return sb.toString();
	}

	/**
	 * Import data from lines into {@link SQLiteDatabase}.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param lines
	 *            data
	 */
	private static void importData(final SQLiteDatabase db, // .
			final String[] lines) {
		final int l = lines.length;
		String table = null;
		ArrayList<ContentValues> cvs = null;
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
				cvs = new ArrayList<ContentValues>();
			} else if (!table.equals(ti[0])) {
				// reload cvs into table
				db.delete(table, null, null);
				reload(db, table, cvs.toArray(new ContentValues[1]));
				// prepare new table/cvs
				table = ti[0];
				cvs = new ArrayList<ContentValues>();
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
			cvs.add(cv);
		}
		if (table != null && table.length() > 0) {
			// reload table
			db.delete(table, null, null);
			reload(db, table, cvs.toArray(new ContentValues[1]));
		}
	}

	/**
	 * Import rule set from {@link String}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param ruleSet
	 *            rule set as {@link String}
	 */
	public static void importRuleSet(final Context context, // .
			final String ruleSet) {
		if (ruleSet == null || ruleSet.length() == 0) {
			return;
		}
		final String[] lines = ruleSet.split("\n");
		if (lines.length <= 2) {
			return;
		}
		final SQLiteDatabase db = new DatabaseHelper(context)
				.getWritableDatabase();
		importData(db, lines);
		db.close();
	}

	/**
	 * Try to backup fields from table.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param table
	 *            table
	 * @param cols
	 *            columns
	 * @param strip
	 *            column to forget on backup, eg. _id
	 * @return array of rows
	 */
	private static ContentValues[] backup(final SQLiteDatabase db,
			final String table, final String[] cols, final String strip) {
		ArrayList<ContentValues> ret = new ArrayList<ContentValues>();
		String[] proj = cols;
		if (strip != null) {
			proj = new String[cols.length - 1];
			int i = 0;
			for (String c : cols) {
				if (strip.equals(c)) {
					continue;
				}
				proj[i] = c;
				++i;
			}
		}
		final int l = proj.length;
		Cursor cursor = null;
		try {
			cursor = db.query(table, proj, null, null, null, null, null);
		} catch (SQLException e) {
			if (l == 1) {
				return null;
			}
			final String err = e.getMessage();
			if (!err.startsWith("no such column:")) {
				return null;
			}
			final String str = err.split(":", 3)[1].trim();
			return backup(db, table, proj, str);
		}
		if (cursor != null && cursor.moveToFirst()) {
			do {
				final ContentValues cv = new ContentValues();
				for (int i = 0; i < l; i++) {
					final String s = cursor.getString(i);
					if (s != null) {
						cv.put(proj[i], s);
					}
				}
				ret.add(cv);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}

		return ret.toArray(new ContentValues[0]);
	}

	/**
	 * Reload backup into table.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @param table
	 *            table
	 * @param values
	 *            {@link ContentValues}[] backed up with backup()
	 */
	private static void reload(final SQLiteDatabase db, final String table,
			final ContentValues[] values) {
		if (values == null || values.length == 0) {
			return;
		}
		Log.d(TAG, "reload(db, " + table + ", cv[" + values.length + "])");
		for (ContentValues cv : values) {
			db.insert(table, null, cv);
		}
		return;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		int ret = 0;
		long id;
		Cursor c;
		String w;
		switch (URI_MATCHER.match(uri)) {
		case LOGS:
			ret = db.delete(Logs.TABLE, selection, selectionArgs);
			break;
		case LOGS_ID:
			ret = db.delete(Logs.TABLE, DbUtils.sqlAnd(Logs.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case PLANS_ID:
			ret = db.delete(Plans.TABLE, DbUtils.sqlAnd(Plans.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case RULES_ID:
			ret = db.delete(Rules.TABLE, DbUtils.sqlAnd(Rules.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case NUMBERS_ID:
			id = ContentUris.parseId(uri);
			w = DbUtils.sqlAnd(Numbers.ID + "=" + id, selection);
			c = db.query(Numbers.TABLE, new String[] { Numbers.GID }, w,
					selectionArgs, null, null, null);
			if (c != null && c.moveToFirst()) {
				final long gid = c.getLong(0);
				this.getContext().getContentResolver().notifyChange(
						ContentUris.withAppendedId(Numbers.GROUP_URI, gid),
						null);
			}
			if (c != null && !c.isClosed()) {
				c.close();
			}
			c = null;
			ret = db.delete(Numbers.TABLE, w, selectionArgs);
			break;
		case NUMBERS_GID:
		case NUMBERS_GROUP_ID:
			id = ContentUris.parseId(uri);
			ret = db.delete(Numbers.TABLE, DbUtils.sqlAnd(Numbers.GID + "="
					+ id, selection), selectionArgs);
			ret += db.delete(NumbersGroup.TABLE, NumbersGroup.ID + " = " + id,
					null);
			break;
		case HOURS_ID:
			id = ContentUris.parseId(uri);
			w = DbUtils.sqlAnd(Hours.ID + "=" + id, selection);
			c = db.query(Hours.TABLE, new String[] { Hours.GID }, w,
					selectionArgs, null, null, null);
			if (c != null && c.moveToFirst()) {
				final long gid = c.getLong(0);
				this.getContext().getContentResolver().notifyChange(
						ContentUris.withAppendedId(Hours.GROUP_URI, gid), null);
			}
			if (c != null && !c.isClosed()) {
				c.close();
			}
			c = null;
			ret = db.delete(Hours.TABLE, DbUtils.sqlAnd(Hours.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case HOURS_GID:
		case HOURS_GROUP_ID:
			id = ContentUris.parseId(uri);
			ret = db.delete(Hours.TABLE, DbUtils.sqlAnd(Hours.GID + "=" + id,
					selection), selectionArgs);
			ret += db
					.delete(HoursGroup.TABLE, HoursGroup.ID + " = " + id, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		if (ret > 0) {
			this.getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getType(final Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case LOGS:
			return Logs.CONTENT_TYPE;
		case LOGS_ID:
			return Logs.CONTENT_ITEM_TYPE;
		case PLANS:
			return Plans.CONTENT_TYPE;
		case PLANS_ID:
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
		case EXPORT:
			return EXPORT_MIMETYPE;
		default:
			throw new IllegalArgumentException("Unknown URI: " + uri);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		long ret = -1;
		switch (URI_MATCHER.match(uri)) {
		case LOGS:
			ret = db.insert(Logs.TABLE, null, values);
			break;
		case PLANS:
			if (!values.containsKey(Plans.ORDER)) {
				final Cursor c = db.query(Plans.TABLE,
						new String[] { Plans.ORDER }, null, null, null, null,
						Plans.ORDER + " DESC");
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
				final Cursor c = db.query(Rules.TABLE,
						new String[] { Plans.ORDER }, null, null, null, null,
						Rules.ORDER + " DESC");
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
			return null;
		} else {
			this.getContext().getContentResolver().notifyChange(uri, null);
			return ContentUris.withAppendedId(uri, ret);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreate() {
		this.mOpenHelper = new DatabaseHelper(this.getContext());
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Cursor query(final Uri uri, final String[] projection,
			final String selection, final String[] selectionArgs,
			final String sortOrder) {
		final SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		final int uid = URI_MATCHER.match(uri);
		String groupBy = null;

		switch (uid) {
		case LOGS_ID:
			qb.appendWhere(Logs.ID + "=" + ContentUris.parseId(uri));
		case LOGS:
			qb.setTables(Logs.TABLE);
			qb.setProjectionMap(Logs.PROJECTION_MAP);
			break;
		case LOGS_SUM:
			qb.setTables(Logs.TABLE);
			qb.setProjectionMap(Logs.PROJECTION_MAP);
			groupBy = Logs.PLAN_ID;
			return db.query(Logs.TABLE, projection, selection, selectionArgs,
					groupBy, null, null);
		case PLANS_ID:
			qb.appendWhere(Plans.ID + "=" + ContentUris.parseId(uri));
		case PLANS:
			qb.setTables(Plans.TABLE);
			qb.setProjectionMap(Plans.PROJECTION_MAP);
			break;
		case RULES_ID:
			qb.appendWhere(Rules.ID + "=" + ContentUris.parseId(uri));
		case RULES:
			qb.setTables(Rules.TABLE);
			qb.setProjectionMap(Rules.PROJECTION_MAP);
			break;
		case NUMBERS_GID:
			qb.appendWhere(Numbers.GID + "=" + ContentUris.parseId(uri));
			qb.setTables(Numbers.TABLE);
			qb.setProjectionMap(Numbers.PROJECTION_MAP);
			break;
		case NUMBERS_ID:
			qb.appendWhere(Numbers.ID + "=" + ContentUris.parseId(uri));
		case NUMBERS:
			qb.setTables(Numbers.TABLE);
			qb.setProjectionMap(Numbers.PROJECTION_MAP);
			break;
		case NUMBERS_GROUP_ID:
			qb.appendWhere(NumbersGroup.ID + "=" + ContentUris.parseId(uri));
		case NUMBERS_GROUP:
			qb.setTables(NumbersGroup.TABLE);
			qb.setProjectionMap(NumbersGroup.PROJECTION_MAP);
			break;
		case HOURS_GID:
			qb.appendWhere(Hours.GID + "=" + ContentUris.parseId(uri));
			qb.setTables(Hours.TABLE);
			qb.setProjectionMap(Hours.PROJECTION_MAP);
			break;
		case HOURS_ID:
			qb.appendWhere(Hours.ID + "=" + ContentUris.parseId(uri));
		case HOURS:
			qb.setTables(Hours.TABLE);
			qb.setProjectionMap(Hours.PROJECTION_MAP);
			break;
		case HOURS_GROUP_ID:
			qb.appendWhere(HoursGroup.ID + "=" + ContentUris.parseId(uri));
		case HOURS_GROUP:
			qb.setTables(HoursGroup.TABLE);
			qb.setProjectionMap(HoursGroup.PROJECTION_MAP);
			break;
		case EXPORT:
			Log.d(TAG, "export proj: + " + projection);
			final int l = projection.length;
			Object[] retArray = new Object[l];
			for (int i = 0; i < l; i++) {
				if (projection[i].equals(OpenableColumns.DISPLAY_NAME)) {
					retArray[i] = EXPORT_FILE;
				} else if (projection[i].equals(OpenableColumns.SIZE)) {
					final File d = Environment.getExternalStorageDirectory();
					final File f = new File(d, PACKAGE + File.separator
							+ DataProvider.EXPORT_FILE);
					retArray[i] = f.length();
				}
			}
			final MatrixCursor ret = new MatrixCursor(projection, 1);
			ret.addRow(retArray);
			return ret;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = null;
		} else {
			orderBy = sortOrder;
		}

		// Run the query
		final Cursor c = qb.query(db, projection, selection, selectionArgs,
				groupBy, null, orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(this.getContext().getContentResolver(), uri);
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		long i;
		int ret = 0;
		switch (URI_MATCHER.match(uri)) {
		case LOGS:
			ret = db.update(Logs.TABLE, values, selection, selectionArgs);
			break;
		case LOGS_ID:
			ret = db.update(Logs.TABLE, values, DbUtils.sqlAnd(Logs.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case PLANS:
			ret = db.update(Plans.TABLE, values, selection, selectionArgs);
			break;
		case PLANS_ID:
			ret = db.update(Plans.TABLE, values, DbUtils.sqlAnd(Plans.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case RULES_ID:
			ret = db.update(Rules.TABLE, values, DbUtils.sqlAnd(Rules.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			break;
		case NUMBERS_ID:
			ret = db
					.update(Numbers.TABLE, values, DbUtils.sqlAnd(Numbers.ID
							+ "=" + ContentUris.parseId(uri), selection),
							selectionArgs);
			if (ret > 0 && values != null) {
				i = values.getAsLong(Numbers.GID);
				if (i >= 0) {
					this.getContext().getContentResolver().notifyChange(
							ContentUris.withAppendedId(Numbers.GROUP_URI, i),
							null);
				}
			}
			break;
		case NUMBERS_GROUP_ID:
			ret = db.update(NumbersGroup.TABLE, values, DbUtils
					.sqlAnd(NumbersGroup.ID + "=" + ContentUris.parseId(uri),
							selection), selectionArgs);
			break;
		case HOURS_ID:
			ret = db.update(Hours.TABLE, values, DbUtils.sqlAnd(Hours.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			if (ret > 0 && values != null) {
				i = values.getAsLong(Numbers.GID);
				if (i >= 0) {
					this.getContext().getContentResolver().notifyChange(
							ContentUris.withAppendedId(Hours.GROUP_URI, i),
							null);
				}
			}
			break;
		case HOURS_GROUP_ID:
			ret = db.update(HoursGroup.TABLE, values, DbUtils.sqlAnd(
					HoursGroup.ID + "=" + ContentUris.parseId(uri), selection),
					selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		if (ret > 0) {
			this.getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	public ParcelFileDescriptor openFile(final Uri uri, final String mode)
			throws FileNotFoundException {
		Log.d(TAG, "openFile(" + uri.toString() + ")");
		final File d = Environment.getExternalStorageDirectory();
		final File f = new File(d, PACKAGE + File.separator
				+ DataProvider.EXPORT_FILE);
		return ParcelFileDescriptor
				.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
	}
}
