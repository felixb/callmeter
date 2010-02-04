/*
 * Copyright (C) 2010 Felix Bechstein
 * 
 * This file is part of Call Meter NG.
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
package de.ub0r.de.android.callMeterNG;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.gsm.SmsMessage;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * AsyncTask to handle calcualtions in background.
 * 
 * @author flx
 */
@SuppressWarnings("deprecation")
class Updater extends AsyncTask<Void, Void, Integer[]> {
	/** Tag for output. */
	private static final String TAG = "CallMeterNG.updater";

	/** Days of a week. */
	static final int DAYS_WEEK = 7;
	/** Hours of a day. */
	static final int HOURS_DAY = 24;
	/** Seconds of a minute. */
	static final int SECONDS_MINUTE = 60;

	/** Prefs: name for first day. */
	static final String PREFS_BILLDAY = "billday";
	/** Prefs: name for billingmode. */
	private static final String PREFS_BILLMODE = "billmode";
	/** Prefs: name for smsperiod. */
	private static final String PREFS_SMSPERIOD = "smsperiod";
	/** Prefs: name for smsbillday. */
	private static final String PREFS_SMSBILLDAY = "smsbillday";

	/** Prefs: split plans. */
	private static final String PREFS_SPLIT_PLANS = "plans_split";
	/** Prefs: merge plans for calls. */
	private static final String PREFS_MERGE_PLANS_CALLS = "plans_merge_calls";
	/** Prefs: merge plans for sms. */
	private static final String PREFS_MERGE_PLANS_SMS = "plans_merge_sms";
	/** Prefs: hours for plan 1. */
	private static final String PREFS_PLAN1_HOURS_PREFIX = "hours_1_";

	/** Prefs: plan1 totally free calls. */
	private static final String PREFS_PLAN1_T_FREE_CALLS = // .
	"plan1_total_free_calls";
	/** Prefs: plan1 free minutes. */
	private static final String PREFS_PLAN1_FREEMIN = "plan1_freemin";
	/** Prefs: plan1 totally free sms. */
	private static final String PREFS_PLAN1_T_FREE_SMS = "plan1_total_free_sms";
	/** Prefs: plan1 free sms. */
	private static final String PREFS_PLAN1_FREESMS = "plan1_freesms";
	/** Prefs: plan1 totally free calls. */
	private static final String PREFS_PLAN2_T_FREE_CALLS = // .
	"plan2_total_free_calls";
	/** Prefs: plan1 free minutes. */
	private static final String PREFS_PLAN2_FREEMIN = "plan2_freemin";
	/** Prefs: plan1 totally free sms. */
	private static final String PREFS_PLAN2_T_FREE_SMS = "plan2_total_free_sms";
	/** Prefs: plan1 free sms. */
	private static final String PREFS_PLAN2_FREESMS = "plan2_freesms";

	/** Prefs: custom name for plan 1. */
	private static final String PREFS_NAME_PLAN1 = "plan_name1";
	/** Prefs: custom name for plan 2. */
	private static final String PREFS_NAME_PLAN2 = "plan_name2";

	/** Prefs: merge sms into calls. */
	private static final String PREFS_MERGE_SMS_TO_CALLS = "merge_sms_calls";
	/**
	 * Prefs: merge sms into calls; number of seconds billed for a single sms.
	 */
	private static final String PREFS_MERGE_SMS_TO_CALLS_SECONDS = // .
	"merge_sms_calls_sec";
	/** Prefs: merge sms into calls; which plan to merge sms in. */
	private static final String PREFS_MERGE_SMS_PLAN1 = "merge_sms_plan1";

	/** Prefs: billmode: 1/1. */
	private static final String BILLMODE_1_1 = "1_1";

	/** ContentProvider Column: Body. */
	private static final String BODY = "body";

	/** Preference's name for time of last checked bill period for calls. */
	private static final String PREFS_CALLS_PERIOD_LASTCHECK = // .
	"calls_period_lastcheck";
	/** Preference's name for time of last walk for calls. */
	private static final String PREFS_CALLS_WALK_LASTCHECK = // .
	"calls_walk_lastcheck";
	/** Preference's name for saving calls in (this period). */
	private static final String PREFS_CALLS_PERIOD_IN = "calls_period_in";
	/** Preference's name for saving calls out plan #1 (this period). */
	private static final String PREFS_CALLS_PERIOD_OUT1 = "calls_period_out1";
	/** Preference's name for saving calls out plan #2 (this period). */
	private static final String PREFS_CALLS_PERIOD_OUT2 = "calls_period_out2";
	/** Preference's name for saving calls in (all). */
	private static final String PREFS_CALLS_ALL_IN = "calls_all_in";
	/** Preference's name for saving calls out (all). */
	private static final String PREFS_CALLS_ALL_OUT = "calls_all_out";

