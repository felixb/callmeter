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

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;

/**
 * @author flx
 */
public final class DataProvider extends ContentProvider {
	/** Tag for output. */
	private static final String TAG = "dp";

	/** Name of the {@link SQLiteDatabase}. */
	private static final String DATABASE_NAME = "callmeter.db";
	/** Version of the {@link SQLiteDatabase}. */
	private static final int DATABASE_VERSION = 4;

	/** Type of log: mixed. */
	public static final int TYPE_MIXED = 0;
	/** Type of log: title. */
	public static final int TYPE_TITLE = 1;
	/** Type of log: spacing. */
	public static final int TYPE_SPACING = 2;
	/** Type of log: billmode. */
	public static final int TYPE_BILLMODE = 3;
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

		/** Content {@link Uri}. */
		public static final Uri CONTENT_URI = Uri.parse("content://"
				+ AUTHORITY + "/logs");
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
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ PLAN_ID + " LONG," // .
					+ RULE_ID + " LONG," // .
					+ TYPE + " INTEGER," // .
					+ DIRECTION + " INTEGER," // .
					+ DATE + " LONG," // .
					+ AMOUNT + " LONG," // .
					+ BILL_AMOUNT + " INTEGER," // .
					+ REMOTE + " TEXT,"// .
					+ ROAMED + " BOOL," // .
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			onCreate(db);
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
		/** Index in projection: limit used month. */
		public static final int INDEX_USED_MONTH = 7;
		/** Index in projection: limit used all time. */
		public static final int INDEX_USED_ALL = 8;
		/** Index in projection: limit used count. */
		public static final int INDEX_USED_COUNT = 9;
		/** Index in projection: Billmode. */
		public static final int INDEX_BILLMODE = 10;
		/** Index in projection: Billday. */
		public static final int INDEX_BILLDAY = 11;
		/** Index in projection: Billperiod. */
		public static final int INDEX_BILLPERIOD = 12;
		/** Index in projection: Cost per item. */
		public static final int INDEX_COST_PER_ITEM = 13;
		/** Index in projection: Cost per amount. */
		public static final int INDEX_COST_PER_AMOUNT = 14;
		/** Index in projection: Cost per item in limit. */
		public static final int INDEX_COST_PER_ITEM_IN_LIMIT = 15;
		/** Index in projection: Cost per plan. */
		public static final int INDEX_COST_PER_PLAN = 16;
		/** Index in projection: Cost. */
		public static final int INDEX_COST = 17;

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
		/** Limit used - month. */
		public static final String USED_MONTH = "_used_month";
		/** Limit used - all. */
		public static final String USED_ALL = "_used_all";
		/** Limit used - count. */
		public static final String USED_COUNT = "_used_count";
		/** Billmode. */
		public static final String BILLMODE = "_billmode";
		/** Billday. */
		public static final String BILLDAY = "_billday";
		/** Billperiod. */
		public static final String BILLPERIOD = "_billperiod";
		/** Cost per item. */
		public static final String COST_PER_ITEM = "_cost_per_item";
		/** Cost per amount. */
		public static final String COST_PER_AMOUNT = "_cost_per_amount";
		/** Cost per item in limit. */
		public static final String COST_PER_ITEM_IN_LIMIT = // .
		"_cost_per_item_in_limit";
		/** Cost per plan. */
		public static final String COST_PER_PLAN = "_cost_per_plan";
		/** Cost. */
		public static final String COST = "_cost";

