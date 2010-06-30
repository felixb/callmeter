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
import java.util.Currency;
import java.util.Locale;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import de.ub0r.android.callmeter.CallMeter;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.ui.Plans;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.apis.TelephonyWrapper;

/**
 * AsyncTask to handle calcualtions in background.
 * 
 * @author flx
 */
public class Updater extends AsyncTask<Void, Void, Integer[]> {
	/** Tag for output. */
	private static final String TAG = "updater";

	/** Days of a week. */
	public static final int DAYS_WEEK = 7;
	/** Hours of a day. */
	public static final int HOURS_DAY = 24;
	/** Seconds of a minute. */
	public static final int SECONDS_MINUTE = 60;

	/** {@link TelephonyWrapper}. */
	private static final TelephonyWrapper WRAPPER = TelephonyWrapper
			.getInstance();

	/** Prefs: name for first day. */
	public static final String PREFS_BILLDAY = "billday";
	/** Prefs: name for billingmode. */
	private static final String PREFS_BILLMODE = "billmode";
	/** Prefs: name for smsperiod. */
	private static final String PREFS_SMSPERIOD = "smsperiod";
	/** Prefs: name for smsbillday. */
	private static final String PREFS_SMSBILLDAY = "smsbillday";

	/** Prefs: split plans. */
	public static final String PREFS_SPLIT_PLANS = "plans_split";
	/** Prefs: merge plans for calls. */
	public static final String PREFS_MERGE_PLANS_CALLS = "plans_merge_calls";
	/** Prefs: merge plans for sms. */
	public static final String PREFS_MERGE_PLANS_SMS = "plans_merge_sms";
	/** Prefs: hours for plan 1. */
	private static final String PREFS_PLAN1_HOURS_PREFIX = "hours_1_";

	/** Prefs: plan1 totally free calls. */
	private static final String PREFS_PLAN1_T_FREE_CALLS = // .
	"plan1_total_free_calls";
	/** Prefs: plan1 free minutes. */
	public static final String PREFS_PLAN1_FREEMIN = "plan1_freemin";
	/** Prefs: plan1 totally free sms. */
	private static final String PREFS_PLAN1_T_FREE_SMS = "plan1_total_free_sms";
	/** Prefs: plan1 cost per call. */
	public static final String PREFS_PLAN1_COST_PER_CALL = "plan1_cost_per_call";
	/** Prefs: plan1 cost per minute. */
	private static final String PREFS_PLAN1_COST_PER_MINUTE = // .
	"plan1_cost_per_minute";
	/** Prefs: plan1 cost per sms. */
	private static final String PREFS_PLAN1_COST_PER_SMS = // .
	"plan1_cost_per_sms";
	/** Prefs: plan1 free sms. */
	private static final String PREFS_PLAN1_FREESMS = "plan1_freesms";
	/** Prefs: plan1 totally free calls. */
	private static final String PREFS_PLAN2_T_FREE_CALLS = // .
	"plan2_total_free_calls";
	/** Prefs: plan2 cost per call. */
	public static final String PREFS_PLAN2_COST_PER_CALL = "plan2_cost_per_call";
	/** Prefs: plan2 cost per minute. */
	private static final String PREFS_PLAN2_COST_PER_MINUTE = // .
	"plan2_cost_per_minute";
	/** Prefs: plan2 cost per sms. */
	private static final String PREFS_PLAN2_COST_PER_SMS = // .
	"plan2_cost_per_sms";
	/** Prefs: plan1 free minutes. */
	public static final String PREFS_PLAN2_FREEMIN = "plan2_freemin";
	/** Prefs: plan1 totally free sms. */
	private static final String PREFS_PLAN2_T_FREE_SMS = "plan2_total_free_sms";
	/** Prefs: plan1 free sms. */
	private static final String PREFS_PLAN2_FREESMS = "plan2_freesms";

	/** Prefs: custom name for plan 1. */
	private static final String PREFS_NAME_PLAN1 = "plan_name1";
	/** Prefs: custom name for plan 2. */
	private static final String PREFS_NAME_PLAN2 = "plan_name2";

	/** Prefs: merge sms into calls. */
	public static final String PREFS_MERGE_SMS_TO_CALLS = "merge_sms_calls";
	/**
	 * Prefs: merge sms into calls; number of seconds billed for a single sms.
	 */
	private static final String PREFS_MERGE_SMS_TO_CALLS_SECONDS = // .
	"merge_sms_calls_sec";
	/** Prefs: merge sms into calls; which plan to merge sms in. */
	public static final String PREFS_MERGE_SMS_PLAN1 = "merge_sms_plan1";

	/** Prefs: billmode: 1/1. */
	private static final String BILLMODE_1_1 = "1_1";

	/** ContentProvider Column: Body. */
	private static final String BODY = "body";

	/** Preference's name for time of last checked bill period for calls. */
	public static final String PREFS_CALLS_PERIOD_LASTCHECK = "calls_period_lastcheck";
	/** Preference's name for time of last walk for calls. */
	public static final String PREFS_CALLS_WALK_LASTCHECK = "calls_walk_lastcheck";
	/** Preference's name for saving calls in plan #1 (this period). */
	public static final String PREFS_CALLS_PERIOD_IN1 = "calls_period_in";
	/** Preference's name for saving calls in plan #1 (this period, count). */
	public static final String PREFS_CALLS_PERIOD_IN1_COUNT = "calls_period_in_n";
	/** Preference's name for saving calls out plan #1 (this period). */
	public static final String PREFS_CALLS_PERIOD_OUT1 = "calls_period_out1";
	/** Preference's name for saving calls out plan #1 (this period, count). */
	public static final String PREFS_CALLS_PERIOD_OUT1_COUNT = "calls_period_out1_n";
	/** Preference's name for saving calls in plan #2 (this period). */
	public static final String PREFS_CALLS_PERIOD_IN2 = "calls_period_in2";
	/** Preference's name for saving calls in plan #2 (this period, count). */
	public static final String PREFS_CALLS_PERIOD_IN2_COUNT = "calls_period_in2_n";
	/** Preference's name for saving calls out plan #2 (this period). */
	public static final String PREFS_CALLS_PERIOD_OUT2 = "calls_period_out2";
	/** Preference's name for saving calls out plan #2 (this period, count). */
	public static final String PREFS_CALLS_PERIOD_OUT2_COUNT = "calls_period_out2_n";
	/** Preference's name for saving calls in (all). */
	public static final String PREFS_CALLS_ALL_IN = "calls_all_in";
	/** Preference's name for saving calls out (all). */
	public static final String PREFS_CALLS_ALL_OUT = "calls_all_out";
	/** Preference's name for billing incoming calls. */
	public static final String PREFS_CALLS_BILL_INCOMING = "bill_calls_incoming";