	/** Preference's name for time of last checked bill period for sms. */
	private static final String PREFS_SMS_PERIOD_LASTCHECK = // .
	"sms_period_lastcheck";
	/** Preference's name for time of last walk for sms. */
	private static final String PREFS_SMS_WALK_LASTCHECK = "sms_walk_lastcheck";
	/** Preference's name for saving sms in (this period). */
	private static final String PREFS_SMS_PERIOD_IN = "sms_period_in";
	/** Preference's name for saving sms out plan #1 (this period). */
	private static final String PREFS_SMS_PERIOD_OUT1 = "sms_period_out1";
	/** Preference's name for saving sms out plan #2 (this period). */
	private static final String PREFS_SMS_PERIOD_OUT2 = "sms_period_out2";
	/** Preference's name for saving sms in (all). */
	private static final String PREFS_SMS_ALL_IN = "sms_all_in";
	/** Preference's name for saving sms out (all). */
	private static final String PREFS_SMS_ALL_OUT = "sms_all_out";

	/** Value for calls out plan #1. */
	private static final int RESULT_CALLS1_VAL = 0;
	/** Limit for calls out plan #1. */
	private static final int RESULT_CALLS1_LIMIT = 1;
	/** Value for calls out plan #2. */
	private static final int RESULT_CALLS2_VAL = 2;
	/** Limit for calls out plan #2. */
	private static final int RESULT_CALLS2_LIMIT = 3;
	/** Value for sms out plan #1. */
	private static final int RESULT_SMS1_VAL = 4;
	/** Limit for sms out plan #1. */
	private static final int RESULT_SMS1_LIMIT = 5;
	/** Value for sms out plan #2. */
	private static final int RESULT_SMS2_VAL = 6;
	/** Limit for sms out plan #2. */
	private static final int RESULT_SMS2_LIMIT = 7;

	/** Status Strings. */
	private String callsIn, callsOut1, callsOut2, callsBillDate, smsIn,
			smsOut1, smsOut2, smsBillDate;
	/** Status TextViews. */
	private TextView twCallsIn, twCallsOut1, twCallsOut2, twCallsBillDate,
			twSMSIn, twSMSOut1, twSMSOut2, twSMSBillDate;
	/** Status ProgressBars. */
	private ProgressBar pbCalls1, pbCalls2, pbSMS1, pbSMS2;

	/** Update string every.. rounds */
	private static final int UPDATE_INTERVAL = 50;

	/** Merge plans for calls. */
	private boolean plansMergeCalls = false;
	/** Merge plans for sms. */
	private boolean plansMergeSms = false;

	/** Bill excluded people in plan1. */
	private boolean excludedToPlan1 = false;
	/** Bill excluded people in plan2. */
	private boolean excludedToPlan2 = false;

	/** Sum of displayed calls out. Used if merging sms into calls. */
	private int callsOutSum;

	/** Length of first billed timeslot. */
	private int lengthOfFirstSlot;
	/** Length of following timeslot. */
	private int lengthOfNextSlots;

	/** Preferences to use. */
	private final SharedPreferences prefs;
	/** Context to use. */
	private final Context context;
	/** Ref to CallMeter instance. */
	private final CallMeter callmeter;
	/** Run updates on GUI. */
	private final boolean updateGUI;

	/**
	 * AsyncTask updating stats.
	 * 
	 * @param c
	 *            Context
	 */
	Updater(final Context c) {
		this.context = c;
		if (c instanceof CallMeter) {
			Log.d(TAG, "running in foreground");
			this.updateGUI = true;
			this.callmeter = (CallMeter) c;
		} else {
			Log.d(TAG, "running in background");
			this.updateGUI = false;
			this.callmeter = null;
		}
		this.prefs = PreferenceManager.getDefaultSharedPreferences(c);
	}

	/**
	 * Get the bill date for calls.
	 * 
	 * @param prefs
	 *            {@link SharedPreferences}
	 * @return bill date
	 */
	private static Calendar getBillDayCalls(final SharedPreferences prefs) {
		return getBillDate(Integer
				.parseInt(prefs.getString(PREFS_BILLDAY, "0")));
	}

	/**
	 * Get the bill date for sms.
	 * 
	 * @param prefs
	 *            {@link SharedPreferences}
	 * @return bill date
	 */
	private static Calendar getBillDaySMS(final SharedPreferences prefs) {
		if (!prefs.getBoolean(PREFS_SMSPERIOD, false)) {
			return getBillDate(Integer.parseInt(prefs.getString(
					PREFS_SMSBILLDAY, "0")));
		}
		return getBillDayCalls(prefs);
	}

	/**
	 * Check if billing period changed for calls.
	 * 
	 * @param prefs
	 *            preferences
	 */
	static final void checkBillperiodCalls(final SharedPreferences prefs) {
		final Calendar billDate = getBillDayCalls(prefs);
		final long lastBill = billDate.getTimeInMillis();
		final long now = System.currentTimeMillis();
		final long lastCheck = prefs.getLong(PREFS_CALLS_PERIOD_LASTCHECK, 0);
		Log.d(TAG, "last check calls: " + now);
		Log.d(TAG, "lastBill: " + lastBill);

		final Editor editor = prefs.edit();
		if (lastCheck < lastBill) {
			editor.remove(PREFS_CALLS_PERIOD_IN);
			editor.remove(PREFS_CALLS_PERIOD_OUT1);
			editor.remove(PREFS_CALLS_PERIOD_OUT2);
		}
		editor.putLong(PREFS_CALLS_PERIOD_LASTCHECK, now);
		editor.commit();
		Log.d(TAG, "last check calls: " + now);
	}

