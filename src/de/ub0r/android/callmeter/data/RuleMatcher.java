/*
 * Copyright (C) 2009-2010 Felix Bechstein
 * 
 * This file is part of NetCounter.
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import de.ub0r.android.lib.Log;

/**
 * Class matching logs via rules to plans.
 * 
 * @author flx
 */
public final class RuleMatcher {
	/** Tag for output. */
	private static final String TAG = "rm";

	/**
	 * A single Rule.
	 * 
	 * @author flx
	 */
	private static class Rule {
		/**
		 * Group of arguments.
		 * 
		 * @author flx
		 */
		private abstract static class Group {
			/**
			 * Math a log.
			 * 
			 * @param log
			 *            {@link Cursor} representing the log.
			 * @return matched?
			 */
			abstract boolean match(final Cursor log);

			/**
			 * Get the {@link Group}.
			 * 
			 * @param cr
			 *            {@link ContentResolver}
			 * @param what
			 *            Kind of rule
			 * @param what0
			 *            argument
			 * @return {@link Group}
			 */
			static Group getGroup(final ContentResolver cr, final int what,
					final long what0) {
				if (what0 < 0) {
					return null;
				}
				Group ret = null;
				if (what == DataProvider.Rules.WHAT_HOURS) {
					ret = new HoursGroup(cr, what0);
				} else if (what == DataProvider.Rules.WHAT_NUMBERS) {
					ret = new NumbersGroup(cr, what0);
				}
				return ret;
			}
		}

		/** Group of numbers. */
		private static final class NumbersGroup extends Group {
			/** List of numbers. */
			private final ArrayList<String> numbers = new ArrayList<String>();

			/**
			 * Default Constructor.
			 * 
			 * @param cr
			 *            {@link ContentResolver}
			 * @param what0
			 *            argument
			 */
			private NumbersGroup(final ContentResolver cr, final long what0) {
				final Cursor cursor = cr.query(ContentUris.withAppendedId(
						DataProvider.Numbers.GROUP_URI, what0),
						DataProvider.Numbers.PROJECTION, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					do {
						this.numbers.add(cursor
								.getString(DataProvider.Numbers.INDEX_NUMBER));
					} while (cursor.moveToNext());
				}
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			boolean match(final Cursor log) {
				String number = log.getString(DataProvider.Logs.INDEX_REMOTE);
				if (number == null || number.length() == 0) {
					return false;
				}
				if (number.startsWith("+")) {
					number = number.substring(1);
				}
				final int l = this.numbers.size();
				for (int i = 0; i < l; i++) {
					String n = this.numbers.get(i);
					if (n == null || n.length() == 0) {
						return false;
					}
					if (n.startsWith("+")) {
						n = n.substring(1);
					}
					if (number.equals(n)) {
						return true;
					}
					if (n.startsWith("%")) {
						if (n.endsWith("%")) {
							return number.contains(n.substring(1,
									n.length() - 1));
						} else {
							return number.endsWith(n.substring(1));
						}
					} else if (n.endsWith("%")) {
						return number
								.startsWith(n.substring(0, n.length() - 1));
					}
				}
				return false;
			}
		}

		/** Group of hours. */
		private static final class HoursGroup extends Group {
			/** List of hours. */
			private final HashMap<Integer, HashSet<Integer>> hours = // .
			new HashMap<Integer, HashSet<Integer>>();

			/**
			 * Default Constructor.
			 * 
			 * @param cr
			 *            {@link ContentResolver}
			 * @param what0
			 *            argument
			 */
			private HoursGroup(final ContentResolver cr, final long what0) {
				final Cursor cursor = cr.query(ContentUris.withAppendedId(
						DataProvider.Hours.GROUP_URI, what0),
						DataProvider.Hours.PROJECTION, null, null, null);
				if (cursor != null && cursor.moveToFirst()) {
					do {
						final int d = cursor
								.getInt(DataProvider.Hours.INDEX_DAY);
						final int h = cursor
								.getInt(DataProvider.Hours.INDEX_HOUR);
						if (this.hours.containsKey(d)) {
							this.hours.get(d).add(h);
						} else {
							final HashSet<Integer> hs = new HashSet<Integer>();
							hs.add(h);
							this.hours.put(d, hs);
						}
					} while (cursor.moveToNext());
				}
				if (cursor != null && !cursor.isClosed()) {
					cursor.close();
				}
			}

