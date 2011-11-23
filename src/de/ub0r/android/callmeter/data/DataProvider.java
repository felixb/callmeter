/*
 * Copyright (C) 2009-2011 Felix Bechstein, The Android Open Source Project
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
import de.ub0r.android.callmeter.ui.prefs.Preferences;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

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
	private static final int DATABASE_VERSION = 30;

	/** Version of the export file. */
	private static final int EXPORT_VERSION = 0;
	/** Separator of values. */
	private static final String EXPORT_VALUESEPARATOR = ":#:";
	/** Mime type for export. */
	public static final String EXPORT_MIMETYPE = // .
	"application/android.callmeter.export";
	/** {@link Uri} for export Content. */
	public static final Uri EXPORT_RULESET_URI = // .
	Uri.parse("content://" + AUTHORITY + "/export/ruleset");
	/** {@link Uri} for export Content. */
	public static final Uri EXPORT_LOGS_URI = Uri.parse("content://"
			+ AUTHORITY + "/export/logs");
	/** {@link Uri} for export Content. */
	public static final Uri EXPORT_NUMGROUPS_URI = Uri.parse("content://"
			+ AUTHORITY + "/export/numgroups");
	/** {@link Uri} for export Content. */
	public static final Uri EXPORT_HOURGROUPS_URI = Uri.parse("content://"
			+ AUTHORITY + "/export/hourgroups");
	/** Filename for the actual export file. */
	public static final String EXPORT_RULESET_FILE = "ruleset.export";
	/** Filename for the actual logs file. */
	public static final String EXPORT_LOGS_FILE = "logs.export";
	/** Filename for the actual number groups file. */
	public static final String EXPORT_NUMGROUPS_FILE = "numgroups.export";
	/** Filename for the actual hour groups file. */
	public static final String EXPORT_HOURGROUPS_FILE = "hourgroups.export";

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
	/** Bill period: 4 month. */
	public static final int BILLPERIOD_4MONTH = 8;
	/** Bill period: 5 month. */
	public static final int BILLPERIOD_5MONTH = 9;
	/** Bill period: 6 month. */
	public static final int BILLPERIOD_6MONTH = 10;
	/** Bill period: 12 month. */
	public static final int BILLPERIOD_12MONTH = 11;
	/** Bill period: infinite. */
	public static final int BILLPERIOD_INFINITE = 12;

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
		public static final String TABLE = "logs";

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
		/** Index in projection: Cost (free). */
		public static final int INDEX_FREE = 11;

		/** Index in projection - sum: Type of log. */
		public static final int INDEX_SUM_TYPE = 0;
		/** Index in projection - sum: id of plan. */
		public static final int INDEX_SUM_PLAN_ID = 1;
		/** Index in projection - sum: type of plan. */
		public static final int INDEX_SUM_PLAN_TYPE = 2;
		/** Index in projection - sum: Amount. */
		public static final int INDEX_SUM_AMOUNT = 3;
		/** Index in projection - sum: Billed amount. */
		public static final int INDEX_SUM_BILL_AMOUNT = 4;
		/** Index in projection - sum: Cost. */
		public static final int INDEX_SUM_COST = 5;
		/** Index in projection - sum: Cost (free). */
		public static final int INDEX_SUM_FREE = 6;
		/** Index in projection - sum: count. */
		public static final int INDEX_SUM_COUNT = 7;

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
		/** Cost (free). */
		public static final String FREE = "_logs_cost_free";
		/** Type of plan. Only available in sum query. */
		public static final String PLAN_TYPE = "_plan_type";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, PLAN_ID,
				RULE_ID, TYPE, DIRECTION, DATE, AMOUNT, BILL_AMOUNT, REMOTE,
				ROAMED, COST, FREE };
		/** Projection used for join query. */
		public static final String[] PROJECTION_JOIN;
		static {
			final int l = PROJECTION.length;
			PROJECTION_JOIN = new String[l + 2];
			for (int i = 0; i < l; i++) {
				PROJECTION_JOIN[i] = TABLE + "." + PROJECTION[i] + " as "
						+ PROJECTION[i];
			}
			PROJECTION_JOIN[l - 2] = Plans.TABLE + "." + Plans.NAME + " as "
					+ Plans.NAME;
			PROJECTION_JOIN[l - 1] = Rules.TABLE + "." + Rules.NAME + " as "
					+ Rules.NAME;
		}

		/** Projection used for query - sum. */
		public static final String[] PROJECTION_SUM = new String[] { TYPE,
				PLAN_ID, Plans.TABLE + "." + Plans.TYPE + " AS " + PLAN_TYPE,
				"sum(" + AMOUNT + ")", "sum(" + BILL_AMOUNT + ")",
				"sum(" + COST + ")", "sum(" + FREE + ")",
				"count(" + PLAN_ID + ")" };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/logs");
		/** Content {@link Uri} logs joined with plans and rules. */
		public static final Uri CONTENT_URI_JOIN = Uri.parse("content://"
				+ AUTHORITY + "/logs/join");
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
					+ PLAN_ID + " LONG, " // .
					+ RULE_ID + " LONG, " // .
					+ TYPE + " INTEGER, " // .
					+ DIRECTION + " INTEGER, " // .
					+ DATE + " LONG, " // .
					+ AMOUNT + " LONG, " // .
					+ BILL_AMOUNT + " FLOAT, " // .
					+ REMOTE + " TEXT, "// .
					+ ROAMED + " INTEGER, " // .
					+ COST + " FLOAT, "// .
					+ FREE + " FLOAT"// .
					+ ");");
			db.execSQL("CREATE INDEX " + TABLE + "_idx on " + TABLE + " (" // .
					+ ID + "," + PLAN_ID + "," + DATE + ")");
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
					AMOUNT, DATE, DIRECTION, REMOTE, ROAMED, TYPE, PLAN_ID },
					null);
			onCreate(db);
			reload(db, TABLE, values);
		}

		/** Default constructor. */
		private Logs() {
			// nothing here.
		}

		/**
		 * Clean a number from all but [?*#+0-9].
		 * 
		 * @param number
		 *            dirty number
		 * @param pattern
		 *            if true, '%' is added to list of allowed chars
		 * @return cleaned number
		 */
		public static String cleanNumber(final String number,
				final boolean pattern) {
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
		/** Table name. */
		private static final String TABLE = "websms";

		/** Index in projection: ID. */
		public static final int INDEX_ID = 0;
		/** Index in projection: Connector's name. */
		public static final int INDEX_CONNECTOR = 1;
		/** Index in projection: date. */
		public static final int INDEX_DATE = 2;

		/** ID. */
		public static final String ID = "_id";
		/** Connector's name. */
		public static final String CONNECTOR = "_connector";
		/** Date. */
		public static final String DATE = "_date";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { // .
		ID, CONNECTOR, DATE };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/websms");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.websms";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.websms";

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
					+ ID + " LONG, " // .
					+ CONNECTOR + " TEXT,"// .
					+ DATE + " LONG"// .
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
		/** Table name. */
		private static final String TABLE = "sipcall";

		/** Index in projection: ID. */
		public static final int INDEX_ID = 0;
		/** Index in projection: Provider's name. */
		public static final int INDEX_PROVIDER = 1;
		/** Index in projection: date. */
		public static final int INDEX_DATE = 2;

		/** ID. */
		public static final String ID = "_id";
		/** Provider's name. */
		public static final String PROVIDER = "_connector";
		/** Date. */
		public static final String DATE = "_date";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { // .
		ID, PROVIDER, DATE };

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/sipcall");
		/**
		 * The MIME type of {@link #CONTENT_URI} providing a list.
		 */
		public static final String CONTENT_TYPE = // .
		"vnd.android.cursor.dir/vnd.ub0r.sipcall";

		/**
		 * The MIME type of a {@link #CONTENT_URI} single entry.
		 */
		public static final String CONTENT_ITEM_TYPE = // .
		"vnd.android.cursor.item/vnd.ub0r.sipcall";

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
					+ ID + " LONG, " // .
					+ PROVIDER + " TEXT,"// .
					+ DATE + " LONG"// .
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

		/** Hide constructor. */
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
		/** A plan. */
		public static final class Plan {
			/** Id of plan. */
			public final long id;
			/** Type of plan. */
			public final int type;
			/** Plans's name. */
			public final String name;
			/** Plans's short name. */
			public final String sname;
			/** Bill period. */
			public final int billperiod;
			/** Bill day. */
			public final long billday;
			/** Next bill day. */
			public final long nextbillday;
			/** Current time used for query. */
			public final long now;
			/** Type of limit. */
			public final int limittype;
			/** Limit. */
			public final long limit;
			/** Position in limit. */
			public final long limitPos;
			/** Used limit. */
			public final float usage;
			/** Cost per plan. */
			private final float cpp;
			/** Sum of cost. */
			public final float cost;
			/** Sum of free cost. */
			public final float free;
			/** Sum of todays count. */
			public final int tdCount;
			/** Sum of todays billed amount. */
			public final float tdBa;
			/** Sum of this bill period's count. */
			public final int bpCount;
			/** Sum of this bill period's billed amount. */
			public final float bpBa;
			/** Sum of this all time's count. */
			public final int atCount;
			/** Sum of this all time's billed amount. */
			public final float atBa;

			/**
			 * Default Constructor from {@link Cursor}. Use this method only
			 * with {@link Cursor}s against {@link Plan}.CONTENT_URI_SUM with
			 * PROJECTION_SUM.
			 * 
			 * @param cursor
			 *            {@link Cursor}
			 */
			public Plan(final Cursor cursor) {
				this.id = cursor.getLong(INDEX_ID);
				this.type = cursor.getInt(INDEX_TYPE);
				this.name = cursor.getString(INDEX_NAME);
				this.sname = cursor.getString(INDEX_SHORTNAME);
				this.billperiod = cursor.getInt(INDEX_BILLPERIOD);
				if (this.type == TYPE_SPACING || this.type == TYPE_TITLE) {
					this.billday = -1;
					this.nextbillday = -1;
					this.limittype = -1;
					this.limit = -1;
					this.limitPos = -1;
					this.cpp = 0;
					this.cost = 0f;
					this.free = 0f;
					this.tdCount = 0;
					this.tdBa = 0f;
					this.bpCount = 0;
					this.bpBa = 0f;
					this.atCount = 0;
					this.atBa = 0f;
					this.now = -1L;
				} else {
					this.cost = cursor.getFloat(INDEX_SUM_COST);
					this.free = cursor.getFloat(INDEX_SUM_FREE);
					this.tdCount = cursor.getInt(INDEX_SUM_TD_COUNT);
					this.tdBa = cursor.getFloat(INDEX_SUM_TD_BILLED_AMOUNT);
					this.bpCount = cursor.getInt(INDEX_SUM_BP_COUNT);
					this.bpBa = cursor.getFloat(INDEX_SUM_BP_BILLED_AMOUNT);
					this.atCount = cursor.getInt(INDEX_SUM_AT_COUNT);
					this.atBa = cursor.getFloat(INDEX_SUM_AT_BILLED_AMOUNT);
					this.now = cursor.getLong(INDEX_SUM_NOW);
					this.billday = cursor.getLong(INDEX_SUM_BILLDAY);
					this.nextbillday = cursor.getLong(INDEX_SUM_NEXTBILLDAY);
					this.cpp = cursor.getFloat(INDEX_SUM_CPP);
					if (this.type == TYPE_BILLPERIOD) {
						this.limittype = -1;
						if (billperiod == DataProvider.BILLPERIOD_INFINITE) {
							this.limitPos = 0;
							this.limit = 0;
						} else {
							this.limitPos = (this.now - this.billday)
									/ Utils.MINUTES_IN_MILLIS;
							this.limit = (this.nextbillday - this.billday)
									/ Utils.MINUTES_IN_MILLIS;
						}
					} else {
						this.limittype = cursor.getInt(INDEX_LIMIT_TYPE);
						this.limit = getLimit(this.type, this.limittype,
								cursor.getLong(INDEX_LIMIT));
						this.limitPos = getUsed(this.type, this.limittype,
								this.bpBa, this.cost);
					}
				}
				if (this.limitPos <= 0) {
					this.usage = 0;
				} else {
					this.usage = (float) this.limitPos / (float) this.limit;
				}
			}

			/**
			 * Get a {@link Plan} from plan's id.
			 * 
			 * @param cr
			 *            {@link ContentResolver}
			 * @param planid
			 *            {@link Plan}'s id
			 * @param now
			 *            time of query
			 * @return {@link Plan}
			 */
			public static Plan getPlan(final ContentResolver cr,
					final long planid, final long now) {
				Uri uri = CONTENT_URI_SUM;
				if (now >= 0) {
					uri = uri
							.buildUpon()
							.appendQueryParameter(PARAM_DATE,
									String.valueOf(now)).build();
				}
				Cursor c = cr.query(uri, PROJECTION_SUM, TABLE + "." + ID
						+ "=?", new String[] { String.valueOf(planid) }, null);
				Plan ret = null;
				if (c.moveToFirst()) {
					ret = new Plan(c);
				}
				c.close();
				return ret;
			}

			/**
			 * Get a {@link Plan} from plan's id.
			 * 
			 * @param cr
			 *            {@link ContentResolver}
			 * @param planid
			 *            {@link Plan}'s id
			 * @param now
			 *            time of query
			 * @return {@link Plan}
			 */
			public static Plan getPlan(final ContentResolver cr,
					final long planid, final Calendar now) {
				if (now == null) {
					return getPlan(cr, planid, -1L);
				} else {
					return getPlan(cr, planid, now.getTimeInMillis());
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			public String toString() {
				return "lan:" + this.id + ":" + this.name;
			}

			/**
			 * Get usage of {@link Plan}'s bill period.
			 * 
			 * @return usage of bill period
			 */
			public float getBillPlanUsage() {
				long blength = this.nextbillday - this.billday;
				if (blength <= 0L) {
					return -1f;
				}
				return ((float) (this.now - this.billday)) / (float) blength;
			}

			/**
			 * Get cost and cost per plan.
			 * 
			 * @return cost + cpp - free
			 */
			public float getAccumCost() {
				return this.cost + this.cpp - this.free;
			}

			/**
			 * Get remaining balance.
			 * 
			 * @return balance - cost for bill periods; else cost + cpp
			 */
			public float getAccumCostPrepaid() {
				if (this.type == TYPE_BILLPERIOD) {
					return this.cpp - this.cost;
				} else {
					return this.cost + this.cpp;
				}
			}

			/**
			 * Get free cost.
			 * 
			 * @return free cost
			 */
			public float getFree() {
				return this.free;
			}
		}

		/** Table name. */
		private static final String TABLE = "plans";

		/** Parameter for query: date. */
		public static final String PARAM_DATE = "date";
		/** Parameter for query: hide zero plans. */
		public static final String PARAM_HIDE_ZERO = "hide_zero";
		/** Parameter for query: hide zero cost plans. */
		public static final String PARAM_HIDE_NOCOST = "hide_nocost";

		/** Index in projection: id. */
		public static final int INDEX_ID = 0;
		/** Index in projection: name. */
		public static final int INDEX_NAME = 1;
		/** Index in projection: short name. */
		public static final int INDEX_SHORTNAME = 2;
		/** Index in projection: order. */
		public static final int INDEX_ORDER = 3;
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
		/** Index in projection: id of billperiod. */
		public static final int INDEX_BILLPERIOD_ID = 20;
		/** Index in projection: next alert. */
		public static final int INDEX_NEXT_ALERT = 21;
		/** Index in projection: strip first seconds. */
		public static final int INDEX_STRIP_SECONDS = 22;
		/** Index in projection: merged plans. */
		public static final int INDEX_MERGED_PLANS = 23;
		/** Index in projection: sum, now. */
		public static final int INDEX_SUM_NOW = 24;
		/** Index in projection: sum, last bill day. */
		public static final int INDEX_SUM_BILLDAY = 25;
		/** Index in projection: sum: next bill day. */
		public static final int INDEX_SUM_NEXTBILLDAY = 26;
		/** Index in projection: sum, TODAY. */
		public static final int INDEX_SUM_TODAY = 27;
		/** Index in projection: sum count for this bill period. */
		public static final int INDEX_SUM_BP_COUNT = 28;
		/** Index in projection: sum billed amount for this bill period. */
		public static final int INDEX_SUM_BP_BILLED_AMOUNT = 29;
		/** Index in projection: sum count. */
		public static final int INDEX_SUM_AT_COUNT = 30;
		/** Index in projection: sum billed amount. */
		public static final int INDEX_SUM_AT_BILLED_AMOUNT = 31;
		/** Index in projection: sum count for today. */
		public static final int INDEX_SUM_TD_COUNT = 32;
		/** Index in projection: sum billed amount for today. */
		public static final int INDEX_SUM_TD_BILLED_AMOUNT = 33;
		/** Index in projection: sum cost for all plans. */
		public static final int INDEX_SUM_CPP = 34;
		/** Index in projection: sum cost for this bill period. */
		public static final int INDEX_SUM_COST = 35;
		/** Index in projection: sum free cost for this bill period. */
		public static final int INDEX_SUM_FREE = 36;

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
		/** Next alert. */
		public static final String NEXT_ALERT = "_next_alert";
		/** Strip first seconds. */
		public static final String STRIP_SECONDS = "_strip_seconds";
		/** Merged plans. */
		public static final String MERGED_PLANS = "_merged_plans";

		/** Sum: now. */
		public static final String SUM_NOW = "NOW";
		/** Sum: last bill day. */
		public static final String SUM_BILLDAY = "BILLDAY";
		/** Sum: next bill day. */
		public static final String SUM_NEXTBILLDAY = "NEXTBILLDAY";
		/** Sum: TODAY. */
		public static final String SUM_TODAY = "TODAY";
		/** Sum: count for this bill period. */
		public static final String SUM_BP_COUNT = "SUM_BP_COUNT";
		/** Sum: billed amount for this bill period. */
		public static final String SUM_BP_BILLED_AMOUNT = "SUM_BP_BA";
		/** Sum: count. */
		public static final String SUM_AT_COUNT = "SUM_AT_COUNT";
		/** Sum: billed amount. */
		public static final String SUM_AT_BILLED_AMOUNT = "SUM_AT_BA";
		/** Sum: count for today. */
		public static final String SUM_TD_COUNT = "SUM_TD_COUNT";
		/** Sum: billed amount for today. */
		public static final String SUM_TD_BILLED_AMOUNT = "SUM_TD_BA";
		/** Sum: cost for all plans. */
		public static final String SUM_CPP = "SUM_CPP";
		/** Sum: cost for this bill period. */
		public static final String SUM_COST = "SUM_COST";
		/** Sum: free cost for this bill period. */
		public static final String SUM_FREE = "SUM_FREE";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, NAME,
				SHORTNAME, ORDER, TYPE, LIMIT_TYPE, LIMIT, BILLMODE, BILLDAY,
				BILLPERIOD, COST_PER_ITEM, COST_PER_AMOUNT1, COST_PER_AMOUNT2,
				COST_PER_ITEM_IN_LIMIT, COST_PER_AMOUNT_IN_LIMIT1,
				COST_PER_AMOUNT_IN_LIMIT2, COST_PER_PLAN, MIXED_UNITS_CALL,
				MIXED_UNITS_SMS, MIXED_UNITS_MMS, BILLPERIOD_ID, NEXT_ALERT,
				STRIP_SECONDS, MERGED_PLANS };

		/** Projection used for sum query. */
		public static final String[] PROJECTION_SUM = // .
		new String[INDEX_SUM_FREE + 1];
		static {
			final int l = PROJECTION.length;
			for (int i = 0; i < l; i++) {
				PROJECTION_SUM[i] = TABLE + "." + PROJECTION[i] + " AS "
						+ PROJECTION[i];
			}
			PROJECTION_SUM[INDEX_SUM_NOW] = "{" + SUM_NOW + "} AS " + SUM_NOW;
			PROJECTION_SUM[INDEX_SUM_TODAY] = "{" + SUM_TODAY + "} AS "
					+ SUM_TODAY;
			PROJECTION_SUM[INDEX_SUM_BILLDAY] = "{" + SUM_BILLDAY + "} AS "
					+ SUM_BILLDAY;
			PROJECTION_SUM[INDEX_SUM_NEXTBILLDAY] = "{" + SUM_NEXTBILLDAY
					+ "} AS " + SUM_NEXTBILLDAY;

			PROJECTION_SUM[INDEX_SUM_TD_COUNT] = "sum(CASE WHEN " + Logs.TABLE
					+ "." + Logs.DATE + " is null or " + Logs.TABLE + "."
					+ Logs.DATE + "<{" + SUM_TODAY + "} or " + Logs.TABLE + "."
					+ Logs.DATE + ">{" + SUM_NOW + "} THEN 0 ELSE 1 END) as "
					+ SUM_TD_COUNT;
			PROJECTION_SUM[INDEX_SUM_TD_BILLED_AMOUNT] = "sum(CASE WHEN "
					+ Logs.TABLE + "." + Logs.DATE + "<{" + SUM_TODAY + "} or "
					+ Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW
					+ "} THEN 0 WHEN " + TABLE + "." + MERGED_PLANS
					+ " is null or " + TABLE + "." + TYPE + "!=" + TYPE_MIXED
					+ " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT
					+ " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "="
					+ TYPE_CALL + " THEN " + Logs.TABLE + "."
					+ Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_CALL
					+ "/60" + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "="
					+ TYPE_SMS + " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT
					+ "*" + TABLE + "." + MIXED_UNITS_SMS + " WHEN  "
					+ Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_MMS + " THEN "
					+ Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "."
					+ MIXED_UNITS_MMS + " ELSE " + Logs.TABLE + "."
					+ Logs.BILL_AMOUNT + " END) AS " + SUM_TD_BILLED_AMOUNT;

			PROJECTION_SUM[INDEX_SUM_BP_COUNT] = "sum(CASE WHEN " + Logs.TABLE
					+ "." + Logs.DATE + " is null or " + Logs.TABLE + "."
					+ Logs.DATE + "<={" + SUM_BILLDAY + "} or " + Logs.TABLE
					+ "." + Logs.DATE + ">{" + SUM_NOW
					+ "} THEN 0 ELSE 1 END) as " + SUM_BP_COUNT;
			PROJECTION_SUM[INDEX_SUM_BP_BILLED_AMOUNT] = "sum(CASE WHEN "
					+ Logs.TABLE + "." + Logs.DATE + "<={" + SUM_BILLDAY
					+ "} or " + Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW
					+ "} THEN 0 WHEN " + TABLE + "." + MERGED_PLANS
					+ " is null or " + TABLE + "." + TYPE + "!=" + TYPE_MIXED
					+ " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT
					+ " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "="
					+ TYPE_CALL + " THEN " + Logs.TABLE + "."
					+ Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_CALL
					+ "/60" + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "="
					+ TYPE_SMS + " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT
					+ "*" + TABLE + "." + MIXED_UNITS_SMS + " WHEN  "
					+ Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_MMS + " THEN "
					+ Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "."
					+ MIXED_UNITS_MMS + " ELSE " + Logs.TABLE + "."
					+ Logs.BILL_AMOUNT + " END) AS " + SUM_BP_BILLED_AMOUNT;

			PROJECTION_SUM[INDEX_SUM_AT_COUNT] = "sum(CASE WHEN " + Logs.TABLE
					+ "." + Logs.DATE + " is null or " + Logs.TABLE + "."
					+ Logs.DATE + ">{" + SUM_NOW + "} THEN 0 ELSE 1 END) as "
					+ SUM_AT_COUNT;
			PROJECTION_SUM[INDEX_SUM_AT_BILLED_AMOUNT] = "sum(CASE WHEN "
					+ Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW
					+ "} THEN 0 WHEN " + TABLE + "." + MERGED_PLANS
					+ " is null or " + TABLE + "." + TYPE + "!=" + TYPE_MIXED
					+ " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT
					+ " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "="
					+ TYPE_CALL + " THEN " + Logs.TABLE + "."
					+ Logs.BILL_AMOUNT + "*" + TABLE + "." + MIXED_UNITS_CALL
					+ "/60" + " WHEN  " + Logs.TABLE + "." + Logs.TYPE + "="
					+ TYPE_SMS + " THEN " + Logs.TABLE + "." + Logs.BILL_AMOUNT
					+ "*" + TABLE + "." + MIXED_UNITS_SMS + " WHEN  "
					+ Logs.TABLE + "." + Logs.TYPE + "=" + TYPE_MMS + " THEN "
					+ Logs.TABLE + "." + Logs.BILL_AMOUNT + "*" + TABLE + "."
					+ MIXED_UNITS_MMS + " ELSE " + Logs.TABLE + "."
					+ Logs.BILL_AMOUNT + " END) AS " + SUM_AT_BILLED_AMOUNT;

			PROJECTION_SUM[INDEX_SUM_CPP] = "(CASE WHEN " + TABLE + "." + TYPE
					+ "=" + TYPE_BILLPERIOD + " THEN " + TABLE + "."
					+ COST_PER_PLAN + " + (select sum(p." + COST_PER_PLAN
					+ ") from " + TABLE + " as p where p." + BILLPERIOD_ID
					+ "=" + TABLE + "." + ID + ") ELSE " + TABLE + "."
					+ COST_PER_PLAN + " END) as " + SUM_CPP;
			PROJECTION_SUM[INDEX_SUM_COST] = "sum(CASE WHEN " + Logs.TABLE
					+ "." + Logs.DATE + "<{" + SUM_BILLDAY + "} or "
					+ Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW
					+ "} THEN 0 ELSE " + Logs.TABLE + "." + Logs.COST
					+ " END) as " + SUM_COST;
			PROJECTION_SUM[INDEX_SUM_FREE] = "sum(CASE WHEN " + Logs.TABLE
					+ "." + Logs.DATE + "<{" + SUM_BILLDAY + "} or "
					+ Logs.TABLE + "." + Logs.DATE + ">{" + SUM_NOW
					+ "} THEN 0 ELSE " + Logs.TABLE + "." + Logs.FREE
					+ " END) as " + SUM_FREE;
		}

		/** Projection used for query id and (short)name. */
		public static final String[] PROJECTION_NAME = new String[] { ID, NAME,
				SHORTNAME };

		/** Select only real plans. */
		public static final String WHERE_REALPLANS = TYPE + "!="
				+ TYPE_BILLPERIOD + " and " + TYPE + "!=" + TYPE_SPACING
				+ " and " + TYPE + "!=" + TYPE_TITLE;
		/** Select only bill periods. */
		public static final String WHERE_BILLPERIODS = TYPE + " = "
				+ TYPE_BILLPERIOD;

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/plans");
		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI_SUM = Uri.parse("content://"
				+ AUTHORITY + "/plans/sum");
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
					+ NEXT_ALERT + " LONG," // .
					+ STRIP_SECONDS + " INTEGER," // .
					+ MERGED_PLANS + " TEXT" // .
					+ ");");
			db.execSQL("CREATE INDEX " + TABLE + "_idx on " + TABLE + " (" // .
					+ ID + "," + ORDER + "," + TYPE + ")");
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
			final Cursor cursor = cr.query(
					ContentUris.withAppendedId(CONTENT_URI, id),
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
		 * @param cr
		 *            {@link ContentResolver}
		 * @param id
		 *            id
		 * @return parent's id
		 */
		public static long getParent(final ContentResolver cr, final long id) {
			if (id < 0) {
				return -1L;
			}
			final Cursor cursor = cr.query(CONTENT_URI, PROJECTION_NAME,
					DataProvider.Plans.MERGED_PLANS + " LIKE '%," + id + ",%'",
					null, null);
			long ret = -1L;
			if (cursor.moveToFirst()) {
				ret = cursor.getLong(0);
			}
			if (!cursor.isClosed()) {
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
				final float amount, final float cost) {
			switch (lType) {
			case DataProvider.LIMIT_TYPE_COST:
				return (int) (cost * CallMeter.HUNDRET);
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
			if (period == BILLPERIOD_INFINITE) {
				return DataProvider.Logs.DATE + " > " + bd.getTimeInMillis();
			}
			String next = "";
			if (now != null) {
				next = " AND " + DataProvider.Logs.DATE + " < "
						+ now.getTimeInMillis();
			}
			// final Calendar nbd = getBillDay(period, start, now, true);
			return DataProvider.Logs.DATE + " > " + bd.getTimeInMillis() + next;
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
			case BILLPERIOD_WEEK:
				f = Calendar.DAY_OF_MONTH;
				v = 7;
				break;
			default:
				return null;
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
				ret.add(f, v * -1);
			}
			while (!ret.after(n)) {
				ret.add(f, v);
			}
			if (!next) {
				ret.add(f, v * -1);
			}
			return ret;
		}

		/**
		 * Parse the MERGED_PLANS filed to a WHERE clause matching all merged
		 * plans.
		 * 
		 * @param pid
		 *            the plan itself
		 * @param merged
		 *            merge plans
		 * @return WHERE clause
		 */
		public static String parseMergerWhere(final long pid,
				final String merged) {
			final String self = DataProvider.Logs.PLAN_ID + " = " + pid;
			if (merged == null) {
				return self;
			}
			final StringBuilder sb = new StringBuilder(self);

			for (String ss : merged.split(",")) {
				if (ss.length() == 0) {
					continue;
				}
				sb.append(" OR " + DataProvider.Logs.PLAN_ID + " = " + ss);
			}
			return sb.toString();
		}
	}

	/**
	 * Rules.
	 * 
	 * @author flx
	 */
	public static final class Rules {
		/** Condition does not matter. */
		public static final int NO_MATTER = 2;

		/** Condition type: match call. */
		public static final int WHAT_CALL = 0;
		/** Condition type: match sms. */
		public static final int WHAT_SMS = 1;
		/** Condition type: match mms. */
		public static final int WHAT_MMS = 2;
		/** Condition type: match data. */
		public static final int WHAT_DATA = 3;

		/** Table name. */
		private static final String TABLE = "rules";

		/** Index in projection: id. */
		public static final int INDEX_ID = 0;
		/** Index in projection: active? */
		public static final int INDEX_ACTIVE = 1;
		/** Index in projection: order. */
		public static final int INDEX_ORDER = 2;
		/** Index in projection: ID of plan referred by this rule. */
		public static final int INDEX_PLAN_ID = 3;
		/** Index in projection: Name. */
		public static final int INDEX_NAME = 4;
		/** Index in projection: Kind of rule. */
		public static final int INDEX_WHAT = 5;
		/** Index in projection: is roamed? */
		public static final int INDEX_ROAMED = 6;
		/** Index in projection: is direction? */
		public static final int INDEX_DIRECTION = 7;
		/** Index in projection: is hours? */
		public static final int INDEX_INHOURS_ID = 8;
		/** Index in projection: is not hours? */
		public static final int INDEX_EXHOURS_ID = 9;
		/** Index in projection: is number? */
		public static final int INDEX_INNUMBERS_ID = 10;
		/** Index in projection: is not number? */
		public static final int INDEX_EXNUMBERS_ID = 11;
		/** Index in projection: limit not reached? */
		public static final int INDEX_LIMIT_NOT_REACHED = 12;
		/** Index in projection: is websms. */
		public static final int INDEX_IS_WEBSMS = 13;
		/** Index in projection: is websms connector. */
		public static final int INDEX_IS_WEBSMS_CONNETOR = 14;
		/** Index in projection: is sipcall. */
		public static final int INDEX_IS_SIPCALL = 15;
		/** Index in projection: is sipcall provider. */
		// public static final int INDEX_IS_SIPCALL_PROVIDER = 16;

		/** ID. */
		public static final String ID = "_id";
		/** Active? */
		public static final String ACTIVE = "_active";
		/** Order. */
		public static final String ORDER = "_order";
		/** ID of plan referred by this rule. */
		public static final String PLAN_ID = "_plan_id";
		/** Name. */
		public static final String NAME = "_rule_name";
		/** Kind of rule. */
		public static final String WHAT = "_what";
		/** Is roamed? */
		public static final String ROAMED = "_roamed";
		/** Is direction? */
		public static final String DIRECTION = "_direction";
		/** Is hours? */
		public static final String INHOURS_ID = "_inhourgroup_id";
		/** Is not hours? */
		public static final String EXHOURS_ID = "_exhourgroup_id";
		/** Is number? */
		public static final String INNUMBERS_ID = "_innumbergroup_id";
		/** Is not number? */
		public static final String EXNUMBERS_ID = "_exnumbergroup_id";
		/** Limit not reached? */
		public static final String LIMIT_NOT_REACHED = "_limit_not_reached";
		/** Is websms. */
		public static final String IS_WEBSMS = "_is_websms";
		/** Is websms connector. */
		public static final String IS_WEBSMS_CONNETOR = "_is_websms_connector";
		/** Is sipcall. */
		public static final String IS_SIPCALL = "_is_sipcall";
		/** Is sipcall provider. */
		public static final String IS_SIPCALL_PROVIDER = "_is_sipcall_provider";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, ACTIVE,
				ORDER, PLAN_ID, NAME, WHAT, ROAMED, DIRECTION, INHOURS_ID,
				EXHOURS_ID, INNUMBERS_ID, EXNUMBERS_ID, LIMIT_NOT_REACHED,
				IS_WEBSMS, IS_WEBSMS_CONNETOR, IS_SIPCALL, // .
				IS_SIPCALL_PROVIDER };

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
					+ ACTIVE + " INTEGER DEFAULT 1," // .
					+ ORDER + " INTEGER," // .
					+ NAME + " TEXT,"// .
					+ PLAN_ID + " INTEGER,"// .
					+ WHAT + " INTEGER,"// .
					+ ROAMED + " INTEGER,"// .
					+ DIRECTION + " INTEGER,"// .
					+ INHOURS_ID + " TEXT,"// .
					+ EXHOURS_ID + " TEXT,"// .
					+ INNUMBERS_ID + " TEXT,"// .
					+ EXNUMBERS_ID + " TEXT,"// .
					+ LIMIT_NOT_REACHED + " INTEGER," // .
					+ IS_WEBSMS + " INTEGER," // .
					+ IS_WEBSMS_CONNETOR + " TEXT," // .
					+ IS_SIPCALL + " INTEGER," // .
					+ IS_SIPCALL_PROVIDER + " TEXT" // .
					+ ");");
			db.execSQL("CREATE INDEX " + TABLE + "_idx on " + TABLE
					+ " (" // .
					+ ID + "," + ACTIVE + "," + ORDER + "," + PLAN_ID + ","
					+ WHAT + ")");
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
			final Cursor cursor = cr.query(
					ContentUris.withAppendedId(CONTENT_URI, id),
					new String[] { NAME }, null, null, null);
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
			final Cursor cursor = cr.query(
					ContentUris.withAppendedId(CONTENT_URI, id),
					new String[] { NAME }, null, null, null);
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
			final Cursor cursor = cr.query(
					ContentUris.withAppendedId(CONTENT_URI, id),
					new String[] { NAME }, null, null, null);
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
	/** Internal id: logs joined with rules and plans. */
	private static final int LOGS_JOIN = 18;
	/** Internal id: websms. */
	private static final int WEBSMS = 19;
	/** Internal id: sipcall. */
	private static final int SIPCALL = 20;
	/** Internal id: plans outer joined with its logs. */
	private static final int PLANS_SUM = 21;
	/** Internal id: single plan outer joined with its logs. */
	private static final int PLANS_SUM_ID = 22;
	/** Internal id: export. */
	private static final int EXPORT_RULESET = 200;
	/** Internal id: export. */
	private static final int EXPORT_LOGS = 201;
	/** Internal id: export. */
	private static final int EXPORT_NUMGROUPS = 202;
	/** Internal id: export. */
	private static final int EXPORT_HOURGROUPS = 203;

	/** {@link UriMatcher}. */
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
		URI_MATCHER.addURI(AUTHORITY, "export/ruleset", EXPORT_RULESET);
		URI_MATCHER.addURI(AUTHORITY, "export/logs", EXPORT_LOGS);
		URI_MATCHER.addURI(AUTHORITY, "export/numgroups", EXPORT_NUMGROUPS);
		URI_MATCHER.addURI(AUTHORITY, "export/hourgroups", EXPORT_HOURGROUPS);
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		/**
		 * {@inheritDoc}
		 */
		@Override
		public synchronized SQLiteDatabase getReadableDatabase() {
			Log.d(TAG, "get readble db");
			SQLiteDatabase ret;
			try {
				ret = super.getReadableDatabase();
			} catch (IllegalStateException e) {
				Log.e(TAG, "could not open databse, try again", e);
				ret = super.getReadableDatabase();
			}
			if (!ret.isOpen()) { // a restore closes the db. retry.
				Log.w(TAG, "got closed database, try again");
				ret = super.getReadableDatabase();
			}
			return ret;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public synchronized SQLiteDatabase getWritableDatabase() {
			Log.d(TAG, "get writable db");
			SQLiteDatabase ret;
			try {
				ret = super.getWritableDatabase();
			} catch (IllegalStateException e) {
				Log.e(TAG, "could not open databse, try again", e);
				ret = super.getWritableDatabase();
			}
			if (!ret.isOpen()) { // a restore closes the db. retry.
				Log.w(TAG, "got closed database, try again");
				ret = super.getWritableDatabase();
			}
			return ret;
		}

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
			if (doRestore(db)) {
				return; // skip create
			}
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
			importDefault(this.ctx, db);
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
			WebSMS.onUpgrade(db, oldVersion, newVersion);
			SipCall.onUpgrade(db, oldVersion, newVersion);
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
	 * Backup logs to String.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param descr
	 *            description of the logs
	 * @return {@link String} representing {@link Logs}
	 */
	public static String backupLogs(final Context context, // .
			final String descr) {
		StringBuilder sb = new StringBuilder();
		sb.append(EXPORT_VERSION + "\n");
		sb.append(URLEncoder.encode(descr) + "\n");
		final SQLiteDatabase db = new DatabaseHelper(context)
				.getReadableDatabase();

		backupRuleSetSub(sb, db, Logs.TABLE, Logs.PROJECTION, null);
		backupRuleSetSub(sb, db, WebSMS.TABLE, WebSMS.PROJECTION, null);
		backupRuleSetSub(sb, db, SipCall.TABLE, SipCall.PROJECTION, null);
		db.close();
		return sb.toString();
	}

	/**
	 * Backup number groups to String.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param descr
	 *            description of the number groups
	 * @return {@link String} representing {@link NumbersGroup} and
	 *         {@link Numbers}
	 */
	public static String backupNumGroups(final Context context, // .
			final String descr) {
		StringBuilder sb = new StringBuilder();
		sb.append(EXPORT_VERSION + "\n");
		sb.append(URLEncoder.encode(descr) + "\n");
		final SQLiteDatabase db = new DatabaseHelper(context)
				.getReadableDatabase();

		backupRuleSetSub(sb, db, NumbersGroup.TABLE, NumbersGroup.PROJECTION,
				null);
		backupRuleSetSub(sb, db, Numbers.TABLE, Numbers.PROJECTION, null);
		db.close();
		return sb.toString();
	}

	/**
	 * Backup hour groups to String.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param descr
	 *            description of the hour groups
	 * @return {@link String} representing {@link HoursGroup} and {@link Hours}
	 */
	public static String backupHourGroups(final Context context, // .
			final String descr) {
		StringBuilder sb = new StringBuilder();
		sb.append(EXPORT_VERSION + "\n");
		sb.append(URLEncoder.encode(descr) + "\n");
		final SQLiteDatabase db = new DatabaseHelper(context)
				.getReadableDatabase();

		backupRuleSetSub(sb, db, HoursGroup.TABLE, HoursGroup.PROJECTION, null);
		backupRuleSetSub(sb, db, Hours.TABLE, Hours.PROJECTION, null);
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
	 * Import data from {@link String}.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param ruleSet
	 *            data as {@link String}; "DEFAULT" will import default rule set
	 */
	public static void importData(final Context context, // .
			final String ruleSet) {
		if (ruleSet == null || ruleSet.length() == 0) {
			return;
		}
		String[] lines = null;
		if (!ruleSet.equals("DEFAULT")) {
			lines = ruleSet.split("\n");
			if (lines.length <= 2) {
				return;
			}
		}
		final SQLiteDatabase db = new DatabaseHelper(context)
				.getWritableDatabase();
		if (lines != null) {
			importData(db, lines);
			Preferences.setDefaultPlan(context, false);
		} else {
			importDefault(context, db);
		}
		db.close();
	}

	/**
	 * Import default rule set.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param db
	 *            {@link SQLiteDatabase}
	 */
	public static void importDefault(final Context context,
			final SQLiteDatabase db) {
		// import default
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				context.getResources().openRawResource(R.raw.default_setup)));
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

		// translate default rule set:
		ContentValues cv = new ContentValues();
		// bill period: 12
		cv.put(Plans.NAME, context.getResources().getStringArray(// .
				R.array.plans_type)[TYPE_BILLPERIOD]);
		cv.put(Plans.SHORTNAME, context.getString(R.string.billperiod_sn));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "12" });
		cv.clear();
		// spacer: 13, 17, 21
		cv.put(Plans.NAME, context.getResources().getStringArray(// .
				R.array.plans_type)[TYPE_SPACING]);
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "13" });
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "17" });
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "21" });
		cv.clear();
		// calls: 14
		cv.put(Plans.NAME, context.getString(R.string.calls));
		cv.put(Plans.SHORTNAME, context.getString(R.string.calls));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "14" });
		cv.clear();
		// calls in: 15
		cv.put(Plans.NAME, context.getString(R.string.calls_in));
		cv.put(Plans.SHORTNAME, context.getString(R.string.calls_in_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "15" });
		cv.clear();
		// calls out: 16
		cv.put(Plans.NAME, context.getString(R.string.calls_out));
		cv.put(Plans.SHORTNAME, context.getString(R.string.calls_out_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "16" });
		cv.clear();
		// messages: 18
		cv.put(Plans.NAME, context.getString(R.string.messages));
		cv.put(Plans.SHORTNAME, context.getString(R.string.messages_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "18" });
		cv.clear();
		// sms in: 19
		cv.put(Plans.NAME, context.getString(R.string.sms_in));
		cv.put(Plans.SHORTNAME, context.getString(R.string.sms_in_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "19" });
		cv.clear();
		// sms out: 20
		cv.put(Plans.NAME, context.getString(R.string.sms_out));
		cv.put(Plans.SHORTNAME, context.getString(R.string.sms_out_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "20" });
		cv.clear();
		// mms in: 27
		cv.put(Plans.NAME, context.getString(R.string.mms_in));
		cv.put(Plans.SHORTNAME, context.getString(R.string.mms_in_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "27" });
		cv.clear();
		// mms out: 28
		cv.put(Plans.NAME, context.getString(R.string.mms_out));
		cv.put(Plans.SHORTNAME, context.getString(R.string.mms_out_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "28" });
		cv.clear();
		// data: 22
		cv.put(Plans.NAME, context.getString(R.string.data_));
		cv.put(Plans.SHORTNAME, context.getString(R.string.data));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "22" });
		cv.clear();
		// data in/out: 23
		cv.put(Plans.NAME, context.getString(R.string.data_inout));
		cv.put(Plans.SHORTNAME, context.getString(R.string.data_inout_));
		db.update(Plans.TABLE, cv, Plans.ID + "=?", new String[] { "23" });
		cv.clear();
		// rules
		// data
		cv.put(Rules.NAME, context.getString(R.string.data));
		db.update(Rules.TABLE, cv, Rules.ID + "=?", new String[] { "1" });
		cv.clear();
		// calls in
		cv.put(Rules.NAME, context.getString(R.string.calls_in));
		db.update(Rules.TABLE, cv, Rules.ID + "=?", new String[] { "2" });
		cv.clear();
		// calls out
		cv.put(Rules.NAME, context.getString(R.string.calls_out));
		db.update(Rules.TABLE, cv, Rules.ID + "=?", new String[] { "3" });
		cv.clear();
		// sms in
		cv.put(Rules.NAME, context.getString(R.string.sms_in));
		db.update(Rules.TABLE, cv, Rules.ID + "=?", new String[] { "4" });
		cv.clear();
		// sms out
		cv.put(Rules.NAME, context.getString(R.string.sms_out));
		db.update(Rules.TABLE, cv, Rules.ID + "=?", new String[] { "5" });
		cv.clear();
		// mms in
		cv.put(Rules.NAME, context.getString(R.string.mms_in));
		db.update(Rules.TABLE, cv, Rules.ID + "=?", new String[] { "7" });
		cv.clear();
		// mms out
		cv.put(Rules.NAME, context.getString(R.string.mms_out));
		db.update(Rules.TABLE, cv, Rules.ID + "=?", new String[] { "8" });
		cv.clear();
		// exclude numbers from calls
		cv.put(NumbersGroup.NAME,
				context.getString(R.string.numbergroup_excalls));
		db.update(NumbersGroup.TABLE, cv, NumbersGroup.ID + "=?",
				new String[] { "1" });
		cv.clear();
		// exclude numbers from sms
		cv.put(NumbersGroup.NAME, // .
				context.getString(R.string.numbergroup_exsms));
		db.update(NumbersGroup.TABLE, cv, NumbersGroup.ID + "=?",
				new String[] { "2" });

		Preferences.setDefaultPlan(context, true);
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
				this.getContext()
						.getContentResolver()
						.notifyChange(
								ContentUris.withAppendedId(Numbers.GROUP_URI,
										gid), null);
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
			ret = db.delete(Numbers.TABLE,
					DbUtils.sqlAnd(Numbers.GID + "=" + id, selection),
					selectionArgs);
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
				this.getContext()
						.getContentResolver()
						.notifyChange(
								ContentUris
										.withAppendedId(Hours.GROUP_URI, gid),
								null);
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
			ret = db.delete(Hours.TABLE,
					DbUtils.sqlAnd(Hours.GID + "=" + id, selection),
					selectionArgs);
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
		case EXPORT_RULESET:
		case EXPORT_LOGS:
		case EXPORT_NUMGROUPS:
		case EXPORT_HOURGROUPS:
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
		Log.d(TAG, "insert(" + uri + "," + values + ")");
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		long ret = -1;
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
			Log.d(TAG, "insert(): null");
			return null;
		} else {
			this.getContext().getContentResolver().notifyChange(uri, null);
			final Uri u = ContentUris.withAppendedId(uri, ret);
			Log.d(TAG, "insert(): " + u);
			return u;
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
		Log.d(TAG, "query(" + uri + "," + selection + ")");
		final SQLiteDatabase db = this.mOpenHelper.getReadableDatabase();
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

		Cursor c = null;

		switch (uid) {
		case LOGS_ID:
			qb.appendWhere(Logs.ID + "=" + ContentUris.parseId(uri));
		case LOGS:
			qb.setTables(Logs.TABLE);
			break;
		case LOGS_JOIN:
			qb.setTables(Logs.TABLE + " LEFT OUTER JOIN " + Plans.TABLE
					+ " ON (" + Logs.TABLE + "." + Logs.PLAN_ID + "="
					+ Plans.TABLE + "." + Plans.ID + ") LEFT OUTER JOIN "
					+ Rules.TABLE + " ON (" + Logs.TABLE + "." + Logs.RULE_ID
					+ "=" + Rules.TABLE + "." + Rules.ID + ")");
			break;
		case LOGS_SUM:
			qb.setTables(Logs.TABLE + " INNER JOIN " + Plans.TABLE + " ON ("
					+ Logs.TABLE + "." + Logs.PLAN_ID + "=" + Plans.TABLE + "."
					+ Plans.ID + ")");
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
			break;
		case PLANS_SUM_ID:
			qb.appendWhere(Plans.TABLE + "." + Plans.ID + "="
					+ ContentUris.parseId(uri));
		case PLANS_SUM:
			final boolean hideZero = Utils.parseBoolean(
					uri.getQueryParameter(Plans.PARAM_HIDE_ZERO), false);
			final boolean hideNoCost = Utils.parseBoolean(
					uri.getQueryParameter(Plans.PARAM_HIDE_NOCOST), false);
			final long date = Utils.parseLong(
					uri.getQueryParameter(Plans.PARAM_DATE),
					System.currentTimeMillis());
			qb.setTables(Plans.TABLE + " left outer join " + Logs.TABLE
					+ " on (" + Logs.TABLE + "." + Logs.PLAN_ID + "="
					+ Plans.TABLE + "." + Plans.ID + " or " + Plans.TABLE + "."
					+ Plans.MERGED_PLANS + " like '%,'||" + Logs.TABLE + "."
					+ Logs.PLAN_ID + "||',%' or ( " + Plans.TABLE + "."
					+ Plans.TYPE + "=" + TYPE_BILLPERIOD + " and " + Logs.TABLE
					+ "." + Logs.PLAN_ID + " in ( select " + Plans.ID
					+ " from " + Plans.TABLE + " as p where p."
					+ Plans.BILLPERIOD_ID + "=" + Plans.TABLE + "." + Plans.ID
					+ ")))");
			groupBy = Plans.TABLE + "." + Plans.ID;
			if (hideZero || hideNoCost) {
				having = Plans.TYPE + " in(" + TYPE_BILLPERIOD + ","
						+ TYPE_SPACING + "," + TYPE_TITLE + ")";

				if (hideZero) {
					having += " or " + Plans.SUM_BP_BILLED_AMOUNT + ">0";
				}

				if (hideNoCost) {
					if (hideZero) {
						having += " and ";
					} else {
						having = " or ";
					}
					having += Plans.SUM_COST + ">0";
				}
			}

			if (orderBy == null) {
				orderBy = Plans.TABLE + "." + Plans.ORDER;
			}
			proj = new String[l];
			final Calendar now = Calendar.getInstance();
			now.setTimeInMillis(date);
			final Calendar today = (Calendar) now.clone();
			today.set(Calendar.MILLISECOND, 0);
			today.set(Calendar.SECOND, 0);
			today.set(Calendar.MINUTE, 0);
			today.set(Calendar.HOUR_OF_DAY, 0);
			String billps = "(CASE ";
			String nbillps = "(CASE ";
			Cursor cursor = db.query(Plans.TABLE, new String[] { Plans.ID,
					Plans.BILLPERIOD, Plans.BILLDAY }, Plans.WHERE_BILLPERIODS,
					null, null, null, null);
			if (cursor.moveToFirst()) {
				do {
					int period = cursor.getInt(1);
					long bday = cursor.getLong(2);
					Calendar bd = Plans.getBillDay(period, bday, now, false);
					Calendar nbd = Plans.getBillDay(period, bd, now, true);
					final long pid = cursor.getLong(0);
					billps += " WHEN " + Plans.TABLE + "." + Plans.ID + "="
							+ pid + " or " + Plans.TABLE + "."
							+ Plans.BILLPERIOD_ID + "=" + pid + " THEN "
							+ bd.getTimeInMillis();
					nbillps += " WHEN " + Plans.TABLE + "." + Plans.ID + "="
							+ pid + " or " + Plans.TABLE + "."
							+ Plans.BILLPERIOD_ID + "=" + pid + " THEN "
							+ nbd.getTimeInMillis();
				} while (cursor.moveToNext());
			}
			cursor.close();
			cursor = null;
			billps += " ELSE 0 END)";
			nbillps += " ELSE 0 END)";
			for (int i = 0; i < l; i++) {
				proj[i] = projection[i].// .
						replace("{" + Plans.SUM_BILLDAY + "}", // .
								billps).// .
						replace("{" + Plans.SUM_NEXTBILLDAY + "}", // .
								nbillps).// .
						replace("{" + Plans.SUM_NOW + "}", // .
								String.valueOf(date)).//
						replace("{" + Plans.SUM_TODAY + "}", // .
								String.valueOf(today.getTimeInMillis()));
				Log.d(TAG, "proj[" + i + "]: " + proj[i]);
			}
			break;
		case RULES_ID:
			qb.appendWhere(Rules.ID + "=" + ContentUris.parseId(uri));
		case RULES:
			qb.setTables(Rules.TABLE);
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
		case EXPORT_RULESET:
		case EXPORT_LOGS:
		case EXPORT_NUMGROUPS:
		case EXPORT_HOURGROUPS:
			Log.d(TAG, "export proj: + " + projection);
			String fn = null;
			switch (uid) {
			case EXPORT_RULESET:
				fn = EXPORT_RULESET_FILE;
				break;
			case EXPORT_LOGS:
				fn = EXPORT_LOGS_FILE;
				break;
			case EXPORT_NUMGROUPS:
				fn = EXPORT_NUMGROUPS_FILE;
				break;
			case EXPORT_HOURGROUPS:
				fn = EXPORT_HOURGROUPS_FILE;
				break;
			default:
				break;
			}
			Object[] retArray = new Object[l];
			for (int i = 0; i < l; i++) {
				if (projection[i].equals(OpenableColumns.DISPLAY_NAME)) {
					retArray[i] = fn;
				} else if (projection[i].equals(OpenableColumns.SIZE)) {
					final File d = Environment.getExternalStorageDirectory();
					final File f = new File(d, PACKAGE + File.separator + fn);
					retArray[i] = f.length();
				}
			}
			final MatrixCursor ret = new MatrixCursor(projection, 1);
			ret.addRow(retArray);
			Log.d(TAG, "query(): " + ret.getCount());
			return ret;
		default:
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}

		if (proj == null) {
			proj = projection;
		}
		// Run the query
		c = qb.query(db, proj, selection, selectionArgs, groupBy, having, // .
				orderBy);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(this.getContext().getContentResolver(), uri);
		Log.d(TAG, "query(): " + c.getCount());
		return c;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int update(final Uri uri, final ContentValues values,
			final String selection, final String[] selectionArgs) {
		Log.d(TAG, "update(" + uri + "," + selection + "," + values + ")");
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
			ret = db.update(Numbers.TABLE, values, DbUtils.sqlAnd(Numbers.ID
					+ "=" + ContentUris.parseId(uri), selection), // .
					selectionArgs);
			if (ret > 0 && values != null) {
				i = values.getAsLong(Numbers.GID);
				if (i >= 0) {
					this.getContext()
							.getContentResolver()
							.notifyChange(
									ContentUris.withAppendedId(
											Numbers.GROUP_URI, i), null);
				}
			}
			break;
		case NUMBERS_GROUP_ID:
			ret = db.update(NumbersGroup.TABLE, values,
					DbUtils.sqlAnd(
							NumbersGroup.ID + "=" + ContentUris.parseId(uri),
							selection), selectionArgs);
			break;
		case HOURS_ID:
			ret = db.update(Hours.TABLE, values, DbUtils.sqlAnd(Hours.ID + "="
					+ ContentUris.parseId(uri), selection), selectionArgs);
			if (ret > 0 && values != null) {
				i = values.getAsLong(Numbers.GID);
				if (i >= 0) {
					this.getContext()
							.getContentResolver()
							.notifyChange(
									ContentUris.withAppendedId(Hours.GROUP_URI,
											i), null);
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
		Log.d(TAG, "update(): " + ret);
		return ret;
	}

	/**
	 * {@inheritDoc}
	 */
	public ParcelFileDescriptor openFile(final Uri uri, final String mode)
			throws FileNotFoundException {
		Log.d(TAG, "openFile(" + uri.toString() + ")");
		final File d = Environment.getExternalStorageDirectory();
		String fn = null;
		if (uri.equals(EXPORT_RULESET_URI)) {
			fn = DataProvider.EXPORT_RULESET_FILE;
		} else if (uri.equals(EXPORT_LOGS_URI)) {
			fn = DataProvider.EXPORT_LOGS_FILE;
		} else if (uri.equals(EXPORT_NUMGROUPS_URI)) {
			fn = DataProvider.EXPORT_NUMGROUPS_FILE;
		} else if (uri.equals(EXPORT_HOURGROUPS_URI)) {
			fn = DataProvider.EXPORT_HOURGROUPS_FILE;
		}
		if (fn == null) {
			return null;
		}
		final File f = new File(d, PACKAGE + File.separator + fn);
		return ParcelFileDescriptor
				.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
	}

	/**
	 * Backup {@link SQLiteDatabase} on file system level.
	 * 
	 * @param context
	 *            {@link Context}
	 * @return backup successful?
	 */
	public static boolean doBackup(final Context context) {
		Log.i(TAG, "doBackup()");
		boolean ret = true;
		final SQLiteDatabase db = new DatabaseHelper(context)
				.getWritableDatabase();
		final String path = db.getPath();
		try {
			Log.d(TAG, "cp " + path + " " + path + ".bak");
			Utils.copyFile(path, path + ".bak");
		} catch (IOException e) {
			ret = false;
			Log.e(TAG, "could not backup databse", e);
		}
		db.close();
		return ret;
	}

	/**
	 * Try to restore backup of {@link SQLiteDatabase}.
	 * 
	 * @param db
	 *            {@link SQLiteDatabase}
	 * @return restored successfully?
	 */
	private static boolean doRestore(final SQLiteDatabase db) {
		Log.i(TAG, "doRestore()");
		boolean ret = false;
		final String path = db.getPath();
		final File bak = new File(path + ".bak");
		if (bak.exists()) {
			try {
				db.close();
				Utils.copyFile(bak.getAbsolutePath(), path);
				ret = true;
				Log.w(TAG, "backup restored");
			} catch (IOException e) {
				Log.e(TAG, "failed restoring backup", e);
			}
		} else {
			Log.w(TAG, "no backup found");
		}
		return ret;
	}
}