		/** Projection used for query. */
		public static final String[] PROJECTION = new String[] { ID, ORDER,
				NAME, SHORTNAME, TYPE, LIMIT_TYPE, LIMIT, USED_MONTH, USED_ALL,
				USED_COUNT, BILLMODE, BILLDAY, BILLPERIOD, COST_PER_ITEM,
				COST_PER_AMOUNT, COST_PER_ITEM_IN_LIMIT, COST_PER_PLAN, COST };

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
			PROJECTION_MAP.put(USED_MONTH, USED_MONTH);
			PROJECTION_MAP.put(USED_ALL, USED_ALL);
			PROJECTION_MAP.put(USED_COUNT, USED_COUNT);
			PROJECTION_MAP.put(BILLMODE, BILLMODE);
			PROJECTION_MAP.put(BILLDAY, BILLDAY);
			PROJECTION_MAP.put(BILLPERIOD, BILLPERIOD);
			PROJECTION_MAP.put(COST_PER_ITEM, COST_PER_ITEM);
			PROJECTION_MAP.put(COST_PER_AMOUNT, COST_PER_AMOUNT);
			PROJECTION_MAP.put(COST_PER_ITEM_IN_LIMIT, COST_PER_ITEM_IN_LIMIT);
			PROJECTION_MAP.put(COST_PER_PLAN, COST_PER_PLAN);
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
			db.execSQL("CREATE TABLE " + TABLE + " (" // .
					+ ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // .
					+ ORDER + " INTEGER," // .
					+ NAME + " TEXT,"// .
					+ SHORTNAME + " TEXT,"// .
					+ TYPE + " TEXT, " // .
					+ LIMIT_TYPE + " INTEGER,"// .
					+ LIMIT + " INTEGER,"// .
					+ USED_MONTH + " INTEGER,"// .
					+ USED_ALL + " INTEGER,"// .
					+ USED_COUNT + " INTEGER,"// .
					+ BILLMODE + " TEXT,"// .
					+ BILLDAY + " INTEGER,"// .
					+ BILLPERIOD + " INTEGER,"// .
					+ COST_PER_ITEM + " FLOAT,"// .
					+ COST_PER_AMOUNT + " FLOAT,"// .
					+ COST_PER_ITEM_IN_LIMIT + " FLOAT,"// .
					+ COST_PER_PLAN + " FLOAT," // .
					+ COST + " FLOAT" // .
					+ ");");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER
					+ ") VALUES ('Calls', 'Calls', " + TYPE_TITLE + ", 0 )");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + "," + LIMIT + "," + LIMIT_TYPE + ","
					+ USED_MONTH + ", " + ORDER
					+ ") VALUES ('Calls', 'Calls', " + TYPE_CALL + ", 10, 3, "
					+ LIMIT_TYPE_UNITS + ", 1 )");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER + ") VALUES ('space', '-', "
					+ TYPE_SPACING + ", 2 )");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER + ") VALUES ('SMS', 'SMS', "
					+ TYPE_TITLE + ", 3)");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER + ") VALUES ('SMS in', 'In', "
					+ TYPE_SMS + ", 4)");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER
					+ ") VALUES ('SMS out', 'Out', " + TYPE_SMS + ", 5)");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER + ") VALUES ('space', '-', "
					+ TYPE_SPACING + ", 6)");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER
					+ ") VALUES ('Data/UMTS', 'Data', " + TYPE_TITLE + ", 7)");
			db.execSQL("INSERT INTO " + TABLE + "(" + NAME + "," + SHORTNAME
					+ "," + TYPE + ", " + ORDER + ") VALUES ('Data', 'Data', "
					+ TYPE_DATA + ", 8)");
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			onCreate(db);
		}

		/** Default constructor. */
		private Plans() {
			// nothing here.
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
		/** Condition type: is roaming. */
		public static final int WHAT_ROAMING = 4;
		/** Condition type: match numbers. */
		public static final int WHAT_NUMBERS = 7;
		/** Condition type: match hours. */
		public static final int WHAT_HOURS = 8;
		/** Condition type: is limit reached. */
		public static final int WHAT_LIMIT_REACHED = 9;

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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			onCreate(db);
		}

		/** Default constructor. */
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			onCreate(db);
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
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			onCreate(db);
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			onCreate(db);
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
		 * Create table in {@link SQLiteDatabase}.
		 * 
		 * @param db
		 *            {@link SQLiteDatabase}
		 */
		public static void onCreate(final SQLiteDatabase db) {
			Log.i(TAG, "create table: " + TABLE);
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
			db.execSQL("DROP TABLE IF EXISTS " + TABLE);
			onCreate(db);
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

	/** Authority. */
	public static final String AUTHORITY = "de.ub0r.android.callmeter."
			+ "provider";

	/** {@link UriMatcher}. */
	private static final UriMatcher URI_MATCHER;

	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(AUTHORITY, "logs", LOGS);
		URI_MATCHER.addURI(AUTHORITY, "logs/#", LOGS_ID);
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
	}

	/**
	 * This class helps open, create, and upgrade the database file.
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {

		/**
		 * Default Constructor.
		 * 
		 * @param context
		 *            {@link Context}
		 */
		DatabaseHelper(final Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
		}
	}

	/** {@link DatabaseHelper}. */
	private DatabaseHelper mOpenHelper;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int delete(final Uri uri, final String selection,
			final String[] selectionArgs) {
		final SQLiteDatabase db = this.mOpenHelper.getWritableDatabase();
		int ret = 0;
		String id;
		switch (URI_MATCHER.match(uri)) {
		case LOGS:
			ret = db.delete(Logs.TABLE, selection, selectionArgs);
			break;
		case LOGS_ID:
			ret = db.delete(Logs.TABLE, DbUtils.sqlAnd(Logs.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case PLANS_ID:
			ret = db.delete(Plans.TABLE, DbUtils.sqlAnd(Plans.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case RULES_ID:
			ret = db.delete(Rules.TABLE, DbUtils.sqlAnd(Rules.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case NUMBERS_ID:
			ret = db.delete(Numbers.TABLE, DbUtils.sqlAnd(Numbers.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case NUMBERS_GID:
		case NUMBERS_GROUP_ID:
			id = uri.getPathSegments().get(2);
			ret = db.delete(Numbers.TABLE, DbUtils.sqlAnd(Numbers.GID + "="
					+ id, selection), selectionArgs);
			ret += db.delete(NumbersGroup.TABLE, NumbersGroup.ID + " = " + id,
					null);
			break;
		case HOURS_ID:
			ret = db.delete(Hours.TABLE, DbUtils.sqlAnd(Hours.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case HOURS_GID:
		case HOURS_GROUP_ID:
			id = uri.getPathSegments().get(2);
			ret = db.delete(Hours.TABLE, DbUtils.sqlAnd(Hours.GID + "=" + id,
					selection), selectionArgs);
			ret += db
					.delete(HoursGroup.TABLE, HoursGroup.ID + " = " + id, null);
			break;
		default:
			db.close();
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
			db.close();
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

		switch (URI_MATCHER.match(uri)) {
		case LOGS_ID:
			qb.appendWhere(Logs.ID + "=" + uri.getPathSegments().get(1));
		case LOGS:
			qb.setTables(Logs.TABLE);
			qb.setProjectionMap(Logs.PROJECTION_MAP);
			break;
		case PLANS_ID:
			qb.appendWhere(Plans.ID + "=" + uri.getPathSegments().get(1));
		case PLANS:
			qb.setTables(Plans.TABLE);
			qb.setProjectionMap(Plans.PROJECTION_MAP);
			break;
		case RULES_ID:
			qb.appendWhere(Rules.ID + "=" + uri.getPathSegments().get(1));
		case RULES:
			qb.setTables(Rules.TABLE);
			qb.setProjectionMap(Rules.PROJECTION_MAP);
			break;
		case NUMBERS_GID:
			qb.appendWhere(Numbers.GID + "=" + uri.getPathSegments().get(2));
			qb.setTables(Numbers.TABLE);
			qb.setProjectionMap(Numbers.PROJECTION_MAP);
			break;
		case NUMBERS_ID:
			qb.appendWhere(Numbers.ID + "=" + uri.getPathSegments().get(1));
		case NUMBERS:
			qb.setTables(Numbers.TABLE);
			qb.setProjectionMap(Numbers.PROJECTION_MAP);
			break;
		case NUMBERS_GROUP_ID:
			qb
					.appendWhere(NumbersGroup.ID + "="
							+ uri.getPathSegments().get(2));
		case NUMBERS_GROUP:
			qb.setTables(NumbersGroup.TABLE);
			qb.setProjectionMap(NumbersGroup.PROJECTION_MAP);
			break;
		case HOURS_GID:
			qb.appendWhere(Hours.GID + "=" + uri.getPathSegments().get(2));
			qb.setTables(Hours.TABLE);
			qb.setProjectionMap(Hours.PROJECTION_MAP);
			break;
		case HOURS_ID:
			qb.appendWhere(Hours.ID + "=" + uri.getPathSegments().get(1));
		case HOURS:
			qb.setTables(Hours.TABLE);
			qb.setProjectionMap(Hours.PROJECTION_MAP);
			break;
		case HOURS_GROUP_ID:
			qb.appendWhere(HoursGroup.ID + "=" + uri.getPathSegments().get(2));
		case HOURS_GROUP:
			qb.setTables(HoursGroup.TABLE);
			qb.setProjectionMap(HoursGroup.PROJECTION_MAP);
			break;
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
		Cursor c = qb.query(db, projection, selection, selectionArgs, null,
				null, orderBy);

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
		int ret = 0;
		switch (URI_MATCHER.match(uri)) {
		case LOGS_ID:
			ret = db.update(Logs.TABLE, values, DbUtils.sqlAnd(Logs.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case PLANS_ID:
			ret = db.update(Plans.TABLE, values, DbUtils.sqlAnd(Plans.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case RULES_ID:
			ret = db.update(Rules.TABLE, values, DbUtils.sqlAnd(Rules.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case NUMBERS_ID:
			ret = db.update(Numbers.TABLE, values, DbUtils.sqlAnd(Numbers.ID
					+ "=" + uri.getPathSegments().get(1), selection),
					selectionArgs);
			break;
		case NUMBERS_GROUP_ID:
			ret = db.update(NumbersGroup.TABLE, values, DbUtils.sqlAnd(
					NumbersGroup.ID + "=" + uri.getPathSegments().get(2),
					selection), selectionArgs);
		case HOURS_ID:
			ret = db.update(Hours.TABLE, values, DbUtils.sqlAnd(Hours.ID + "="
					+ uri.getPathSegments().get(1), selection), selectionArgs);
			break;
		case HOURS_GROUP_ID:
			ret = db.update(HoursGroup.TABLE, values, DbUtils.sqlAnd(
					HoursGroup.ID + "=" + uri.getPathSegments().get(2),
					selection), selectionArgs);
		default:
			db.close();
			throw new IllegalArgumentException("Unknown Uri " + uri);
		}
		if (ret > 0) {
			this.getContext().getContentResolver().notifyChange(uri, null);
		}
		return ret;
	}
}