			/**
			 * {@inheritDoc}
			 */
			@Override
			boolean match(final Cursor log) {
				long date = log.getLong(DataProvider.Logs.INDEX_DATE);
				final Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(date);
				final int d = (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY)
						% Calendar.SATURDAY;
				final int h = cal.get(Calendar.HOUR_OF_DAY) + 1;
				for (int k : this.hours.keySet()) {
					if (k == 0 || k == d) {
						for (int v : this.hours.get(k)) {
							if (v == 0 || v == h) {
								return true;
							}
						}
					}
				}
				return false;
			}
		}

		/** Id. */
		private final long id;
		/** ID of plan referred by this rule. */
		private final long planId;
		/** Name. */
		private final String name;
		/** Negate rule? */
		private final boolean negate;
		/** Kind of rule. */
		private final int what;
		/** Target 0. */
		private final Group what0;
		/** Rule to "and". */
		private final Rule what1;

		/**
		 * Load a {@link Rule} by id.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param id
		 *            ID of {@link Rule}
		 * @return {@link Rule}
		 */
		private static Rule getRule(final ContentResolver cr, final long id) {
			if (id < 0) {
				return null;
			}
			Rule ret = null;
			final Cursor cursor = cr.query(ContentUris.withAppendedId(
					DataProvider.Rules.CONTENT_URI, id),
					DataProvider.Rules.PROJECTION, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret = new Rule(cr, cursor);
			}
			if (cursor != null && !cursor.isClosed()) {
				cursor.close();
			}
			return ret;
		}

		/**
		 * Load a {@link Rule}.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param cursor
		 *            {@link Cursor}
		 */
		Rule(final ContentResolver cr, final Cursor cursor) {
			this.id = cursor.getLong(DataProvider.Rules.INDEX_ID);
			this.planId = cursor.getLong(DataProvider.Rules.INDEX_PLAN_ID);
			this.name = cursor.getString(DataProvider.Rules.INDEX_NAME);
			this.negate = cursor.getInt(DataProvider.Rules.INDEX_NOT) > 0;
			this.what = cursor.getInt(DataProvider.Rules.INDEX_WHAT);
			this.what0 = Group.getGroup(cr, this.what, cursor
					.getLong(DataProvider.Rules.INDEX_WHAT0));
			this.what1 = getRule(cr, cursor
					.getLong(DataProvider.Rules.INDEX_WHAT1));
		}

		/**
		 * @return {@link Rule}'s id
		 */
		long getId() {
			return this.id;
		}

		/**
		 * @return {@link Plan}'s id
		 */
		long getPlanId() {
			return this.planId;
		}

		/**
		 * Math a log.
		 * 
		 * @param log
		 *            {@link Cursor} representing the log.
		 * @return matched?
		 */
		boolean match(final Cursor log) {
			final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
			boolean ret = false;
			switch (this.what) {
			case DataProvider.Rules.WHAT_CALL:
				if (t == DataProvider.TYPE_CALL) {
					ret = true;
				}
				break;
			case DataProvider.Rules.WHAT_DATA:
				if (t == DataProvider.TYPE_DATA) {
					ret = true;
				}
				break;
			case DataProvider.Rules.WHAT_INCOMMING:
				ret = log.getInt(DataProvider.Logs.INDEX_DIRECTION) == // .
				DataProvider.DIRECTION_IN;
				break;
			case DataProvider.Rules.WHAT_HOURS:
			case DataProvider.Rules.WHAT_NUMBERS:
				ret = this.what0 != null && this.what0.match(log);
				break;
			case DataProvider.Rules.WHAT_LIMIT_REACHED:// TODO
				break;
			case DataProvider.Rules.WHAT_MMS:
				if (t == DataProvider.TYPE_MMS) {
					ret = true;
				}
				break;
			case DataProvider.Rules.WHAT_ROAMING:
				ret = log.getInt(DataProvider.Logs.INDEX_ROAMED) > 0;
				break;
			case DataProvider.Rules.WHAT_SMS:
				if (t == DataProvider.TYPE_SMS) {
					ret = true;
				}
				break;
			default:
				break;
			}
			if (this.negate) {
				ret ^= true;
			}
			if (ret && this.what1 != null) {
				return this.what1.match(log);
			}
			return ret;
		}
	}