	/** Preference's name for time of last checked bill period for sms. */
	public static final String PREFS_SMS_PERIOD_LASTCHECK = "sms_period_lastcheck";
	/** Preference's name for time of last walk for sms. */
	public static final String PREFS_SMS_WALK_LASTCHECK = "sms_walk_lastcheck";
	/** Preference's name for saving sms in plan #1 (this period). */
	public static final String PREFS_SMS_PERIOD_IN1 = "sms_period_in";
	/** Preference's name for saving sms in plan #2 (this period). */
	public static final String PREFS_SMS_PERIOD_IN2 = "sms_period_in2";
	/** Preference's name for saving sms out plan #1 (this period). */
	public static final String PREFS_SMS_PERIOD_OUT1 = "sms_period_out1";
	/** Preference's name for saving sms out plan #2 (this period). */
	public static final String PREFS_SMS_PERIOD_OUT2 = "sms_period_out2";
	/** Preference's name for saving sms in (all). */
	public static final String PREFS_SMS_ALL_IN = "sms_all_in";
	/** Preference's name for saving sms out (all). */
	public static final String PREFS_SMS_ALL_OUT = "sms_all_out";
	/** Preference's name for billing incoming sms. */
	public static final String PREFS_SMS_BILL_INCOMING = "bill_sms_incoming";

	/** Value for calls out plan #1. */
	private static final int RESULT_CALLS1_VAL = 0;
	/** Limit for calls out plan #1. */
	private static final int RESULT_CALLS1_LIMIT = 1;
	/** Value for calls out plan #2. */
	private static final int RESULT_CALLS2_VAL = 2;
	/** Limit for calls out plan #2. */
	private static final int RESULT_CALLS2_LIMIT = 3;
	/** Calls in plan #1. */
	private static final int RESULT_CALLS1_IN = 4;
	/** Calls in plan #2. */
	private static final int RESULT_CALLS2_IN = 5;
	/** Value for sms out plan #1. */
	private static final int RESULT_SMS1_VAL = 6;
	/** Limit for sms out plan #1. */
	private static final int RESULT_SMS1_LIMIT = 7;
	/** Value for sms out plan #2. */
	private static final int RESULT_SMS2_VAL = 8;
	/** Limit for sms out plan #2. */
	private static final int RESULT_SMS2_LIMIT = 9;
	/** SMS in plan #1. */
	private static final int RESULT_SMS1_IN = 10;
	/** SMS in plan #2. */
	private static final int RESULT_SMS2_IN = 11;

	/** Status Strings. */
	private String callsIn1, callsIn2, callsOut1, callsOut2, callsInOut1,
			callsInOut2, callsBillDate, smsIn1, smsIn2, smsOut1, smsOut2,
			smsInOut1, smsInOut2, smsBillDate;
	/** Status TextViews. */
	private TextView twCallsIn1, twCallsIn2, twCallsOut1, twCallsOut2,
			twCallsBillDate, twCallsPB1Text, twCallsPB2Text, twSMSIn1,
			twSMSIn2, twSMSOut1, twSMSOut2, twSMSBillDate, twSMSPB1Text,
			twSMSPB2Text;
	/** Status ProgressBars. */
	private ProgressBar pbCallsBillDate, pbCalls1, pbCalls2, pbSMSBillDate,
			pbSMS1, pbSMS2;

	/** Update string every.. rounds */
	private static final int UPDATE_INTERVAL = 50;

	/** Merge plans for calls. */
	private boolean plansMergeCalls = false;
	/** Merge plans for sms. */
	private boolean plansMergeSms = false;

	/** Bill excluded calls in this plan, 0 for no plan. */
	private int excludedCallsToPlan = 0;
	/** Bill excluded sms in this plan, 0 for no plan. */
	private int excludedSmsToPlan = 0;

	/** Excluded numbers. */
	// private ExcludedPerson[] excludeNumbers = null;

	/** Sum of displayed calls in/out. Used if merging sms into calls. */
	private int callsInSum, callsOutSum;
	/** Number of calls. */
	private int callsInCount1, callsInCount2, callsOutCount1, callsOutCount2;

	/** Position of {@link ProgressBar}. */
	private int billDatePosCalls, billDatePosSMS;

	/** Length of first billed timeslot. */
	private int lengthOfFirstSlot;
	/** Length of following timeslot. */
	private int lengthOfNextSlots;

	/** Preferences to use. */
	private final SharedPreferences prefs;
	/** Context to use. */
	private final Context context;
	/** Ref to CallMeter instance. */
	private final Plans callmeter;
	/** Run updates on GUI. */
	private final boolean updateGUI;

	/** {@link Currency} symbol. */
	static String currencySymbol = "$";
	/** {@link Currency} fraction digits. */
	static int currencyDigits = 2;

	/** Error message which should be returned to user. */
	private String error = null;

