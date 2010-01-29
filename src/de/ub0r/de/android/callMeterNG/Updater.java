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
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.gsm.SmsMessage;
import android.text.format.DateFormat;
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
	// private static final String TAG = "CallMeterNG.updater";

	/** Days of a week. */
	static final int DAYS_WEEK = 7;
	/** Hours of a day. */
	static final int HOURS_DAY = 24;
	/** Seconds of a minute. */
	static final int SECONDS_MINUTE = 60;

	/** Status Strings. */
	private String callsIn, callsOut1, callsOut2, callsBillDate, smsIn,
			smsOut1, smsOut2, smsBillDate;
	/** Status TextViews. */
	private TextView twCallsIn, twCallsOut1, twCallsOut2, twCallsBillDate,
			twSMSIn, twSMSOut1, twSMSOut2, twSMSBillDate;
	/** Status ProgressBars. */
	private ProgressBar pbCalls1, pbCalls2, pbSMS1, pbSMS2;

	/** Sum of old calls/sms loaded/saved from preferences. */
	private int allCallsIn, allCallsOut, allSMSIn, allSMSOut;
	/** Date of when calls/sms are "old". */
	private long allOldDate = 0;

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
			this.updateGUI = true;
			this.callmeter = (CallMeter) c;
		} else {
			this.updateGUI = false;
			this.callmeter = null;
		}
		this.prefs = PreferenceManager.getDefaultSharedPreferences(c);
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
				ret[day][hour] = p.getBoolean(
						CallMeter.PREFS_PLAN1_HOURS_PREFIX + (day + 1) + "_"
								+ hour, false);
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
	 * Reset saved data.
	 */
	private void resetSavedDate() {
		this.allCallsIn = 0;
		this.allCallsOut = 0;
		this.allSMSIn = 0;
		this.allSMSOut = 0;
		this.allOldDate = 0;
		ResetData.resetData(this.context);
	}

	/**
	 * Load old values from database.
	 */
	private void getOldData() {
		if (this.allOldDate < System.currentTimeMillis()) {
			this.allCallsIn = this.prefs
					.getInt(CallMeter.PREFS_ALL_CALLS_IN, 0);
			this.allCallsOut = this.prefs.getInt(CallMeter.PREFS_ALL_CALLS_OUT,
					0);
			this.allSMSIn = this.prefs.getInt(CallMeter.PREFS_ALL_SMS_IN, 0);
			this.allSMSOut = this.prefs.getInt(CallMeter.PREFS_ALL_SMS_OUT, 0);
			this.allOldDate = this.prefs.getLong(CallMeter.PREFS_DATE_OLD, 0);
		} else { // fix bad results from "happy new 2010"
			this.resetSavedDate();
		}
	}

	/**
	 * get prefs for splitting/merging plans.
	 */
	private void getSplitMergePrefs() {
		if (this.prefs.getBoolean(CallMeter.PREFS_SPLIT_PLANS, false)) {
			this.plansMergeCalls = this.prefs.getBoolean(
					CallMeter.PREFS_MERGE_PLANS_CALLS, false);
			this.plansMergeSms = this.prefs.getBoolean(
					CallMeter.PREFS_MERGE_PLANS_SMS, false);
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

			namePlan1 = this.prefs.getString(CallMeter.PREFS_NAME_PLAN1, "");
			if (namePlan1.length() <= 0) {
				namePlan1 = this.context.getString(R.string.plan1);
			}
			namePlan2 = this.prefs.getString(CallMeter.PREFS_NAME_PLAN2, "");
			if (namePlan2.length() <= 0) {
				namePlan2 = this.context.getString(R.string.plan2);
			}
			namePlan1 = " (" + namePlan1 + ")";
			namePlan2 = " (" + namePlan2 + ")";
		}

		this.getSplitMergePrefs();
		this.getOldData();

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
			if (this.prefs
					.getBoolean(CallMeter.PREFS_MERGE_SMS_TO_CALLS, false)) {
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
	 * @param oldDate
	 *            old date
	 * @param status
	 *            status to return
	 */
	private void walkCalls(final boolean[][] plans, final Calendar calBillDate,
			final long oldDate, final Integer[] status) {

		this.callsBillDate = DateFormat.getDateFormat(this.context).format(
				calBillDate.getTime());
		long billDate = calBillDate.getTimeInMillis();

		final String[] excludeNumbers = CallMeter.prefsExcludePeople
				.toArray(new String[1]);
		final int excludeNumbersSize = excludeNumbers.length;

		// report calls
		String[] projection = new String[] { Calls.TYPE, Calls.DURATION,
				Calls.DATE, Calls.NUMBER };

		Cursor cur = this.context.getContentResolver().query(Calls.CONTENT_URI,
				projection, Calls.DATE + " >= " + this.allOldDate, null,
				Calls.DATE + " DESC");

		String prefBillMode = this.prefs.getString(CallMeter.PREFS_BILLMODE,
				CallMeter.BILLMODE_1_1);
		String[] prefTimeSlots = prefBillMode.split("_");
		this.lengthOfFirstSlot = Integer.parseInt(prefTimeSlots[0]);
		this.lengthOfNextSlots = Integer.parseInt(prefTimeSlots[1]);
		prefBillMode = null;
		prefTimeSlots = null;

		int durIn = this.allCallsIn;
		int durOut = this.allCallsOut;
		int durInMonth = 0;
		int durOut1Month = 0;
		int durOut2Month = 0;
		int free1 = 0; // -1 -> totally free
		int free2 = 0;
		if (this.prefs.getBoolean(CallMeter.PREFS_PLAN1_T_FREE_CALLS, false)) {
			free1 = -1;
		} else {
			String s = this.prefs.getString(CallMeter.PREFS_PLAN1_FREEMIN, "0");
			if (s.length() > 0) {
				free1 = Integer.parseInt(s);
			}
		}
		if (this.prefs.getBoolean(CallMeter.PREFS_PLAN2_T_FREE_CALLS, false)) {
			free2 = -1;
		} else {
			String s = this.prefs.getString(CallMeter.PREFS_PLAN2_FREEMIN, "0");
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
			int t = 0;
			int i = 0;
			boolean p = true;
			boolean check = true;
			do {
				type = cur.getInt(idType);
				d = cur.getLong(idDate);
				switch (type) {
				case Calls.INCOMING_TYPE:
					t = cur.getInt(idDuration);
					durIn += t;
					if (billDate <= d) {
						durInMonth += this.roundTime(t);
					} else if (d < oldDate) {
						this.allCallsIn += t;
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
					} else if (d < oldDate) {
						this.allCallsOut += t;
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

		status[0] = durOut1Month;
		status[1] = free1 * SECONDS_MINUTE;
		status[2] = durOut2Month;
		status[3] = free2 * SECONDS_MINUTE;
		this.publishProgress((Void) null);

		this.callsOutSum = durOut;
	}

	/**
	 * Walk sms.
	 * 
	 * @param plans
	 *            array of plans
	 * @param calBillDate
	 *            Date of billdate
	 * @param oldDate
	 *            old date
	 * @param status
	 *            status to return
	 */
	private void walkSMS(final boolean[][] plans, final Calendar calBillDate,
			final long oldDate, final Integer[] status) {
		// report basics
		this.smsBillDate = DateFormat.getDateFormat(this.context).format(
				calBillDate.getTime());
		final long billDate = calBillDate.getTimeInMillis();
		final String[] projection = new String[] { Calls.TYPE, Calls.DATE,
				CallMeter.BODY };
		final Cursor cur = this.context.getContentResolver().query(
				Uri.parse("content://sms"), projection,
				Calls.DATE + " >= " + this.allOldDate, null,
				Calls.DATE + " DESC");
		int free1 = 0;
		int free2 = 0;
		if (this.prefs.getBoolean(CallMeter.PREFS_PLAN1_T_FREE_SMS, false)) {
			free1 = -1;
		} else {
			String s = this.prefs.getString(CallMeter.PREFS_PLAN1_FREESMS, "0");
			if (s.length() > 0) {
				free1 = Integer.parseInt(s);
			}
		}
		if (this.prefs.getBoolean(CallMeter.PREFS_PLAN2_T_FREE_SMS, false)) {
			free2 = -1;
		} else {
			String s = this.prefs.getString(CallMeter.PREFS_PLAN2_FREESMS, "0");
			if (s.length() > 0) {
				free2 = Integer.parseInt(s);
			}
		}

		int iSMSIn = this.allSMSIn;
		int iSMSOut = this.allSMSOut;
		int smsInMonth = 0;
		int smsOut1Month = 0;
		int smsOut2Month = 0;
		boolean p = true;
		if (cur.moveToFirst()) {
			int type;
			long d;
			final int idType = cur.getColumnIndex(Calls.TYPE);
			final int idDate = cur.getColumnIndex(Calls.DATE);
			final int idBody = cur.getColumnIndex(CallMeter.BODY);
			int i = 0;
			int l = 1;
			do {
				type = cur.getInt(idType);
				d = cur.getLong(idDate);
				l = SmsMessage.calculateLength(cur.getString(idBody), false)[0];
				switch (type) {
				case Calls.INCOMING_TYPE:
					iSMSIn += l;
					if (billDate <= d) {
						smsInMonth += l;
					} else if (d < oldDate) {
						this.allSMSIn += l;
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
					} else if (d < oldDate) {
						this.allSMSOut += l;
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

		status[4] = smsOut1Month;
		status[5] = free1;
		status[6] = smsOut2Month;
		status[7] = free2;

		if (this.prefs.getBoolean(CallMeter.PREFS_MERGE_SMS_TO_CALLS, false)) {
			// merge sms into calls.
			final boolean mergeToPlan1 = this.plansMergeCalls
					|| this.prefs.getBoolean(CallMeter.PREFS_MERGE_SMS_PLAN1,
							true);
			final int secondsForSMS = Integer.parseInt(this.prefs.getString(
					CallMeter.PREFS_MERGE_SMS_TO_CALLS_SECONDS, "0"));
			int i = 0; // plan 1 number of seconds
			if (!mergeToPlan1) {
				i = 2; // plan 2 number of seconds
			}
			status[i] += secondsForSMS * smsOut1Month;

			status[4] = 0;
			status[5] = 0;
			status[6] = 0;
			status[7] = 0;

			final String s = calcString(status[i], status[i + 1],
					this.callsOutSum, false); // false ->
			// no multiply 60s/min
			if (mergeToPlan1) {
				this.callsOut1 = s;
			} else {
				this.callsOut2 = s;
			}
		}
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
		final long oldDate = getOldDate();

		// progressbar positions: calls1_pos, calls1_max, calls2_*, sms*,
		final Integer[] ret = { 0, 0, 0, 0, 1, 1, 1, 1 };
		Calendar calBillDate = getBillDate(Integer.parseInt(this.prefs
				.getString(CallMeter.PREFS_BILLDAY, "0")));
		if (this.plansMergeCalls) {
			this.walkCalls(null, calBillDate, oldDate, ret);
		} else {
			this.walkCalls(plans, calBillDate, oldDate, ret);
		}

		// report sms
		if (!this.prefs.getBoolean(CallMeter.PREFS_SMSPERIOD, false)) {
			calBillDate = getBillDate(Integer.parseInt(this.prefs.getString(
					CallMeter.PREFS_SMSBILLDAY, "0")));
		}
		if (this.plansMergeSms) {
			this.walkSMS(null, calBillDate, oldDate, ret);
		} else {
			this.walkSMS(plans, calBillDate, oldDate, ret);
		}

		this.allOldDate = oldDate;

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
			if (result[1] > 0) {
				pb1.setMax(result[1]);
				if (result[1] > result[0]) {
					pb1.setProgress(result[1]);
				} else {
					pb1.setProgress(result[0]);
				}
				pb1.setProgress(result[0]);
				pb1.setVisibility(View.VISIBLE);
			} else {
				pb2.setVisibility(View.GONE);
				pb2 = this.pbCalls1;
			}
			if (this.plansMergeCalls || result[3] <= 0) {
				pb2.setVisibility(View.GONE);
			} else {
				pb2.setMax(result[3]);
				if (result[3] > result[2]) {
					pb2.setProgress(result[3]);
				} else {
					pb2.setProgress(result[2]);
				}
				pb2.setProgress(result[2]);
				pb2.setVisibility(View.VISIBLE);
			}

			// sms
			pb1 = this.pbSMS1;
			pb2 = this.pbSMS2;
			if (result[5] > 0) {
				pb1.setMax(result[5]);
				if (result[4] > result[5]) {
					pb1.setProgress(result[5]);
				} else {
					pb1.setProgress(result[4]);
				}
				pb1.setVisibility(View.VISIBLE);
			} else {
				pb2.setVisibility(View.GONE);
				pb2 = this.pbSMS1;
			}
			if (this.plansMergeSms || result[7] <= 0) {
				pb2.setVisibility(View.GONE);
			} else {
				pb2.setMax(result[7]);
				if (result[6] > result[7]) {
					pb2.setProgress(result[7]);
				} else {
					pb2.setProgress(result[6]);
				}
				pb2.setVisibility(View.VISIBLE);
			}
		}

		// save old values to database
		SharedPreferences.Editor editor = this.prefs.edit();
		editor.putInt(CallMeter.PREFS_ALL_CALLS_IN, this.allCallsIn);
		editor.putInt(CallMeter.PREFS_ALL_CALLS_OUT, this.allCallsOut);
		editor.putInt(CallMeter.PREFS_ALL_SMS_IN, this.allSMSIn);
		editor.putInt(CallMeter.PREFS_ALL_SMS_OUT, this.allSMSOut);
		editor.putLong(CallMeter.PREFS_DATE_OLD, this.allOldDate);
		editor.commit();

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
				return ((thisPeriod * 100) / (limit * SECONDS_MINUTE)) + "% / "
						+ getTime(thisPeriod) + " / " + getTime(all);
			} else {
				return ((thisPeriod * 100) / limit) + "% / " + thisPeriod
						+ " / " + all;
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
	 * Return "old" date as timestamp.
	 * 
	 * @return date before all calls/sms are "old"
	 */
	private static long getOldDate() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis();
	}

	/**
	 * Return Billdate as Calendar for a given day of month.
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