	/**
	 * A single Plan.
	 * 
	 * @author flx
	 */
	private static class Plan {
		/** Id. */
		private final long id;
		/** Name. */
		private final String name;
		/** Short name. */
		private final String shortname;
		/** Type of log. */
		private final int type;
		/** Type of limit. */
		private final int limitType;
		/** Limit. */
		private final int limit;
		/** Limit used - month. */
		private int usedMonth;
		/** Limit used - all. */
		private int usedAll;
		/** Limit used - count. */
		private int usedCount;
		/** Billmode. */
		private final String billmode;
		/** Billday. */
		private final int billday;
		/** Billperiod. */
		private final int billperiod;
		/** Cost per item. */
		private final float costPerItem;
		/** Cost per amount. */
		private final float costPerAmount;
		/** Cost per item in limit. */
		private final float costPerItemInLimit;
		/** Cost per plan. */
		private final float costPerPlan;
		/** Cost. */
		private float cost;

		/**
		 * Load a {@link Plan}.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param cursor
		 *            {@link Cursor}
		 */
		Plan(final ContentResolver cr, final Cursor cursor) {
			this.id = cursor.getLong(DataProvider.Plans.INDEX_ID);
			this.name = cursor.getString(DataProvider.Plans.INDEX_NAME);
			this.shortname = cursor
					.getString(DataProvider.Plans.INDEX_SHORTNAME);
			this.type = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
			this.limitType = cursor.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			this.limit = cursor.getInt(DataProvider.Plans.INDEX_LIMIT);
			this.usedMonth = cursor.getInt(DataProvider.Plans.INDEX_USED_MONTH);
			this.usedAll = cursor.getInt(DataProvider.Plans.INDEX_USED_ALL);
			this.usedCount = cursor.getInt(DataProvider.Plans.INDEX_USED_COUNT);
			this.billmode = cursor.getString(DataProvider.Plans.INDEX_BILLMODE);
			this.billday = cursor.getInt(DataProvider.Plans.INDEX_BILLDAY);
			this.billperiod = cursor
					.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
			this.costPerItem = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_ITEM);
			this.costPerAmount = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_AMOUNT);
			this.costPerItemInLimit = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_ITEM_IN_LIMIT);
			this.costPerPlan = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_PLAN);
			this.cost = cursor.getFloat(DataProvider.Plans.INDEX_COST);
		}

		/**
		 * Get {@link Plan}s id.
		 * 
		 * @return {@link Plan}s id
		 */
		long getId() {
			return this.id;
		}

		/**
		 * Get billed amount for amount.
		 * 
		 * @param log
		 *            {@link Cursor} pointing to log
		 * @param updatePlan
		 *            update {@link Plan}'s fields
		 * @return billed amount.
		 */
		long getBilledAmount(final Cursor log, final boolean updatePlan) {
			final long a = log.getLong(DataProvider.Logs.INDEX_AMOUNT);
			return a; // TODO
		}

		/**
		 * Get cost for amount.
		 * 
		 * @param log
		 *            {@link Cursor} pointing to log
		 * @param updatePlan
		 *            update {@link Plan}'s fields
		 * @return cost
		 */
		float getCost(final Cursor log, final boolean updatePlan) {
			return 0; // TODO
		}
	}

	/**
	 * List of {@link Rule}s.
	 */
	private static ArrayList<Rule> rules = null;
	/**
	 * List of {@link Plan}s.
	 */
	private static HashMap<Long, Plan> plans = null;

	/**
	 * Default constructor.
	 */
	private RuleMatcher() {
	}

	/**
	 * Load {@link Rule}s and {@link Plan}s.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	private static void load(final Context context) {
		Log.d(TAG, "load()");
		if (rules != null && plans != null) {
			return;
		}
		final ContentResolver cr = context.getContentResolver();

		// load rules
		rules = new ArrayList<Rule>();
		Cursor cursor = cr.query(DataProvider.Rules.CONTENT_URI,
				DataProvider.Rules.PROJECTION, DataProvider.Rules.ISCHILD
						+ " = 0", null, DataProvider.Rules.ORDER);
		if (cursor != null && cursor.moveToFirst()) {
			do {
				rules.add(new Rule(cr, cursor));
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
			cursor = null;
		}

		// load plans
		plans = new HashMap<Long, Plan>();
		cursor = cr.query(DataProvider.Plans.CONTENT_URI, // TODO: excl. space..
				DataProvider.Plans.PROJECTION, null, null, null);
		if (cursor != null && cursor.moveToFirst()) {
			do {
				final long i = cursor.getLong(DataProvider.Plans.INDEX_ID);
				plans.put(i, new Plan(cr, cursor));
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
			cursor = null;
		}
	}

	/**
	 * Reload Rules and plans.
	 */
	static void flush() {
		Log.d(TAG, "flush()");
		rules = null;
		plans = null;
	}

	/**
	 * Unmatch all logs.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	public static void unmatch(final Context context) {
		Log.d(TAG, "unmatch()");
		ContentValues cv = new ContentValues();
		cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NO_ID);
		cv.put(DataProvider.Logs.RULE_ID, DataProvider.NO_ID);
		context.getContentResolver().update(DataProvider.Logs.CONTENT_URI, cv,
				null, null);
		flush();
	}

	/**
	 * Match a log.
	 * 
	 * @param cr
	 *            {@link ContentResolver}
	 * @param log
	 *            {@link Cursor} representing the log
	 */
	private static void matchLog(final ContentResolver cr, final Cursor log) {
		final long lid = log.getLong(DataProvider.Logs.INDEX_ID);
		Log.d(TAG, "matchLog(cr, " + lid + ")");
		boolean matched = false;
		final int l = rules.size();
		for (int i = 0; i < l; i++) {
			final Rule r = rules.get(i);
			if (!r.match(log)) {
				continue;
			}
			final Plan p = plans.get(r.getPlanId());
			if (p != null) {
				final long pid = p.getId();
				final long rid = r.getId();
				final ContentValues cv = new ContentValues();
				cv.put(DataProvider.Logs.PLAN_ID, pid);
				cv.put(DataProvider.Logs.RULE_ID, rid);
				cv.put(DataProvider.Logs.BILL_AMOUNT, p.getBilledAmount(log,
						true));
				cv.put(DataProvider.Logs.COST, p.getCost(log, true));
				cr.update(ContentUris.withAppendedId(
						DataProvider.Logs.CONTENT_URI, lid), cv, null, null);
				// TODO
				matched = true;
				break;
			}
		}
		if (!matched) {
			final ContentValues cv = new ContentValues();
			cv.put(DataProvider.Logs.PLAN_ID, DataProvider.NOT_FOUND);
			cv.put(DataProvider.Logs.RULE_ID, DataProvider.NOT_FOUND);
			cr.update(ContentUris.withAppendedId(DataProvider.Logs.CONTENT_URI,
					lid), cv, null, null);
		}
	}

	/**
	 * Math logs.
	 * 
	 * @param context
	 *            {@link Context}
	 */
	static synchronized void match(final Context context) {
		Log.d(TAG, "match()");
		load(context);
		final Cursor cursor = context.getContentResolver().query(
				DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
				DataProvider.Logs.PLAN_ID + " = " + DataProvider.NO_ID, null,
				DataProvider.Logs.DATE + " ASC");
		if (cursor != null && cursor.moveToFirst()) {
			final ContentResolver cr = context.getContentResolver();
			do {
				matchLog(cr, cursor);
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
	}
}