	/**
	 * AsyncTask updating statistics.
	 * 
	 * @param c
	 *            {@link Context}
	 */
	public Updater(final Context c) {
		this.context = c;
		if (c instanceof CallMeter) {
			Log.d(TAG, "running in foreground");
			this.updateGUI = true;
			this.callmeter = (Plans) c;
		} else {
			Log.d(TAG, "running in background");
			this.updateGUI = false;
			this.callmeter = null;
		}
		this.prefs = PreferenceManager.getDefaultSharedPreferences(c);
		try {
			final Currency cur = Currency.getInstance(Locale.getDefault());
			currencySymbol = cur.getSymbol();
			currencyDigits = cur.getDefaultFractionDigits();
		} catch (Exception e) {
			Log.w(TAG, "error getting currency", e);
		}

		if (this.updateGUI) {
			this.pbCallsBillDate = (ProgressBar) this.callmeter
					.findViewById(R.id.calls_progressbar_date);
			this.pbCalls1 = (ProgressBar) this.callmeter
					.findViewById(R.id.calls1_progressbar);
			this.pbCalls2 = (ProgressBar) this.callmeter
					.findViewById(R.id.calls2_progressbar);
			this.pbSMSBillDate = (ProgressBar) this.callmeter
					.findViewById(R.id.sms_progressbar_date);
			this.pbSMS1 = (ProgressBar) this.callmeter
					.findViewById(R.id.sms1_progressbar);
			this.pbSMS2 = (ProgressBar) this.callmeter
					.findViewById(R.id.sms2_progressbar);
			this.twCallsIn1 = (TextView) this.callmeter
					.findViewById(R.id.calls1_in);
			this.twCallsIn2 = (TextView) this.callmeter
					.findViewById(R.id.calls2_in);
			this.twCallsOut1 = (TextView) this.callmeter
					.findViewById(R.id.calls1_out);
			this.twCallsOut2 = (TextView) this.callmeter
					.findViewById(R.id.calls2_out);
			this.twCallsBillDate = (TextView) this.callmeter
					.findViewById(R.id.calls_billdate);
			this.twCallsPB1Text = (TextView) this.callmeter
					.findViewById(R.id.calls1_progressbar_text);
			this.twCallsPB2Text = (TextView) this.callmeter
					.findViewById(R.id.calls2_progressbar_text);
			this.twSMSIn1 = (TextView) this.callmeter
					.findViewById(R.id.sms1_in);
			this.twSMSIn2 = (TextView) this.callmeter
					.findViewById(R.id.sms2_in);
			this.twSMSOut1 = (TextView) this.callmeter
					.findViewById(R.id.sms1_out);
			this.twSMSOut2 = (TextView) this.callmeter
					.findViewById(R.id.sms2_out);
			this.twSMSBillDate = (TextView) this.callmeter
					.findViewById(R.id.sms_billdate);
			this.twSMSPB1Text = (TextView) this.callmeter
					.findViewById(R.id.sms1_progressbar_text);
			this.twSMSPB2Text = (TextView) this.callmeter
					.findViewById(R.id.sms2_progressbar_text);
		}
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
				.parseInt(prefs.getString(PREFS_BILLDAY, "1")));
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
					PREFS_SMSBILLDAY, "1")));
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
		Log.d(TAG, "last check calls: " + lastCheck);
		Log.d(TAG, "lastBill: " + lastBill);

		final Editor editor = prefs.edit();
		if (lastCheck < lastBill) {
			editor.remove(PREFS_CALLS_PERIOD_IN1);
			editor.remove(PREFS_CALLS_PERIOD_IN1_COUNT);
			editor.remove(PREFS_CALLS_PERIOD_IN2);
			editor.remove(PREFS_CALLS_PERIOD_IN2_COUNT);
			editor.remove(PREFS_CALLS_PERIOD_OUT1);
			editor.remove(PREFS_CALLS_PERIOD_OUT1_COUNT);
			editor.remove(PREFS_CALLS_PERIOD_OUT2);
			editor.remove(PREFS_CALLS_PERIOD_OUT2_COUNT);
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
			editor.remove(PREFS_SMS_PERIOD_IN1);
			editor.remove(PREFS_SMS_PERIOD_IN2);
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
		this.twCallsIn1.setText(this.callsIn1);
		this.twCallsIn2.setText(this.callsIn2);
		this.twCallsOut1.setText(this.callsOut1);
		this.twCallsOut2.setText(this.callsOut2);
		this.twCallsPB1Text.setText(this.callsInOut1);
		this.twCallsPB2Text.setText(this.callsInOut2);

		this.twSMSBillDate.setText(this.smsBillDate);
		this.twSMSIn1.setText(this.smsIn1);
		this.twSMSIn2.setText(this.smsIn2);
		this.twSMSOut1.setText(this.smsOut1);
		this.twSMSOut2.setText(this.smsOut2);
		this.twSMSPB1Text.setText(this.smsInOut1);
		this.twSMSPB2Text.setText(this.smsInOut2);

		this.pbCallsBillDate.setProgress(this.billDatePosCalls);
		this.pbSMSBillDate.setProgress(this.billDatePosSMS);
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
			if (this.plansMergeCalls) {
				this.excludedCallsToPlan = 0;
			} else {
				// if (this.prefs.getBoolean(ExcludePeople.// .
				// PREFS_EXCLUDE_PEOPLE_CALLS_PLAN1, false)) {
				// this.excludedCallsToPlan = 1;
				// } else if (this.prefs.getBoolean(ExcludePeople.// .
				// PREFS_EXCLUDE_PEOPLE_CALLS_PLAN2, false)) {
				// this.excludedCallsToPlan = 2;
				// }
			}
			if (this.plansMergeSms) {
				this.excludedSmsToPlan = 0;
			} else {
				// if (this.prefs.getBoolean(
				// ExcludePeople.PREFS_EXCLUDE_PEOPLE_SMS_PLAN1, false)) {
				// this.excludedSmsToPlan = 1;
				// } else if (this.prefs.getBoolean(
				// ExcludePeople.PREFS_EXCLUDE_PEOPLE_SMS_PLAN2, false)) {
				// this.excludedSmsToPlan = 2;
				// }
			}
		} else {
			this.plansMergeCalls = true;
			this.plansMergeSms = true;
			this.excludedCallsToPlan = 0;
			this.excludedSmsToPlan = 0;
		}
		// if (!this.prefs.getBoolean(
		// ExcludePeople.PREFS_EXCLUDE_PEOPLE_CALLS_ENABLE, true)) {
		// this.excludedCallsToPlan = -1;
		// }
		// if (!this.prefs.getBoolean(
		// ExcludePeople.PREFS_EXCLUDE_PEOPLE_SMS_ENABLE, false)) {
		// this.excludedSmsToPlan = -1;
		// }
	}

	/**
	 * Init status text.
	 */
	private void initStatusText() {
		this.callsBillDate = "?";
		this.callsIn1 = "?";
		this.callsIn2 = "?";
		this.callsOut1 = "?";
		this.callsOut2 = "?";
		this.smsBillDate = "?";
		this.smsIn1 = "?";
		this.smsIn2 = "?";
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
			final String stringInCalls = this.context
					.getString(R.string.in_calls);
			final String stringInSMS = this.context.getString(R.string.in_sms);

			this.pbCalls1.setProgress(0);
			this.pbCalls1.setIndeterminate(false);
			int v = setVisableIfSet(this.prefs.getString(PREFS_PLAN1_FREEMIN,
					null), this.prefs.getBoolean(PREFS_PLAN1_T_FREE_CALLS,
					false));
			this.pbCalls1.setVisibility(v);
			if (this.plansMergeCalls) {
				this.callmeter.findViewById(R.id.calls2_view).setVisibility(
						View.GONE);
				((TextView) this.callmeter.findViewById(R.id.calls1_in_))
						.setText(String.format(stringInCalls, ""));
				((TextView) this.callmeter.findViewById(R.id.calls1_out_))
						.setText(String.format(this.context
								.getString(R.string.out_calls), ""));
			} else {
				((TextView) this.callmeter.findViewById(R.id.calls1_in_))
						.setText(String.format(stringInCalls, namePlan1));
				((TextView) this.callmeter.findViewById(R.id.calls2_in_))
						.setText(String.format(stringInCalls, namePlan2));

				String s = this.context.getString(R.string.out_calls);
				((TextView) this.callmeter.findViewById(R.id.calls1_out_))
						.setText(String.format(s, namePlan1));
				((TextView) this.callmeter.findViewById(R.id.calls2_out_))
						.setText(String.format(s, namePlan2));

				this.pbCalls2.setProgress(0);
				this.pbCalls2.setIndeterminate(false);
				v = setVisableIfSet(this.prefs.getString(PREFS_PLAN2_FREEMIN,
						null), this.prefs.getBoolean(PREFS_PLAN2_T_FREE_CALLS,
						false));
				this.pbCalls2.setVisibility(v);
				this.callmeter.findViewById(R.id.calls2_view).setVisibility(
						View.VISIBLE);
			}
			this.pbSMS1.setProgress(0);
			this.pbSMS1.setIndeterminate(false);
			v = setVisableIfSet(this.prefs.getString(PREFS_PLAN1_FREESMS, // .
					null), this.prefs.getBoolean(PREFS_PLAN1_T_FREE_SMS, // .
					false));
			this.pbSMS1.setVisibility(v);
			if (this.plansMergeSms) {
				this.callmeter.findViewById(R.id.sms2_view).setVisibility(
						View.GONE);
				((TextView) this.callmeter.findViewById(R.id.sms1_in_))
						.setText(String.format(stringInSMS, ""));
				((TextView) this.callmeter.findViewById(R.id.sms1_out_))
						.setText(String.format(this.context
								.getString(R.string.out_sms), ""));
			} else {
				((TextView) this.callmeter.findViewById(R.id.sms1_in_))
						.setText(String.format(stringInSMS, namePlan1));
				((TextView) this.callmeter.findViewById(R.id.sms2_in_))
						.setText(String.format(stringInSMS, namePlan2));

				String s = this.context.getString(R.string.out_sms);
				((TextView) this.callmeter.findViewById(R.id.sms1_out_))
						.setText(String.format(s, namePlan1));
				((TextView) this.callmeter.findViewById(R.id.sms2_out_))
						.setText(String.format(s, namePlan2));

				this.pbSMS2.setProgress(0);
				this.pbSMS2.setIndeterminate(false);
				v = setVisableIfSet(this.prefs.getString(PREFS_PLAN2_FREESMS,
						null), this.prefs.getBoolean(PREFS_PLAN2_T_FREE_SMS,
						false));
				this.pbSMS2.setVisibility(v);
				this.callmeter.findViewById(R.id.sms2_view).setVisibility(
						View.VISIBLE);
			}

			v = View.GONE;
			if (!this.prefs.getBoolean(PREFS_MERGE_SMS_TO_CALLS, false)) {
				v = View.VISIBLE;

				int v1 = View.VISIBLE;
				if (!this.prefs.getBoolean(PREFS_SMS_BILL_INCOMING, false)) {
					v1 = View.GONE;
				}
				TextView tw = this.twSMSPB1Text;
				tw.setText("");
				tw.setVisibility(v1);
				tw = this.twSMSPB2Text;
				tw.setText("");
				tw.setVisibility(v1);

			} else {
				this.twSMSPB1Text.setVisibility(View.GONE);
				this.twSMSPB2Text.setVisibility(View.GONE);
				this.pbSMS1.setVisibility(View.GONE);
				this.callmeter.findViewById(R.id.sms2_view).setVisibility(
						View.GONE);
			}
			this.pbSMSBillDate.setVisibility(v);
			this.callmeter.findViewById(R.id.sms_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms_billdate_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms_billdate).setVisibility(v);
			this.callmeter.findViewById(R.id.sms1_out_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms1_out).setVisibility(v);
			this.callmeter.findViewById(R.id.sms1_in_).setVisibility(v);
			this.callmeter.findViewById(R.id.sms1_in).setVisibility(v);

			v = View.VISIBLE;
			if (!this.prefs.getBoolean(PREFS_CALLS_BILL_INCOMING, false)) {
				v = View.GONE;
			}
			TextView tw = this.twCallsPB1Text;
			tw.setText("");
			tw.setVisibility(v);
			tw = this.twCallsPB2Text;
			tw.setText("");
			tw.setVisibility(v);

			// common
			this.initStatusText();
			this.updateText();
		}
	}

	/**
	 * Check, in which plan the call/message should be billed.
	 * 
	 * @param cur
	 *            {@link Cursor}
	 * @param idNumber
	 *            id of number in cursor
	 * @param plans
	 *            plans
	 * @param toPlan
	 *            bill excluded numbers in this plan, 0 for no plan.
	 * @param date
	 *            timestamp
	 * @return 0: no billing, 1: plan1, 2: plan2
	 */
	private int getPlan(final Cursor cur, final int idNumber,
			final boolean[][] plans, final int toPlan, final long date) {
		return 0;
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
		if (this.updateGUI) {
			this.billDatePosCalls = getDatePos(calBillDate);
		}

		long billDate = calBillDate.getTimeInMillis();

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
		int durIn1Month = this.prefs.getInt(PREFS_CALLS_PERIOD_IN1, 0);
		int durIn2Month = this.prefs.getInt(PREFS_CALLS_PERIOD_IN2, 0);
		int durOut1Month = this.prefs.getInt(PREFS_CALLS_PERIOD_OUT1, 0);
		int durOut2Month = this.prefs.getInt(PREFS_CALLS_PERIOD_OUT2, 0);
		int countIn1Month = this.prefs.getInt(PREFS_CALLS_PERIOD_IN1_COUNT, 0);
		int countIn2Month = this.prefs.getInt(PREFS_CALLS_PERIOD_IN2_COUNT, 0);
		int countOut1Month = this.prefs
				.getInt(PREFS_CALLS_PERIOD_OUT1_COUNT, 0);
		int countOut2Month = this.prefs
				.getInt(PREFS_CALLS_PERIOD_OUT2_COUNT, 0);

		int free1 = 0; // -1 -> totally free
		int free2 = 0;
		if (this.prefs.getBoolean(PREFS_PLAN1_T_FREE_CALLS, false)) {
			free1 = -1;
		} else {
			try {
				String s = this.prefs.getString(PREFS_PLAN1_FREEMIN, "0");
				if (s.length() > 0) {
					free1 = Integer.parseInt(s);
				}
			} catch (NumberFormatException e) {
				free1 = 0;
				Log.e(TAG, "wrong number format", e);
			}
		}
		if (this.prefs.getBoolean(PREFS_PLAN2_T_FREE_CALLS, false)) {
			free2 = -1;
		} else {
			try {
				String s = this.prefs.getString(PREFS_PLAN2_FREEMIN, "0");
				if (s.length() > 0) {
					free2 = Integer.parseInt(s);
				}
			} catch (NumberFormatException e) {
				free2 = 0;
				Log.e(TAG, "wrong number format", e);
			}
		}
		// walk through log
		if (cur.moveToFirst()) {
			int type;
			long d;
			final int idType = cur.getColumnIndex(Calls.TYPE);
			final int idDuration = cur.getColumnIndex(Calls.DURATION);
			final int idDate = cur.getColumnIndex(Calls.DATE);
			final int idNumber = cur.getColumnIndex(Calls.NUMBER);
			lastWalk = cur.getLong(idDate);
			int t = 0;
			int i = 0;
			int dt = 0;
			int p = 0;
			do {
				type = cur.getInt(idType);
				d = cur.getLong(idDate);
				t = cur.getInt(idDuration);
				Log.d(TAG, "got entry: " + d + ", t: " + t);
				if (t <= 0) {
					Log.d(TAG, "skip");
					continue;
				}
				if (billDate <= d) {
					dt = this.roundTime(t);
					p = this.getPlan(cur, idNumber, plans,
							this.excludedCallsToPlan, d);
				} else {
					p = 0;
				}
				switch (type) {
				case Calls.INCOMING_TYPE:
					durIn += t;
					if (p == 1) {
						++countIn1Month;
						durIn1Month += dt;
					} else if (p == 2) {
						++countIn2Month;
						durIn2Month += dt;
					}
					break;
				case Calls.OUTGOING_TYPE:
					durOut += t;
					if (p == 1) {
						++countOut1Month;
						durOut1Month += dt;
					} else if (p == 2) {
						++countOut2Month;
						durOut2Month += dt;
					}
					break;
				default:
					break;
				}
				++i;
				if (i % UPDATE_INTERVAL == 1) {
					this.callsIn1 = calcString(durIn1Month, 0, durIn, true,
							countIn1Month);
					this.callsIn2 = calcString(durIn2Month, 0, durIn, true,
							countIn2Month);
					this.callsOut1 = calcString(durOut1Month, free1, durOut,
							true, countOut1Month);
					this.callsOut2 = calcString(durOut2Month, free2, durOut,
							true, countOut2Month);
					this.publishProgress((Void) null);
				}
			} while (cur.moveToNext());
		}
		status[RESULT_CALLS1_VAL] = durOut1Month;
		status[RESULT_CALLS1_LIMIT] = free1 * SECONDS_MINUTE;
		status[RESULT_CALLS2_VAL] = durOut2Month;
		status[RESULT_CALLS2_LIMIT] = free2 * SECONDS_MINUTE;
		status[RESULT_CALLS1_IN] = durIn1Month;
		status[RESULT_CALLS2_IN] = durIn2Month;

		if (this.prefs.getBoolean(PREFS_CALLS_BILL_INCOMING, false)) {
			int sum = durIn1Month + durOut1Month;
			status[RESULT_CALLS1_VAL] = sum;
			if (status[RESULT_CALLS1_LIMIT] > 0) {
				this.callsInOut1 = (sum * CallMeter.HUNDRET / // .
						status[RESULT_CALLS1_LIMIT])
						+ "% / ";
			} else {
				this.callsInOut1 = "";
			}
			this.callsInOut1 += getTime(sum) + " ("
					+ (countIn1Month + countOut1Month) + ")";
			free1 = 0;

			sum = durIn2Month + durOut2Month;
			status[RESULT_CALLS2_VAL] = sum;
			if (status[RESULT_CALLS2_LIMIT] > 0) {
				this.callsInOut2 = (sum * CallMeter.HUNDRET / // .
						status[RESULT_CALLS2_LIMIT])
						+ "% / ";
			} else {
				this.callsInOut2 = "";
			}
			this.callsInOut2 += getTime(sum) + " ("
					+ (countIn2Month + countOut2Month) + ")";
			free2 = 0;
		} else {
			this.callsInOut1 = "";
			this.callsInOut2 = "";
		}

		this.callsIn1 = calcString(durIn1Month, 0, durIn, true, countIn1Month);
		this.callsIn2 = calcString(durIn2Month, 0, durIn, true, countIn2Month);
		this.callsOut1 = calcString(durOut1Month, free1, durOut, true,
				countOut1Month);
		this.callsOut2 = calcString(durOut2Month, free2, durOut, true,
				countOut2Month);

		this.publishProgress((Void) null);

		this.callsInSum = durIn;
		this.callsOutSum = durOut;
		this.callsInCount1 = countIn1Month;
		this.callsInCount2 = countIn2Month;
		this.callsOutCount1 = countOut1Month;
		this.callsOutCount2 = countOut2Month;

		Log.d(TAG, "last walk calls: " + lastWalk);
		final Editor editor = this.prefs.edit();
		editor.putInt(PREFS_CALLS_ALL_IN, durIn);
		editor.putInt(PREFS_CALLS_ALL_OUT, durOut);
		editor.putInt(PREFS_CALLS_PERIOD_IN1, durIn1Month);
		editor.putInt(PREFS_CALLS_PERIOD_IN1_COUNT, countIn1Month);
		editor.putInt(PREFS_CALLS_PERIOD_IN2, durIn2Month);
		editor.putInt(PREFS_CALLS_PERIOD_IN2_COUNT, countIn2Month);
		editor.putInt(PREFS_CALLS_PERIOD_OUT1, durOut1Month);
		editor.putInt(PREFS_CALLS_PERIOD_OUT1_COUNT, countOut1Month);
		editor.putInt(PREFS_CALLS_PERIOD_OUT2, durOut2Month);
		editor.putInt(PREFS_CALLS_PERIOD_OUT2_COUNT, countOut2Month);
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
		if (this.updateGUI) {
			this.billDatePosSMS = getDatePos(calBillDate);
		}

		final long billDate = calBillDate.getTimeInMillis();
		final String[] projection = new String[] // .
		{ Calls.TYPE, Calls.DATE, BODY, "address" };

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
			try {
				String s = this.prefs.getString(PREFS_PLAN1_FREESMS, "0");
				if (s.length() > 0) {
					free1 = Integer.parseInt(s);
				}
			} catch (NumberFormatException e) {
				free1 = 0;
				Log.e(TAG, "wrong number format", e);
			}
		}
		if (this.prefs.getBoolean(PREFS_PLAN2_T_FREE_SMS, false)) {
			free2 = -1;
		} else {
			try {
				String s = this.prefs.getString(PREFS_PLAN2_FREESMS, "0");
				if (s.length() > 0) {
					free2 = Integer.parseInt(s);
				}
			} catch (NumberFormatException e) {
				free2 = 0;
				Log.e(TAG, "wrong number format", e);
			}
		}

		int iSMSIn = this.prefs.getInt(PREFS_SMS_ALL_IN, 0);
		int iSMSOut = this.prefs.getInt(PREFS_SMS_ALL_OUT, 0);
		int smsIn1Month = this.prefs.getInt(PREFS_SMS_PERIOD_IN1, 0);
		int smsIn2Month = this.prefs.getInt(PREFS_SMS_PERIOD_IN2, 0);
		int smsOut1Month = this.prefs.getInt(PREFS_SMS_PERIOD_OUT1, 0);
		int smsOut2Month = this.prefs.getInt(PREFS_SMS_PERIOD_OUT2, 0);

		if (cur.moveToFirst()) {
			int type;
			long d;
			final int idType = cur.getColumnIndex(Calls.TYPE);
			final int idDate = cur.getColumnIndex(Calls.DATE);
			final int idBody = cur.getColumnIndex(BODY);
			final int idNumber = cur.getColumnIndex("address");
			String body;
			lastWalk = cur.getLong(idDate);
			int i = 0;
			int p = 0;
			int l = 1;
			do {
				type = cur.getInt(idType);
				d = cur.getLong(idDate);
				Log.d(TAG, "got entry: " + d);
				body = cur.getString(idBody);
				if (body == null) {
					l = 1;
				} else {
					l = WRAPPER.calculateLength(body, false)[0];
				}
				p = this.getPlan(cur, idNumber, plans, this.excludedSmsToPlan,
						d);
				switch (type) {
				case Calls.INCOMING_TYPE:
					iSMSIn += l;
					if (billDate <= d) {
						if (p == 1) {
							smsIn1Month += l;
						} else if (p == 2) {
							smsIn2Month += l;
						}
					}
					break;
				case Calls.OUTGOING_TYPE:
					iSMSOut += l;
					if (billDate <= d) {
						if (p == 1) {
							smsOut1Month += l;
						} else if (p == 2) {
							smsOut2Month += l;
						}
					}
					break;
				default:
					break;
				}
				++i;
				if (i % UPDATE_INTERVAL == 1) {
					this.smsIn1 = calcString(smsIn1Month, 0, iSMSIn, false, 0);
					this.smsIn2 = calcString(smsIn2Month, 0, iSMSIn, false, 0);
					this.smsOut1 = calcString(smsOut2Month, free1, iSMSOut,
							false, 0);
					this.smsOut2 = calcString(smsOut2Month, free2, iSMSOut,
							false, 0);
					this.publishProgress((Void) null);
				}
			} while (cur.moveToNext());
		}

		status[RESULT_SMS1_VAL] = smsOut1Month;
		status[RESULT_SMS1_LIMIT] = free1;
		status[RESULT_SMS2_VAL] = smsOut2Month;
		status[RESULT_SMS2_LIMIT] = free2;
		status[RESULT_SMS1_IN] = smsIn1Month;

		if (this.prefs.getBoolean(PREFS_SMS_BILL_INCOMING, false)) {
			int sum = smsOut1Month + smsIn1Month;
			status[RESULT_SMS1_VAL] = sum;
			if (free1 > 0) {
				this.smsInOut1 = (sum * CallMeter.HUNDRET / free1) + "% / ";
			} else {
				this.smsInOut1 = "";
			}
			this.smsInOut1 += sum;
			free1 = 0;
			sum = smsOut2Month + smsIn2Month;
			status[RESULT_SMS2_VAL] = sum;
			if (free2 > 0) {
				this.smsInOut2 = (sum * CallMeter.HUNDRET / free2) + "% / ";
			} else {
				this.smsInOut2 = "";
			}
			this.smsInOut2 += sum;
			free2 = 0;
		} else {
			this.smsInOut1 = "";
			this.smsInOut2 = "";
		}

		this.smsIn1 = calcString(smsIn1Month, 0, iSMSIn, false, 0);
		this.smsIn2 = calcString(smsIn2Month, 0, iSMSIn, false, 0);
		this.smsOut1 = calcString(smsOut1Month, free1, iSMSOut, false, 0);
		this.smsOut2 = calcString(smsOut2Month, free2, iSMSOut, false, 0);

		if (this.prefs.getBoolean(PREFS_MERGE_SMS_TO_CALLS, false)) {
			// merge sms into calls.
			final boolean mergeToPlan1 = this.plansMergeCalls
					|| this.prefs.getBoolean(PREFS_MERGE_SMS_PLAN1, true);
			final int secondsForSMS = Integer.parseInt(this.prefs.getString(
					PREFS_MERGE_SMS_TO_CALLS_SECONDS, "0"));
			int i = RESULT_CALLS1_VAL; // plan 1 number of seconds
			int j = RESULT_CALLS1_IN;
			int callsInCount = this.callsInCount1;
			int callsOutCount = this.callsOutCount1;
			if (!mergeToPlan1) {
				i = RESULT_CALLS2_VAL; // plan 2 number of seconds
				j = RESULT_CALLS2_IN;
				callsInCount = this.callsInCount2;
				callsOutCount = this.callsOutCount2;
			}
			status[i] += secondsForSMS * smsOut1Month;
			if (this.prefs.getBoolean(PREFS_SMS_BILL_INCOMING, false)) {
				status[j] += secondsForSMS * smsIn1Month;
			}

			status[RESULT_SMS1_VAL] = 0;
			status[RESULT_SMS1_LIMIT] = 0;
			status[RESULT_SMS2_VAL] = 0;
			status[RESULT_SMS2_LIMIT] = 0;

			String ssum;
			int freesum = status[i + 1];
			if (this.prefs.getBoolean(PREFS_CALLS_BILL_INCOMING, false)) {
				int sum = status[i] + status[j];
				if (freesum > 0) {
					ssum = (sum * CallMeter.HUNDRET / freesum) + "% / ";
				} else {
					ssum = "";
				}
				ssum += getTime(sum);
				freesum = 0;
			} else {
				ssum = "";
			}

			final String sin = calcString(status[j], 0, this.callsInSum, true,
					callsInCount);
			final String sout = calcString(status[i], freesum / SECONDS_MINUTE,
					this.callsOutSum, true, callsOutCount);
			if (mergeToPlan1) {
				this.callsIn1 = sin;
				this.callsOut1 = sout;
				this.callsInOut1 = ssum;
			} else {
				this.callsIn2 = sin;
				this.callsOut2 = sout;
				this.callsInOut2 = ssum;
			}
		}

		Log.d(TAG, "last walk sms: " + lastWalk);
		final Editor editor = this.prefs.edit();
		editor.putInt(PREFS_SMS_ALL_IN, iSMSIn);
		editor.putInt(PREFS_SMS_ALL_OUT, iSMSOut);
		editor.putInt(PREFS_SMS_PERIOD_IN1, smsIn1Month);
		editor.putInt(PREFS_SMS_PERIOD_IN2, smsIn2Month);
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
		// progressbar positions
		final Integer[] ret = { 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0 };
		try {
			// load splitted plans
			final boolean[][] plans = this.loadPlans(this.prefs);
			// this.excludeNumbers = ExcludePeople
			// .loadExcludedPeople(this.context).toArray(
			// new ExcludedPerson[1]);

			Calendar calBillDate = getBillDayCalls(this.prefs);
			if (this.plansMergeCalls) {
				this.walkCalls(null, calBillDate, ret);
			} else {
				this.walkCalls(plans, calBillDate, ret);
			}

			// report sms
			calBillDate = getBillDaySMS(this.prefs);
			if (this.plansMergeSms) {
				this.walkSMS(null, calBillDate, ret);
			} else {
				this.walkSMS(plans, calBillDate, ret);
			}
		} catch (Exception e) {
			this.error = "some error occured, "
					+ "please load sendlog from market and "
					+ "send your logs to the developer.";
			Log.e(TAG, "error in background", e);
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
	 * Add cost to strings.
	 * 
	 * @param result
	 *            result
	 */
	private void updateCost(final Integer[] result) {
		// calls
		float costPerCall1 = 0;
		float costPerCall2 = 0;
		float costPerMinute1 = 0;
		float costPerMinute2 = 0;
		if (!this.prefs.getBoolean(PREFS_PLAN1_T_FREE_CALLS, false)) {
			if (result[RESULT_CALLS1_LIMIT] <= 0) {
				final String s = this.prefs.getString(
						PREFS_PLAN1_COST_PER_CALL, "");
				if (s.length() > 0) {
					costPerCall1 = Float.parseFloat(s);
				}
			}
			final String s = this.prefs.getString(PREFS_PLAN1_COST_PER_MINUTE,
					"");
			if (s.length() > 0) {
				costPerMinute1 = Float.parseFloat(s);
			}
		}
		if (!this.prefs.getBoolean(PREFS_PLAN2_T_FREE_CALLS, false)) {
			if (result[RESULT_CALLS2_LIMIT] <= 0) {
				final String s = this.prefs.getString(
						PREFS_PLAN2_COST_PER_CALL, "");
				if (s.length() > 0) {
					costPerCall2 = Float.parseFloat(s);
				}
			}
			final String s = this.prefs.getString(PREFS_PLAN2_COST_PER_MINUTE,
					"");
			if (s.length() > 0) {
				costPerMinute2 = Float.parseFloat(s);
			}
		}
		boolean billIn = this.prefs
				.getBoolean(PREFS_CALLS_BILL_INCOMING, false);
		if (costPerCall1 > 0 || costPerMinute1 > 0) {
			int i;
			int c;
			String s;
			if (billIn) {
				i = result[RESULT_CALLS1_VAL] + result[RESULT_CALLS1_IN];
				c = this.prefs.getInt(PREFS_CALLS_PERIOD_IN1_COUNT, 0);
				s = this.callsInOut1;
			} else {
				i = result[RESULT_CALLS1_VAL];
				s = this.callsOut1;
				c = this.prefs.getInt(PREFS_CALLS_PERIOD_OUT1_COUNT, 0);
			}
			if (result[RESULT_CALLS1_LIMIT] > 0) {
				i -= result[RESULT_CALLS1_LIMIT];
			}
			if (i < 0) {
				i = 0;
			}
			float cost = ((costPerMinute1 * i) / SECONDS_MINUTE)
					+ (costPerCall1 * c);
			s = String.format("%." + currencyDigits + "f", cost)
					+ currencySymbol + " | " + s;
			if (billIn) {
				this.callsInOut1 = s;
			} else {
				this.callsOut1 = s;
			}
		}

		if (costPerCall2 > 0 || costPerMinute2 > 0) {
			int i;
			int c;
			String s;
			if (billIn) {
				i = result[RESULT_CALLS2_VAL] + result[RESULT_CALLS2_IN];
				c = this.prefs.getInt(PREFS_CALLS_PERIOD_IN2_COUNT, 0);
				s = this.callsInOut2;
			} else {
				i = result[RESULT_CALLS2_VAL];
				s = this.callsOut2;
				c = this.prefs.getInt(PREFS_CALLS_PERIOD_OUT2_COUNT, 0);
			}
			if (result[RESULT_CALLS2_LIMIT] > 0) {
				i -= result[RESULT_CALLS2_LIMIT];
			}
			if (i < 0) {
				i = 0;
			}
			float cost = ((costPerMinute2 * i) / SECONDS_MINUTE)
					+ (costPerCall2 * c);
			s = String.format("%." + currencyDigits + "f", cost)
					+ currencySymbol + " | " + s;
			if (billIn) {
				this.callsInOut2 = s;
			} else {
				this.callsOut2 = s;
			}
		}

		// sms
		float costPerSMS1 = 0;
		float costPerSMS2 = 0;
		if (!this.prefs.getBoolean(PREFS_PLAN1_T_FREE_SMS, false)) {
			final String s = this.prefs.getString(PREFS_PLAN1_COST_PER_SMS, "");
			if (s.length() > 0) {
				costPerSMS1 = Float.parseFloat(s);
			}
		}
		if (!this.prefs.getBoolean(PREFS_PLAN2_T_FREE_SMS, false)) {
			final String s = this.prefs.getString(PREFS_PLAN2_COST_PER_SMS, "");
			if (s.length() > 0) {
				costPerSMS2 = Float.parseFloat(s);
			}
		}
		billIn = this.prefs.getBoolean(PREFS_SMS_BILL_INCOMING, false);
		if (costPerSMS1 > 0) {
			int i;
			String s;
			if (billIn) {
				i = result[RESULT_SMS1_VAL] + result[RESULT_SMS1_IN];
				s = this.smsInOut1;
			} else {
				i = result[RESULT_SMS1_VAL];
				s = this.smsOut1;
			}
			if (result[RESULT_SMS1_LIMIT] > 0) {
				i -= result[RESULT_SMS1_LIMIT];
			}
			if (i < 0) {
				i = 0;
			}
			final float cost = costPerSMS1 * i;
			s = String.format("%." + currencyDigits + "f", cost)
					+ currencySymbol + " | " + s;
			if (billIn) {
				this.smsInOut1 = s;
			} else {
				this.smsOut1 = s;
			}
		}
		if (costPerSMS2 > 0) {
			int i;
			String s;
			if (billIn) {
				i = result[RESULT_SMS2_VAL] + result[RESULT_SMS2_IN];
				s = this.smsInOut2;
			} else {
				i = result[RESULT_SMS2_VAL];
				s = this.smsOut2;
			}
			if (result[RESULT_SMS2_LIMIT] > 0) {
				i -= result[RESULT_SMS2_LIMIT];
			}
			if (i < 0) {
				i = 0;
			}
			final float cost = costPerSMS2 * i;
			s = String.format("%." + currencyDigits + "f", cost)
					+ currencySymbol + " | " + s;
			if (billIn) {
				this.smsInOut2 = s;
			} else {
				this.smsOut2 = s;
			}
		}
	}

	/**
	 * Push data back to GUI. Hide {@link ProgressBar}s.
	 * 
	 * @param result
	 *            result
	 */
	@Override
	protected final void onPostExecute(final Integer[] result) {
		if (this.error != null) {
			Toast.makeText(this.context, this.error, Toast.LENGTH_SHORT).show();
		}
		if (this.updateGUI) {
			this.updateCost(result);
			this.updateText();

			// calls
			ProgressBar pb = this.pbCalls1;
			if (result[RESULT_CALLS1_LIMIT] > 0) {
				pb.setMax(result[1]);
				if (result[RESULT_CALLS1_LIMIT] > result[RESULT_CALLS1_VAL]) {
					pb.setProgress(result[RESULT_CALLS1_LIMIT]);
				} else {
					pb.setProgress(result[RESULT_CALLS1_VAL]);
				}
				pb.setProgress(result[RESULT_CALLS1_VAL]);
				pb.setVisibility(View.VISIBLE);
			} else {
				pb.setVisibility(View.GONE);
			}
			pb = this.pbCalls2;
			if (this.plansMergeCalls || result[RESULT_CALLS2_LIMIT] <= 0) {
				pb.setVisibility(View.GONE);
			} else {
				pb.setMax(result[RESULT_CALLS2_LIMIT]);
				if (result[RESULT_CALLS2_LIMIT] > result[RESULT_CALLS2_VAL]) {
					pb.setProgress(result[RESULT_CALLS2_LIMIT]);
				} else {
					pb.setProgress(result[RESULT_CALLS2_VAL]);
				}
				pb.setProgress(result[RESULT_CALLS2_VAL]);
				pb.setVisibility(View.VISIBLE);
			}

			// sms
			pb = this.pbSMS1;
			if (result[RESULT_SMS1_LIMIT] > 0) {
				pb.setMax(result[RESULT_SMS1_LIMIT]);
				if (result[RESULT_SMS1_VAL] > result[RESULT_SMS1_LIMIT]) {
					pb.setProgress(result[RESULT_SMS1_LIMIT]);
				} else {
					pb.setProgress(result[RESULT_SMS1_VAL]);
				}
				pb.setVisibility(View.VISIBLE);
			} else {
				pb.setVisibility(View.GONE);
			}
			pb = this.pbSMS2;
			if (this.plansMergeSms || result[RESULT_SMS2_LIMIT] <= 0) {
				pb.setVisibility(View.GONE);
			} else {
				pb.setMax(result[RESULT_SMS2_LIMIT]);
				if (result[RESULT_SMS2_VAL] > result[RESULT_SMS2_LIMIT]) {
					pb.setProgress(result[RESULT_SMS2_LIMIT]);
				} else {
					pb.setProgress(result[RESULT_SMS2_VAL]);
				}
				pb.setVisibility(View.VISIBLE);
			}
		}

		if (this.updateGUI) {
			// FIXME
			((Plans) this.context).setProgressBarIndeterminateVisibility(false);
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
	 * @param count
	 *            number (of calls)
	 * @return String holding all the data
	 */
	private static String calcString(final int thisPeriod, final int limit,
			final int all, final boolean calls, final int count) {
		String c = "";
		if (calls && count >= 0) {
			c = " (" + count + ")";
		}
		if (limit > 0) {
			if (calls) {
				return ((thisPeriod * CallMeter.HUNDRET) / // .
						(limit * SECONDS_MINUTE))
						+ "% | " + getTime(thisPeriod) + c
						+ " | "
						+ getTime(all);
			} else {
				return ((thisPeriod * CallMeter.HUNDRET) / limit) + "% | "
						+ thisPeriod + " | " + all;
			}
		} else {
			if (calls) {
				return getTime(thisPeriod) + c + " | " + getTime(all);
			} else {
				return thisPeriod + " | " + all;
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

	/**
	 * @param s
	 *            {@link String}
	 * @param hide
	 *            force hide
	 * @return View.GONE if s is null, empty or "0", else View.VISABLE
	 */
	static int setVisableIfSet(final String s, final boolean hide) {
		if (hide || s == null || s.length() == 0 || s.equals("0")) {
			return View.GONE;
		} else {
			return View.VISIBLE;
		}
	}

	/**
	 * Get a {@link ProgressBar} position.
	 * 
	 * @param cal
	 *            {@link Calendar}
	 * @return position
	 */
	public static int getDatePos(final Calendar cal) {
		Calendar c = (Calendar) cal.clone();
		long bll = c.getTimeInMillis();
		long now = System.currentTimeMillis() - bll;
		c.add(Calendar.MONTH, 1);
		long nxt = c.getTimeInMillis() - bll;
		return (int) (now * 100 / nxt);
	}
}