	/**
	 * Check if billing period changed for sms.
	 * 
	 * @param prefs
	 *            preferences
	 */
	static final void checkBillperiodSMS(final SharedPreferences prefs) {
		final Calendar billDate = getBillDaySMS(prefs);
		final long lastBill = billDate.getTimeInMillis();
		final long now = System.currentTimeMillis();
		final long lastCheck = prefs.getLong(PREFS_SMS_PERIOD_LASTCHECK, 0);
		Log.d(TAG, "last check sms: " + lastCheck);
		Log.d(TAG, "lastBill: " + lastBill);

		final Editor editor = prefs.edit();
		if (lastCheck < lastBill) {
			editor.remove(PREFS_SMS_PERIOD_IN);
			editor.remove(PREFS_SMS_PERIOD_OUT1);
			editor.remove(PREFS_SMS_PERIOD_OUT2);
		}
		editor.putLong(PREFS_SMS_PERIOD_LASTCHECK, now);
		editor.commit();
		Log.d(TAG, "last check sms: " + now);
	}

	/**
	 * Load plans from preferences.
	 * 
	 * @param p
	 *            {@link SharedPreferences} to read plans from
	 * @return null if plans aren't splitetd, else true for all hours in plan1
	 */
	private boolean[][] loadPlans(final SharedPreferences p) {
		if (this.plansMergeCalls && this.plansMergeSms) {
			return null;
		}
		boolean[][] ret = new boolean[DAYS_WEEK][HOURS_DAY];
		for (int day = 0; day < DAYS_WEEK; day++) {
			for (int hour = 0; hour < HOURS_DAY; hour++) {
				ret[day][hour] = p.getBoolean(PREFS_PLAN1_HOURS_PREFIX
						+ (day + 1) + "_" + hour, false);
			}
		}
		return ret;
	}

	/**
	 * Check whether this a timesstamp is part of plan 1.
	 * 
	 * @param plans
	 *            plans
	 * @param d
	 *            timestamp
	 * @return true if timestamp is part of plan 1
	 */
	private boolean isPlan1(final boolean[][] plans, final long d) {
		if (plans == null || d < 0) {
			return true;
		}
		Calendar date = Calendar.getInstance();
		date.setTimeInMillis(d);
		final int day = (date.get(Calendar.DAY_OF_WEEK) + 5) % DAYS_WEEK;
		final int hour = date.get(Calendar.HOUR_OF_DAY);
		return plans[day][hour];
	}

	/**
	 * Update text on main Activity.
	 */
	private void updateText() {
		if (!this.updateGUI) {
			return;
		}
		this.twCallsBillDate.setText(this.callsBillDate);
		this.twCallsIn.setText(this.callsIn);
		this.twCallsOut1.setText(this.callsOut1);
		this.twCallsOut2.setText(this.callsOut2);

		this.twSMSBillDate.setText(this.smsBillDate);
		this.twSMSIn.setText(this.smsIn);
		this.twSMSOut1.setText(this.smsOut1);
		this.twSMSOut2.setText(this.smsOut2);
	}

	/**
	 * Get preferences for splitting/merging plans.
	 */
	private void getSplitMergePrefs() {
		if (this.prefs.getBoolean(PREFS_SPLIT_PLANS, false)) {
			this.plansMergeCalls = this.prefs.getBoolean(
					PREFS_MERGE_PLANS_CALLS, false);
			this.plansMergeSms = this.prefs.getBoolean(PREFS_MERGE_PLANS_SMS,
					false);
			this.excludedToPlan1 = !this.plansMergeCalls
					&& this.prefs.getBoolean(
							CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN1, false);
			this.excludedToPlan2 = !this.plansMergeCalls
					&& this.prefs.getBoolean(
							CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN2, false);
		} else {
			this.plansMergeCalls = true;
			this.plansMergeSms = true;
			this.excludedToPlan1 = false;
			this.excludedToPlan2 = false;
		}
	}

