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
import android.os.Handler;
import android.os.Message;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.ui.Plans;
import de.ub0r.android.lib.DbUtils;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Class matching logs via rules to plans.
 * 
 * @author flx
 */
public final class RuleMatcher {
	/** Tag for output. */
	private static final String TAG = "rm";

	/** Steps for updating the GUI. */
	private static final int PROGRESS_STEPS = 50;

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
		 * @param overwritePlanId
		 *            overwrite plan id
		 * @return {@link Rule}
		 */
		private static Rule getRule(final ContentResolver cr, final long id,
				final long overwritePlanId) {
			if (id < 0) {
				return null;
			}
			Rule ret = null;
			final Cursor cursor = cr.query(ContentUris.withAppendedId(
					DataProvider.Rules.CONTENT_URI, id),
					DataProvider.Rules.PROJECTION, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				ret = new Rule(cr, cursor, overwritePlanId);
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
		 * @param overwritePlanId
		 *            overwrite plan id
		 * @param cursor
		 *            {@link Cursor}
		 */
		Rule(final ContentResolver cr, final Cursor cursor,
				final long overwritePlanId) {
			this.id = cursor.getLong(DataProvider.Rules.INDEX_ID);
			if (overwritePlanId >= 0) {
				this.planId = overwritePlanId;
			} else {
				this.planId = cursor.getLong(DataProvider.Rules.INDEX_PLAN_ID);
			}
			this.negate = cursor.getInt(DataProvider.Rules.INDEX_NOT) > 0;
			this.what = cursor.getInt(DataProvider.Rules.INDEX_WHAT);
			this.what0 = Group.getGroup(cr, this.what, cursor
					.getLong(DataProvider.Rules.INDEX_WHAT0));
			this.what1 = getRule(cr, cursor
					.getLong(DataProvider.Rules.INDEX_WHAT1), this.planId);
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
				ret = (t == DataProvider.TYPE_CALL);
				break;
			case DataProvider.Rules.WHAT_DATA:
				ret = (t == DataProvider.TYPE_DATA);
				break;
			case DataProvider.Rules.WHAT_INCOMMING:
				ret = log.getInt(DataProvider.Logs.INDEX_DIRECTION) == // .
				DataProvider.DIRECTION_IN;
				break;
			case DataProvider.Rules.WHAT_HOURS:
			case DataProvider.Rules.WHAT_NUMBERS:
				ret = this.what0 != null && this.what0.match(log);
				break;
			case DataProvider.Rules.WHAT_LIMIT_REACHED:
				final Plan p = plans.get(this.planId);
				if (p != null) {
					p.checkBillday(log);
					ret = !p.isInLimit();
				}
				if (ret) {
					Log.d(TAG, "limit reached: " + this.planId);
				}
				break;
			case DataProvider.Rules.WHAT_MMS:
				ret = (t == DataProvider.TYPE_MMS);
				break;
			case DataProvider.Rules.WHAT_ROAMING:
				ret = log.getInt(DataProvider.Logs.INDEX_ROAMED) > 0;
				break;
			case DataProvider.Rules.WHAT_SMS:
				ret = (t == DataProvider.TYPE_SMS);
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
		/** Type of log. */
		private final int type;
		/** Type of limit. */
		private final int limitType;
		/** Limit. */
		private final long limit;
		/** Billmode. */
		private final int billModeFirstLength, billModeNextLength;
		/** Billday. */
		private final Calendar billday;
		/** Billperiod. */
		private final int billperiod;
		/** Cost per item. */
		private final float costPerItem;
		/** Cost per amount. */
		private final float costPerAmount1, costPerAmount2;
		/** Cost per item in limit. */
		private final float costPerItemInLimit;
		/** Cost per amount in limit. */
		private final float costPerAmountInLimit1, costPerAmountInLimit2;
		/** Units for mixed plans. */
		private final int mixedUnitsCall, mixedUnitsSMS, mixedUnitsMMS;

		/** Last valid billday. */
		private Calendar currentBillday = null;
		/** Time of nextBillday. */
		private long nextBillday = -1;
		/** Amount billed this period. */
		private long billedAmount = 0;
		/** Cost billed this period. */
		private float billedCost = 0;

		/** {@link ContentResolver}. */
		private final ContentResolver cResolver;

		/**
		 * Load a {@link Plan}.
		 * 
		 * @param cr
		 *            {@link ContentResolver}
		 * @param cursor
		 *            {@link Cursor}
		 */
		Plan(final ContentResolver cr, final Cursor cursor) {
			this.cResolver = cr;
			this.id = cursor.getLong(DataProvider.Plans.INDEX_ID);
			this.type = cursor.getInt(DataProvider.Plans.INDEX_TYPE);
			this.limitType = cursor.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			if (this.limitType == DataProvider.LIMIT_TYPE_UNITS) {
				this.limit = DataProvider.Plans.getLimit(this.type, cursor
						.getLong(DataProvider.Plans.INDEX_LIMIT));
			} else {
				this.limit = cursor.getLong(DataProvider.Plans.INDEX_LIMIT);
			}

			this.costPerItem = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_ITEM);
			this.costPerAmount1 = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_AMOUNT1);
			this.costPerAmount2 = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_AMOUNT2);
			this.costPerItemInLimit = cursor
					.getFloat(DataProvider.Plans.INDEX_COST_PER_ITEM_IN_LIMIT);
			this.costPerAmountInLimit1 = cursor.getFloat(DataProvider.Plans.// .
					INDEX_COST_PER_AMOUNT_IN_LIMIT1);
			this.costPerAmountInLimit2 = cursor.getFloat(DataProvider.Plans.// .
					INDEX_COST_PER_AMOUNT_IN_LIMIT2);
			this.mixedUnitsCall = cursor
					.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_CALL);
			this.mixedUnitsSMS = cursor
					.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_SMS);
			this.mixedUnitsMMS = cursor
					.getInt(DataProvider.Plans.INDEX_MIXED_UNITS_MMS);

			final int bp = cursor.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
			if (bp >= 0) {
				final Cursor c = cr.query(ContentUris.withAppendedId(
						DataProvider.Plans.CONTENT_URI, bp),
						DataProvider.Plans.PROJECTION, null, null, null);
				if (c != null && c.moveToFirst()) {
					this.billday = Calendar.getInstance();
					this.billday.setTimeInMillis(c
							.getLong(DataProvider.Plans.INDEX_BILLDAY));
					this.billperiod = c
							.getInt(DataProvider.Plans.INDEX_BILLPERIOD);
				} else {
					this.billperiod = DataProvider.BILLPERIOD_INFINITE;
					this.billday = null;
				}
				if (c != null && !c.isClosed()) {
					c.close();
				}
			} else {
				this.billperiod = DataProvider.BILLPERIOD_INFINITE;
				this.billday = null;
			}
			final String billmode = cursor
					.getString(DataProvider.Plans.INDEX_BILLMODE);
			if (billmode != null && billmode.contains("/")) {
				String[] billmodes = billmode.split("/");
				this.billModeFirstLength = Utils.parseInt(billmodes[0], 1);
				this.billModeNextLength = Utils.parseInt(billmodes[1], 1);
			} else {
				this.billModeFirstLength = 1;
				this.billModeNextLength = 1;
			}
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
		 * Check if this log is starting a new billing period.
		 * 
		 * @param log
		 *            {@link Cursor} pointing to log
		 */
		void checkBillday(final Cursor log) {
			// skip for infinite bill periods
			if (this.billperiod == DataProvider.BILLPERIOD_INFINITE) {
				return;
			}

			// check whether date is in current bill period
			final long d = log.getLong(DataProvider.Logs.INDEX_DATE);
			if (this.currentBillday == null || this.nextBillday < d
					|| d < this.currentBillday.getTimeInMillis()) {
				final Calendar now = Calendar.getInstance();
				now.setTimeInMillis(d);
				this.currentBillday = DataProvider.Plans.getBillDay(
						this.billperiod, this.billday, now, false);
				this.nextBillday = DataProvider.Plans.getBillDay(
						this.billperiod, this.billday, now, true)
						.getTimeInMillis();

				// load old stats
				final String where = DbUtils.sqlAnd(DataProvider.Plans
						.getBilldayWhere(this.billperiod, this.currentBillday,
								now), DataProvider.Logs.PLAN_ID + " = "
						+ this.id);
				final Cursor c = this.cResolver.query(
						DataProvider.Logs.SUM_URI,
						DataProvider.Logs.PROJECTION_SUM, where, null, null);
				if (c != null && c.moveToFirst()) {
					this.billedAmount = c
							.getLong(DataProvider.Logs.INDEX_SUM_BILL_AMOUNT);
					this.billedCost = c
							.getFloat(DataProvider.Logs.INDEX_SUM_COST);
				} else {
					this.billedAmount = 0;
					this.billedCost = 0;
				}
				if (c != null && !c.isClosed()) {
					c.close();
				}
			}
		}

		/**
		 * Return true if billed cost/amount is in limit.
		 * 
		 * @return true if billed cost/amount is in limit
		 */
		boolean isInLimit() {
			switch (this.limitType) {
			case DataProvider.LIMIT_TYPE_COST:
				return this.billedCost < this.limit;
			case DataProvider.LIMIT_TYPE_UNITS:
				return this.billedAmount < this.limit;
			default:
				return true;
			}
		}

		/**
		 * Round up time with bill mode in mind.
		 * 
		 * @param time
		 *            time
		 * @return rounded time
		 */
		private long roundTime(final long time) {
			// 0 => 0
			if (time <= 0) {
				return 0;
			}
			final long fl = this.billModeFirstLength;
			final long nl = this.billModeNextLength;
			// !0 ..
			if (time <= fl) { // round first slot
				return fl;
			}
			if (nl == 0) {
				return fl;
			}
			if (time % nl == 0 || nl == 1) {
				return time;
			}
			// round up to next full slot
			return ((time / nl) + 1) * nl;
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
			long ret = log.getLong(DataProvider.Logs.INDEX_AMOUNT);
			final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
			switch (t) {
			case DataProvider.TYPE_CALL:
				ret = this.roundTime(ret);
				break;
			default:
				break;
			}

			if (this.type == DataProvider.TYPE_MIXED) {
				switch (t) {
				case DataProvider.TYPE_CALL:
					ret = (ret * this.mixedUnitsCall)
							/ CallMeter.SECONDS_MINUTE;
					break;
				case DataProvider.TYPE_SMS:
					ret = ret * this.mixedUnitsSMS;
					break;
				case DataProvider.TYPE_MMS:
					ret = ret * this.mixedUnitsMMS;
					break;
				default:
					break;
				}
			}

			if (updatePlan) {
				this.billedAmount += ret;
			}
			return ret;
		}

		/**
		 * Get cost for amount.
		 * 
		 * @param log
		 *            {@link Cursor} pointing to log
		 * @param bAmount
		 *            billed amount
		 * @param updatePlan
		 *            update {@link Plan}'s fields
		 * @return cost
		 */
		float getCost(final Cursor log, final long bAmount,
				final boolean updatePlan) {
			float ret = 0;
			float cpi, cpa1, cpa2;
			if (this.limitType != DataProvider.LIMIT_TYPE_NONE
					&& this.isInLimit()) {
				cpi = this.costPerItemInLimit;
				cpa1 = this.costPerAmountInLimit1;
				cpa2 = this.costPerAmountInLimit2;
			} else {
				cpi = this.costPerItem;
				cpa1 = this.costPerAmount1;
				cpa2 = this.costPerAmount2;
			}

			ret += cpi;

			final int t = log.getInt(DataProvider.Logs.INDEX_TYPE);
			switch (t) {
			case DataProvider.TYPE_CALL:
				if (bAmount <= this.billModeFirstLength) {
					ret += (cpa1 * bAmount) / CallMeter.SECONDS_MINUTE;
				} else {
					ret += (cpa1 * this.billModeFirstLength)
							/ CallMeter.SECONDS_MINUTE;
					ret += (cpa2 * (bAmount - // .
							this.billModeFirstLength))
							/ CallMeter.SECONDS_MINUTE;
				}
				break;
			case DataProvider.TYPE_DATA:
				ret += (cpa1 * bAmount) / CallMeter.BYTE_MB;
				break;
			default:
				break;
			}

			if (updatePlan) {
				this.billedCost += ret;
			}
			return ret;
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
				rules.add(new Rule(cr, cursor, -1));
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
			cursor = null;
		}

		// load plans
		plans = new HashMap<Long, Plan>();
		cursor = cr.query(DataProvider.Plans.CONTENT_URI,
				DataProvider.Plans.PROJECTION,
				DataProvider.Plans.WHERE_REALPLANS, null, null);
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
	 * @return true if a log was matched
	 */
	private static boolean matchLog(final ContentResolver cr, // .
			final Cursor log) {
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
				p.checkBillday(log);
				final ContentValues cv = new ContentValues();
				cv.put(DataProvider.Logs.PLAN_ID, pid);
				cv.put(DataProvider.Logs.RULE_ID, rid);
				final long ba = p.getBilledAmount(log, true);
				cv.put(DataProvider.Logs.BILL_AMOUNT, ba);
				final float bc = p.getCost(log, ba, true);
				cv.put(DataProvider.Logs.COST, bc);
				cr.update(ContentUris.withAppendedId(
						DataProvider.Logs.CONTENT_URI, lid), cv, null, null);
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
		return matched;
	}

	/**
	 * Math logs.
	 * 
	 * @param context
	 *            {@link Context}
	 * @param showStatus
	 *            post status to dialog/handler
	 * @return true if a log was matcheds
	 */
	static synchronized boolean match(final Context context,
			final boolean showStatus) {
		Log.d(TAG, "match(ctx, " + showStatus + ")");
		boolean ret = false;
		load(context);
		final Cursor cursor = context.getContentResolver().query(
				DataProvider.Logs.CONTENT_URI, DataProvider.Logs.PROJECTION,
				DataProvider.Logs.PLAN_ID + " = " + DataProvider.NO_ID, null,
				DataProvider.Logs.DATE + " ASC");
		if (cursor != null && cursor.moveToFirst()) {
			final ContentResolver cr = context.getContentResolver();
			final int l = cursor.getCount();
			Handler h = null;
			if (showStatus) {
				h = Plans.getHandler();
				if (h != null) {
					final Message m = h.obtainMessage(// .
							Plans.MSG_BACKGROUND_PROGRESS_MATCHER);
					m.arg1 = 0;
					m.arg2 = l;
					m.sendToTarget();
				}
			}
			int i = 1;
			do {
				ret |= matchLog(cr, cursor);
				if (h != null && (// .
						i % PROGRESS_STEPS == 0 || // .
						(i < PROGRESS_STEPS && i % CallMeter.TEN == 0))) {
					final Message m = h.obtainMessage(// .
							Plans.MSG_BACKGROUND_PROGRESS_MATCHER);
					m.arg1 = i;
					m.sendToTarget();
				}
				++i;
			} while (cursor.moveToNext());
		}
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}
		return ret;
	}
}