	/**
	 * Init status text.
	 */
	private void initStatusText() {
		this.callsBillDate = "?";
		this.callsIn = "?";
		this.callsOut1 = "?";
		this.callsOut2 = "?";
		this.smsBillDate = "?";
		this.smsIn = "?";
		this.smsOut1 = "?";
		this.smsOut2 = "?";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onPreExecute() {
		String namePlan1 = null;
		String namePlan2 = null;
		if (this.updateGUI) {
			this.callmeter.setProgressBarIndeterminateVisibility(true);

			namePlan1 = this.prefs.getString(PREFS_NAME_PLAN1, "");
			if (namePlan1.length() <= 0) {
				namePlan1 = this.context.getString(R.string.plan1);
			}
			namePlan2 = this.prefs.getString(PREFS_NAME_PLAN2, "");
			if (namePlan2.length() <= 0) {
				namePlan2 = this.context.getString(R.string.plan2);
			}
			namePlan1 = " (" + namePlan1 + ")";
			namePlan2 = " (" + namePlan2 + ")";
		}

		this.getSplitMergePrefs();

		if (this.updateGUI) {
			this.pbCalls1 = (ProgressBar) this.callmeter
					.findViewById(R.id.calls1_progressbar);
			this.pbCalls2 = (ProgressBar) this.callmeter
					.findViewById(R.id.calls2_progressbar);

			this.pbSMS1 = (ProgressBar) this.callmeter
					.findViewById(R.id.sms1_progressbar);
			this.pbSMS2 = (ProgressBar) this.callmeter
					.findViewById(R.id.sms2_progressbar);

			this.twCallsIn = (TextView) this.callmeter
					.findViewById(R.id.calls_in);
			this.twCallsOut1 = (TextView) this.callmeter
					.findViewById(R.id.calls1_out);
			this.twCallsOut2 = (TextView) this.callmeter
					.findViewById(R.id.calls2_out);
			this.twCallsBillDate = (TextView) this.callmeter
					.findViewById(R.id.calls_billdate);
			this.twSMSIn = (TextView) this.callmeter.findViewById(R.id.sms_in);
			this.twSMSOut1 = (TextView) this.callmeter
					.findViewById(R.id.sms1_out);
			this.twSMSOut2 = (TextView) this.callmeter
					.findViewById(R.id.sms2_out);
			this.twSMSBillDate = (TextView) this.callmeter
					.findViewById(R.id.sms_billdate);

			this.pbCalls1.setProgress(0);
			this.pbCalls1.setIndeterminate(false);
			this.pbCalls1.setVisibility(View.VISIBLE);
			if (this.plansMergeCalls) {
				((TextView) this.callmeter.findViewById(R.id.calls1_out_))
						.setText(String.format(this.context
								.getString(R.string.out_calls), ""));

				this.callmeter.findViewById(R.id.calls2_out_).setVisibility(
						View.GONE);
				this.twCallsOut2.setVisibility(View.GONE);
				this.pbCalls2.setVisibility(View.GONE);
			} else {
				this.callmeter.findViewById(R.id.calls2_out_).setVisibility(
						View.VISIBLE);
				this.twCallsOut2.setVisibility(View.VISIBLE);

				String s = this.context.getString(R.string.out_calls);
				((TextView) this.callmeter.findViewById(R.id.calls1_out_))
						.setText(String.format(s, namePlan1));
				((TextView) this.callmeter.findViewById(R.id.calls2_out_))
						.setText(String.format(s, namePlan2));

				this.pbCalls2.setProgress(0);
				this.pbCalls2.setIndeterminate(false);
				this.pbCalls2.setVisibility(View.VISIBLE);
			}
			this.pbSMS1.setProgress(0);
			this.pbSMS1.setIndeterminate(false);
			this.pbSMS1.setVisibility(View.VISIBLE);
			if (this.plansMergeSms) {
				((TextView) this.callmeter.findViewById(R.id.sms1_out_))
						.setText(String.format(this.context
								.getString(R.string.out_sms), ""));

				this.callmeter.findViewById(R.id.sms2_out_).setVisibility(
						View.GONE);
				this.twSMSOut2.setVisibility(View.GONE);
				this.pbSMS2.setVisibility(View.GONE);
			} else {
				this.callmeter.findViewById(R.id.sms2_out_).setVisibility(
						View.VISIBLE);
				this.twSMSOut2.setVisibility(View.VISIBLE);

				String s = this.context.getString(R.string.out_sms);
				((TextView) this.callmeter.findViewById(R.id.sms1_out_))
						.setText(String.format(s, namePlan1));
				((TextView) this.callmeter.findViewById(R.id.sms2_out_))
						.setText(String.format(s, namePlan2));

				this.pbSMS2.setProgress(0);
				this.pbSMS2.setIndeterminate(false);
				this.pbSMS2.setVisibility(View.VISIBLE);
			}

			int v = View.VISIBLE;
			if (this.prefs.getBoolean(PREFS_MERGE_SMS_TO_CALLS, false)) {
				v = View.GONE;
				this.callmeter.findViewById(R.id.sms2_out_).setVisibility(v);
				this.twSMSOut2.setVisibility(v);
				this.pbSMS2.setVisibility(v);
			}
			this.callmeter.findViewById(R.id.sms_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms_billdate_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms_billdate).setVisibility(v);
			this.callmeter.findViewById(R.id.sms1_out_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms1_out).setVisibility(v);
			this.callmeter.findViewById(R.id.sms_in_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms_in).setVisibility(v);
			this.pbSMS1.setVisibility(v);

			// common
			this.initStatusText();
			this.updateText();
		}
	}

	/**
	 * Walk calls.
	 * 
	 * @param plans
	 *            array of plans
	 * @param calBillDate
	 *            Date of billdate
	 * @param status
	 *            status to return
	 */
	private void walkCalls(final boolean[][] plans, final Calendar calBillDate,
			final Integer[] status) {
		checkBillperiodCalls(this.prefs);

		this.callsBillDate = DateFormat.getDateFormat(this.context).format(
				calBillDate.getTime());
		long billDate = calBillDate.getTimeInMillis();

		final String[] excludeNumbers = CallMeter.prefsExcludePeople
				.toArray(new String[1]);
		final int excludeNumbersSize = excludeNumbers.length;

		// report calls
		String[] projection = new String[] { Calls.TYPE, Calls.DURATION,
				Calls.DATE, Calls.NUMBER };

		// get time of last walk
		long lastWalk = this.prefs.getLong(PREFS_CALLS_WALK_LASTCHECK, 0);
		Log.d(TAG, "last walk calls: " + lastWalk);

		Cursor cur = this.context.getContentResolver().query(Calls.CONTENT_URI,
				projection, Calls.DATE + " > " + lastWalk, null,
				Calls.DATE + " DESC");

		String prefBillMode = this.prefs
				.getString(PREFS_BILLMODE, BILLMODE_1_1);
		String[] prefTimeSlots = prefBillMode.split("_");
		this.lengthOfFirstSlot = Integer.parseInt(prefTimeSlots[0]);
		this.lengthOfNextSlots = Integer.parseInt(prefTimeSlots[1]);
		prefBillMode = null;
		prefTimeSlots = null;

		int durIn = this.prefs.getInt(PREFS_CALLS_ALL_IN, 0);
		int durOut = this.prefs.getInt(PREFS_CALLS_ALL_OUT, 0);
		int durInMonth = this.prefs.getInt(PREFS_CALLS_PERIOD_IN, 0);
		int durOut1Month = this.prefs.getInt(PREFS_CALLS_PERIOD_OUT1, 0);
		int durOut2Month = this.prefs.getInt(PREFS_CALLS_PERIOD_OUT2, 0);

		int free1 = 0; // -1 -> totally free
		int free2 = 0;
		if (this.prefs.getBoolean(PREFS_PLAN1_T_FREE_CALLS, false)) {
			free1 = -1;
		} else {
			String s = this.prefs.getString(PREFS_PLAN1_FREEMIN, "0");
			if (s.length() > 0) {
				free1 = Integer.parseInt(s);
			}
		}
		if (this.prefs.getBoolean(PREFS_PLAN2_T_FREE_CALLS, false)) {
			free2 = -1;
		} else {
			String s = this.prefs.getString(PREFS_PLAN2_FREEMIN, "0");
			if (s.length() > 0) {
				free2 = Integer.parseInt(s);
			}
		}
		// walk through log
		if (cur.moveToFirst()) {
			int type;
			long d;
			String n;
			final int idType = cur.getColumnIndex(Calls.TYPE);
			final int idDuration = cur.getColumnIndex(Calls.DURATION);
			final int idDate = cur.getColumnIndex(Calls.DATE);
			final int idNumber = cur.getColumnIndex(Calls.NUMBER);
			lastWalk = cur.getLong(idDate);
			int t = 0;
			int i = 0;
			boolean p = true;
			boolean check = true;
			do {
				type = cur.getInt(idType);
				d = cur.getLong(idDate);
				Log.d(TAG, "got entry: " + d);
				switch (type) {
				case Calls.INCOMING_TYPE:
					t = cur.getInt(idDuration);
					durIn += t;
					if (billDate <= d) {
						durInMonth += this.roundTime(t);
					}
					break;
				case Calls.OUTGOING_TYPE:
					t = cur.getInt(idDuration);
					durOut += t;
					if (billDate <= d) {
						check = true;
						n = cur.getString(idNumber);
						// check if number should be excluded from billing
						for (int j = 1; j < excludeNumbersSize; j++) {
							final String s = excludeNumbers[j];
							if (s.startsWith("*")) {
								if (n.endsWith(s.substring(1))) {
									check = false;
									break;
								}
							} else if (s.endsWith("*")) {
								if (n
										.startsWith(s.substring(0,
												s.length() - 1))) {
									check = false;
									break;
								}
							} else if (n.equals(excludeNumbers[j])) {
								check = false;
								break;
							}
						}
						if (check) {
							p = this.isPlan1(plans, d);
							if (p) {
								durOut1Month += this.roundTime(t);
							} else {
								durOut2Month += this.roundTime(t);
							}
						} else {
							if (this.excludedToPlan1) {
								durOut1Month += this.roundTime(t);
							} else if (this.excludedToPlan2) {
								durOut2Month += this.roundTime(t);
							}
						}
					}
					break;
				default:
					break;
				}
				++i;
				if (i % UPDATE_INTERVAL == 1) {
					this.callsIn = calcString(durInMonth, 0, durIn, true);
					this.callsOut1 = calcString(durOut1Month, free1, durOut,
							true);
					this.callsOut2 = calcString(durOut2Month, free2, durOut,
							true);
					this.publishProgress((Void) null);
				}
			} while (cur.moveToNext());
		}
		this.callsIn = calcString(durInMonth, 0, durIn, true);
		this.callsOut1 = calcString(durOut1Month, free1, durOut, true);
		this.callsOut2 = calcString(durOut2Month, free2, durOut, true);

		status[RESULT_CALLS1_VAL] = durOut1Month;
		status[RESULT_CALLS1_LIMIT] = free1 * SECONDS_MINUTE;
		status[RESULT_CALLS2_VAL] = durOut2Month;
		status[RESULT_CALLS2_LIMIT] = free2 * SECONDS_MINUTE;
		this.publishProgress((Void) null);

		this.callsOutSum = durOut;

		Log.d(TAG, "last walk calls: " + lastWalk);
		final Editor editor = this.prefs.edit();
		editor.putInt(PREFS_CALLS_ALL_IN, durIn);
		editor.putInt(PREFS_CALLS_ALL_OUT, durOut);
		editor.putInt(PREFS_CALLS_PERIOD_IN, durInMonth);
		editor.putInt(PREFS_CALLS_PERIOD_OUT1, durOut1Month);
		editor.putInt(PREFS_CALLS_PERIOD_OUT2, durOut2Month);
		editor.putLong(PREFS_CALLS_WALK_LASTCHECK, lastWalk);
		editor.commit();
	}

	/**
	 * Walk sms.
	 * 
	 * @param plans
	 *            array of plans
	 * @param calBillDate
	 *            Date of billdate
	 * @param status
	 *            status to return
	 */
	private void walkSMS(final boolean[][] plans, final Calendar calBillDate,
			final Integer[] status) {
		checkBillperiodSMS(this.prefs);
		// report basics
		this.smsBillDate = DateFormat.getDateFormat(this.context).format(
				calBillDate.getTime());
		final long billDate = calBillDate.getTimeInMillis();
		final String[] projection = new String[] // .
		{ Calls.TYPE, Calls.DATE, BODY };

		// get time of last walk
		long lastWalk = this.prefs.getLong(PREFS_SMS_WALK_LASTCHECK, 0);
		Log.d(TAG, "last walk sms: " + lastWalk);

		final Cursor cur = this.context.getContentResolver().query(
				Uri.parse("content://sms"), projection,
				Calls.DATE + " > " + lastWalk, null, Calls.DATE + " DESC");
		int free1 = 0;
		int free2 = 0;
		if (this.prefs.getBoolean(PREFS_PLAN1_T_FREE_SMS, false)) {
			free1 = -1;
		} else {
			String s = this.prefs.getString(PREFS_PLAN1_FREESMS, "0");
			if (s.length() > 0) {
				free1 = Integer.parseInt(s);
			}
		}
		if (this.prefs.getBoolean(PREFS_PLAN2_T_FREE_SMS, false)) {
			free2 = -1;
		} else {
			String s = this.prefs.getString(PREFS_PLAN2_FREESMS, "0");
			if (s.length() > 0) {
				free2 = Integer.parseInt(s);
			}
		}

		int iSMSIn = this.prefs.getInt(PREFS_SMS_ALL_IN, 0);
		int iSMSOut = this.prefs.getInt(PREFS_SMS_ALL_OUT, 0);
		int smsInMonth = this.prefs.getInt(PREFS_SMS_PERIOD_IN, 0);
		int smsOut1Month = this.prefs.getInt(PREFS_SMS_PERIOD_OUT1, 0);
		int smsOut2Month = this.prefs.getInt(PREFS_SMS_PERIOD_OUT1, 0);

		boolean p = true;
		if (cur.moveToFirst()) {
			int type;
			long d;
			final int idType = cur.getColumnIndex(Calls.TYPE);
			final int idDate = cur.getColumnIndex(Calls.DATE);
			final int idBody = cur.getColumnIndex(BODY);
			lastWalk = cur.getLong(idDate);
			int i = 0;
			int l = 1;
			do {
				type = cur.getInt(idType);
				d = cur.getLong(idDate);
				Log.d(TAG, "got entry: " + d);
				l = SmsMessage.calculateLength(cur.getString(idBody), false)[0];
				switch (type) {
				case Calls.INCOMING_TYPE:
					iSMSIn += l;
					if (billDate <= d) {
						smsInMonth += l;
					}
					break;
				case Calls.OUTGOING_TYPE:
					iSMSOut += l;
					if (billDate <= d) {
						p = this.isPlan1(plans, d);
						if (p) {
							smsOut1Month += l;
						} else {
							smsOut2Month += l;
						}
					}
					break;
				default:
					break;
				}
				++i;
				if (i % UPDATE_INTERVAL == 1) {
					this.smsIn = calcString(smsInMonth, 0, iSMSIn, false);
					this.smsOut1 = calcString(smsOut2Month, free1, iSMSOut,
							false);
					this.smsOut2 = calcString(smsOut2Month, free2, iSMSOut,
							false);
					this.publishProgress((Void) null);
				}
			} while (cur.moveToNext());
		}

		this.smsIn = calcString(smsInMonth, 0, iSMSIn, false);
		this.smsOut1 = calcString(smsOut1Month, free1, iSMSOut, false);
		this.smsOut2 = calcString(smsOut2Month, free2, iSMSOut, false);

		status[RESULT_SMS1_VAL] = smsOut1Month;
		status[RESULT_SMS1_LIMIT] = free1;
		status[RESULT_SMS2_VAL] = smsOut2Month;
		status[RESULT_SMS2_LIMIT] = free2;

		if (this.prefs.getBoolean(PREFS_MERGE_SMS_TO_CALLS, false)) {
			// merge sms into calls.
			final boolean mergeToPlan1 = this.plansMergeCalls
					|| this.prefs.getBoolean(PREFS_MERGE_SMS_PLAN1, true);
			final int secondsForSMS = Integer.parseInt(this.prefs.getString(
					PREFS_MERGE_SMS_TO_CALLS_SECONDS, "0"));
			int i = 0; // plan 1 number of seconds
			if (!mergeToPlan1) {
				i = 2; // plan 2 number of seconds
			}
			status[i] += secondsForSMS * smsOut1Month;

			status[RESULT_SMS1_VAL] = 0;
			status[RESULT_SMS1_LIMIT] = 0;
			status[RESULT_SMS2_VAL] = 0;
			status[RESULT_SMS2_LIMIT] = 0;

			final String s = calcString(status[i], status[i + 1],
					this.callsOutSum, false); // false ->
			// no multiply 60s/min
			if (mergeToPlan1) {
				this.callsOut1 = s;
			} else {
				this.callsOut2 = s;
			}
		}

		Log.d(TAG, "last walk sms: " + lastWalk);
		final Editor editor = this.prefs.edit();
		editor.putInt(PREFS_SMS_ALL_IN, iSMSIn);
		editor.putInt(PREFS_SMS_ALL_OUT, iSMSOut);
		editor.putInt(PREFS_SMS_PERIOD_IN, smsInMonth);
		editor.putInt(PREFS_SMS_PERIOD_OUT1, smsOut1Month);
		editor.putInt(PREFS_SMS_PERIOD_OUT2, smsOut2Month);
		editor.putLong(PREFS_SMS_WALK_LASTCHECK, lastWalk);
		editor.commit();
	}

	/**
	 * Round up time with billmode in mind.
	 * 
	 * @param time
	 *            time
	 * @return rounded time
	 */
	private int roundTime(final int time) {
		// 0 => 0
		if (time == 0) {
			return 0;
		}
		// !0 ..
		if (time <= this.lengthOfFirstSlot) { // round first slot
			return this.lengthOfFirstSlot;
		}
		final int lons = this.lengthOfNextSlots;
		if (lons == 0) {
			return this.lengthOfFirstSlot;
		}
		if (time % lons == 0 || lons == 1) {
			return time;
		}
		// round up to next full slot
		return ((time / lons) + 1) * lons;
	}

	/**
	 * Run in backgrund.
	 * 
	 * @param arg0
	 *            Void[]
	 * @return status
	 */
	@Override
	protected Integer[] doInBackground(final Void... arg0) {

		// load splitted plans
		final boolean[][] plans = this.loadPlans(this.prefs);

		// progressbar positions: calls1_pos, calls1_max, calls2_*, sms*,
		final Integer[] ret = { 0, 0, 0, 0, 1, 1, 1, 1 };
		Calendar calBillDate = getBillDate(Integer.parseInt(this.prefs
				.getString(PREFS_BILLDAY, "0")));
		if (this.plansMergeCalls) {
			this.walkCalls(null, calBillDate, ret);
		} else {
			this.walkCalls(plans, calBillDate, ret);
		}

		// report sms
		if (!this.prefs.getBoolean(PREFS_SMSPERIOD, false)) {
			calBillDate = getBillDate(Integer.parseInt(this.prefs.getString(
					PREFS_SMSBILLDAY, "0")));
		}
		if (this.plansMergeSms) {
			this.walkSMS(null, calBillDate, ret);
		} else {
			this.walkSMS(plans, calBillDate, ret);
		}

		return ret;
	}

	/**
	 * Update progress.
	 * 
	 * @param progress
	 *            progress
	 */
	@Override
	protected final void onProgressUpdate(final Void... progress) {
		this.updateText();
	}

	/**
	 * Push data back to GUI. Hide progressbars.
	 * 
	 * @param result
	 *            result
	 */
	@Override
	protected final void onPostExecute(final Integer[] result) {
		if (this.updateGUI) {
			this.updateText();

			// calls
			ProgressBar pb1 = this.pbCalls1;
			ProgressBar pb2 = this.pbCalls2;
			if (result[RESULT_CALLS1_LIMIT] > 0) {
				pb1.setMax(result[1]);
				if (result[RESULT_CALLS1_LIMIT] > result[RESULT_CALLS1_VAL]) {
					pb1.setProgress(result[RESULT_CALLS1_LIMIT]);
				} else {
					pb1.setProgress(result[RESULT_CALLS1_VAL]);
				}
				pb1.setProgress(result[RESULT_CALLS1_VAL]);
				pb1.setVisibility(View.VISIBLE);
			} else {
				pb2.setVisibility(View.GONE);
				pb2 = this.pbCalls1;
			}
			if (this.plansMergeCalls || result[RESULT_CALLS2_LIMIT] <= 0) {
				pb2.setVisibility(View.GONE);
			} else {
				pb2.setMax(result[RESULT_CALLS2_LIMIT]);
				if (result[RESULT_CALLS2_LIMIT] > result[RESULT_CALLS2_VAL]) {
					pb2.setProgress(result[RESULT_CALLS2_LIMIT]);
				} else {
					pb2.setProgress(result[RESULT_CALLS2_VAL]);
				}
				pb2.setProgress(result[RESULT_CALLS2_VAL]);
				pb2.setVisibility(View.VISIBLE);
			}

			// sms
			pb1 = this.pbSMS1;
			pb2 = this.pbSMS2;
			if (result[RESULT_SMS1_LIMIT] > 0) {
				pb1.setMax(result[RESULT_SMS1_LIMIT]);
				if (result[RESULT_SMS1_VAL] > result[RESULT_SMS1_LIMIT]) {
					pb1.setProgress(result[RESULT_SMS1_LIMIT]);
				} else {
					pb1.setProgress(result[RESULT_SMS1_VAL]);
				}
				pb1.setVisibility(View.VISIBLE);
			} else {
				pb2.setVisibility(View.GONE);
				pb2 = this.pbSMS1;
			}
			if (this.plansMergeSms || result[RESULT_SMS2_LIMIT] <= 0) {
				pb2.setVisibility(View.GONE);
			} else {
				pb2.setMax(result[RESULT_SMS2_LIMIT]);
				if (result[RESULT_SMS2_VAL] > result[RESULT_SMS2_LIMIT]) {
					pb2.setProgress(result[RESULT_SMS2_LIMIT]);
				} else {
					pb2.setProgress(result[RESULT_SMS2_VAL]);
				}
				pb2.setVisibility(View.VISIBLE);
			}
		}

		if (this.updateGUI) {
			// FIXME
			((CallMeter) this.context)
					.setProgressBarIndeterminateVisibility(false);
		}
	}

	/**
	 * Build a String for given time/limit combination.
	 * 
	 * @param thisPeriod
	 *            used this billperiod
	 * @param limit
	 *            limit
	 * @param all
	 *            used all together
	 * @param calls
	 *            calls/sms?
	 * @return String holding all the data
	 */
	private static String calcString(final int thisPeriod, final int limit,
			final int all, final boolean calls) {
		if (limit > 0) {
			if (calls) {
				return ((thisPeriod * CallMeter.HUNDRET) / // .
						(limit * SECONDS_MINUTE))
						+ "% / " + getTime(thisPeriod) + " / " + getTime(all);
			} else {
				return ((thisPeriod * CallMeter.HUNDRET) / limit) + "% / "
						+ thisPeriod + " / " + all;
			}
		} else {
			if (calls) {
				return getTime(thisPeriod) + " / " + getTime(all);
			} else {
				return thisPeriod + " / " + all;
			}
		}
	}

	/**
	 * Parse number of seconds to a readable time format.
	 * 
	 * @param seconds
	 *            seconds
	 * @return parsed string
	 */
	private static String getTime(final int seconds) {
		String ret;
		int d = seconds / 86400;
		int h = (seconds % 86400) / 3600;
		int m = (seconds % 3600) / 60;
		int s = seconds % 60;
		if (d > 0) {
			ret = d + "d ";
		} else {
			ret = "";
		}
		if (h > 0 || d > 0) {
			if (h < 10) {
				ret += "0";
			}
			ret += h + ":";
		}
		if (m > 0 || h > 0 || d > 0) {
			if (m < 10 && h > 0) {
				ret += "0";
			}
			ret += m + ":";
		}
		if (s < 10 && (m > 0 || h > 0 || d > 0)) {
			ret += "0";
		}
		ret += s;
		if (d == 0 && h == 0 && m == 0) {
			ret += "s";
		}
		return ret;
	}

	/**
	 * Return billing date as Calendar for a given day of month.
	 * 
	 * @param billDay
	 *            first day of bill
	 * @return date as Calendar
	 */
	private static Calendar getBillDate(final int billDay) {
		Calendar cal = Calendar.getInstance();
		if (cal.get(Calendar.DAY_OF_MONTH) < billDay) {
			cal.add(Calendar.MONTH, -1);
		}
		cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), billDay);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal;
	}
}
