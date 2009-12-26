/*
 * Copyright (C) 2009 Felix Bechstein
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.telephony.gsm.SmsMessage;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.admob.android.ads.AdView;

/**
 * The main Activity, holding all data.
 * 
 * @author flx
 */
public class CallMeter extends Activity {
	/** Tag for output. */
	private static final String TAG = "CallMeterNG";
	/** Dialog: donate. */
	private static final int DIALOG_DONATE = 0;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 1;
	/** Dialog: update. */
	private static final int DIALOG_UPDATE = 2;

	/** Days of a week. */
	private static final int DAYS_WEEK = 7;
	/** Hours of a day. */
	private static final int HOURS_DAY = 24;
	/** Seconds of a minute. */
	private static final int SECONDS_MINUTE = 60;
	/** Bytes per Megabyte. */
	static final int BYTES_MEGABYTE = 1024 * 1024;

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";
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
	private static final String PREFS_PLAN1_T_FREE_CALLS = "plan1_total_free_calls";
	/** Prefs: plan1 free minutes. */
	private static final String PREFS_PLAN1_FREEMIN = "plan1_freemin";
	/** Prefs: plan1 totally free sms. */
	private static final String PREFS_PLAN1_T_FREE_SMS = "plan1_total_free_sms";
	/** Prefs: plan1 free sms. */
	private static final String PREFS_PLAN1_FREESMS = "plan1_freesms";
	/** Prefs: plan1 totally free calls. */
	private static final String PREFS_PLAN2_T_FREE_CALLS = "plan2_total_free_calls";
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
	/** Prefs: merge sms into calls; number of seconds billed for a single sms. */
	private static final String PREFS_MERGE_SMS_TO_CALLS_SECONDS = "merge_sms_calls_sec";
	/** Prefs: merge sms into calls; which plan to merge sms in. */
	private static final String PREFS_MERGE_SMS_PLAN1 = "merge_sms_plan1";

	/** Prefs: name for all old calls in. */
	private static final String PREFS_ALL_CALLS_IN = "all_calls_in";
	/** Prefs: name for all old calls out. */
	private static final String PREFS_ALL_CALLS_OUT = "all_calls_out";
	/** Prefs: name for all old sms in. */
	private static final String PREFS_ALL_SMS_IN = "all_sms_in";
	/** Prefs: name for all old sms out. */
	private static final String PREFS_ALL_SMS_OUT = "all_sms_out";
	/** Prefs: name for date of old calls/sms. */
	private static final String PREFS_DATE_OLD = "all_date_old";
	/** Prefs: Exclude people prefix. */
	private static final String PREFS_EXCLUDE_PEOPLE_PREFIX = "exclude_people_";
	/** Prefs: Exclude people count. */
	private static final String PREFS_EXCLUDE_PEOPLE_COUNT = PREFS_EXCLUDE_PEOPLE_PREFIX
			+ "n";
	/** Prefs: enable data stats. */
	private static final String PREFS_DATA_ENABLE = "data_enable";
	/** Prefs: limit for data traffic. */
	private static final String PREFS_DATA_LIMIT = "data_limit";

	/** Prefs: data in at last boot. */
	static final String PREFS_DATA_BOOT_IN = "data_boot_in";
	/** Prefs: data out at last boot. */
	static final String PREFS_DATA_BOOT_OUT = "data_boot_out";
	/** Prefs: data in after last boot. */
	static final String PREFS_DATA_RUNNING_IN = "data_running_in";
	/** Prefs: data out after last boot. */
	static final String PREFS_DATA_RUNNING_OUT = "data_running_out";
	/** Prefs: data in before bolling date. */
	static final String PREFS_DATA_PREBILLING_IN = "data_prebilling_in";
	/** Prefs: data out before billing date. */
	static final String PREFS_DATA_PREBILLING_OUT = "data_prebilling_out";
	/** Prefs: date of last billing. */
	static final String PREFS_DATA_LASTCHECK = "data_lastcheck";

	/** Prefs: billmode: 1/1. */
	private static final String BILLMODE_1_1 = "1_1";
	/** Prefs: billmode: 10/10. */
	private static final String BILLMODE_10_10 = "10_10";
	/** Prefs: billmode: 30/6. */
	private static final String BILLMODE_30_6 = "30_6";
	/** Prefs: billmode: 30/10. */
	private static final String BILLMODE_30_10 = "30_10";
	/** Prefs: billmode: 45/1. */
	private static final String BILLMODE_45_1 = "45_1";
	/** Prefs: billmode: 60/0. */
	private static final String BILLMODE_60_0 = "60_0";
	/** Prefs: billmode: 60/1. */
	private static final String BILLMODE_60_1 = "60_1";
	/** Prefs: billmode: 60/10. */
	private static final String BILLMODE_60_10 = "60_10";
	/** Prefs: billmode: 60/60. */
	private static final String BILLMODE_60_60 = "60_60";

	/** ContentProvider Column: Body. */
	private static final String BODY = "body";

	/** SharedPreferences. */
	private static SharedPreferences preferences;

	/** Unique ID of device. */
	private String imeiHash = null;
	/** Display ads? */
	private static boolean prefsNoAds;

	/** Preferences: excluded numbers. */
	private static ArrayList<String> prefsExcludePeople;
	/** ArrayAdapter for excluded numbers. */
	private static ArrayAdapter<String> excludedPeaoplAdapter;

	/** Array of md5(imei) for which no ads should be displayed. */
	private static final String[] NO_AD_HASHS = { // .
	"43dcb861b9588fb733300326b61dbab9", // me
			"d9018351e0159dd931e20cc1861ac5d8", // Tommaso C.
			"2c72e52ef02a75210dc6680edab6b75d", // Danny S.
			"f39b49859c04e6ea7849b43c73bd050e", // Łukasz M.
			"225905ca10fd56ae9c4b82254fa6d490", // George K.
			// "9e30468a2b516aac2d1ddf2a875ca8b8", // Alfons V.
			"4bf7f35515fb7306dc7c43c9fa88558c", // Ronny T.
			"75b9d156ebfda12a0e63da875593edc0", // Ángel M.
			"80cfd25e841424e968db64de0d7d236e" // Renato P.
	};

	/**
	 * Preferences.
	 * 
	 * @author flx
	 */
	public static class Preferences extends PreferenceActivity implements
			SharedPreferences.OnSharedPreferenceChangeListener {

		/** Preference: merge sms into calls. */
		private Preference prefMergeSMStoCalls = null;
		/** Preference: merge sms into plan 1. */
		private Preference prefMergeToPlan1 = null;

		/**
		 * {@inheritDoc}
		 */
		@Override
		public final void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.addPreferencesFromResource(R.xml.prefs);
			CallMeter.preferences
					.registerOnSharedPreferenceChangeListener(this);
			this.prefMergeSMStoCalls = this
					.findPreference(PREFS_MERGE_SMS_TO_CALLS);
			this.prefMergeToPlan1 = this.findPreference(PREFS_MERGE_SMS_PLAN1);
			// run check on create!
			this.onSharedPreferenceChanged(CallMeter.preferences,
					PREFS_SPLIT_PLANS);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void onSharedPreferenceChanged(
				final SharedPreferences sharedPreferences, final String key) {
			if (key.equals(PREFS_SPLIT_PLANS)
					|| key.equals(PREFS_MERGE_PLANS_SMS)
					|| key.equals(PREFS_MERGE_PLANS_CALLS)
					|| key.equals(PREFS_MERGE_SMS_TO_CALLS)) {
				final boolean b0 = sharedPreferences.getBoolean(
						PREFS_SPLIT_PLANS, false);
				final boolean b1 = sharedPreferences.getBoolean(
						PREFS_MERGE_PLANS_SMS, false);
				final boolean b2 = sharedPreferences.getBoolean(
						PREFS_MERGE_PLANS_CALLS, false);
				final boolean b3 = sharedPreferences.getBoolean(
						PREFS_MERGE_SMS_TO_CALLS, false);
				this.prefMergeSMStoCalls.setEnabled(!b0 || b1);
				this.prefMergeToPlan1.setEnabled(b0 && b1 && !b2 && b3);
			}
		}
	}

	/**
	 * Preferences subscreen to exclude numbers.
	 * 
	 * @author flx
	 */
	public static class ExcludePeople extends Activity implements
			OnItemClickListener {
		/**
		 * {@inheritDoc}
		 */
		protected final void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.setContentView(R.layout.exclude_people);
			final ListView lv = (ListView) this.findViewById(R.id.list);
			lv.setAdapter(CallMeter.excludedPeaoplAdapter);
			lv.setOnItemClickListener(this);
		}

		/**
		 * {@inheritDoc}
		 */
		protected final void onPause() {
			super.onPause();
			SharedPreferences.Editor editor = CallMeter.preferences.edit();
			final int s = CallMeter.prefsExcludePeople.size();
			editor.putInt(PREFS_EXCLUDE_PEOPLE_COUNT, s - 1);
			for (int i = 1; i < s; i++) {
				editor.putString(PREFS_EXCLUDE_PEOPLE_PREFIX + (i - 1),
						CallMeter.prefsExcludePeople.get(i));
			}
			editor.commit();
		}

		/**
		 * {@inheritDoc}
		 */
		public final void onItemClick(final AdapterView<?> parent,
				final View view, final int position, final long id) {
			if (position == 0) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						this);
				final EditText et = new EditText(this);
				builder.setView(et);
				builder.setTitle(R.string.exclude_people_add);
				builder.setCancelable(true);
				builder.setPositiveButton(android.R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								CallMeter.prefsExcludePeople.add(et.getText()
										.toString());
								CallMeter.excludedPeaoplAdapter
										.notifyDataSetChanged();
								dialog.dismiss();
							}
						});
				builder.setNegativeButton(android.R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(final DialogInterface dialog,
									final int id) {
								dialog.cancel();
							}
						});
				builder.create().show();
			} else {
				final AlertDialog.Builder builder = new AlertDialog.Builder(
						this);
				builder.setCancelable(true);
				final String[] itms = new String[2];
				itms[0] = this.getString(R.string.exclude_people_edit);
				itms[1] = this.getString(R.string.exclude_people_delete);
				builder.setItems(itms, new DialogInterface.OnClickListener() {
					public void onClick(final DialogInterface dialog,
							final int item) {
						if (item == 0) { // edit
							final AlertDialog.Builder builder2 = new AlertDialog.Builder(
									ExcludePeople.this);
							final EditText et = new EditText(ExcludePeople.this);
							et.setText(CallMeter.prefsExcludePeople
									.get(position));
							builder2.setView(et);
							builder2.setTitle(R.string.exclude_people_edit);
							builder2.setCancelable(true);
							builder2.setPositiveButton(android.R.string.ok,
									new DialogInterface.OnClickListener() {
										public void onClick(
												final DialogInterface dialog,
												final int id) {
											CallMeter.prefsExcludePeople.set(
													position, et.getText()
															.toString());
											CallMeter.excludedPeaoplAdapter
													.notifyDataSetChanged();
											dialog.dismiss();
										}
									});
							builder2.setNegativeButton(android.R.string.cancel,
									new DialogInterface.OnClickListener() {
										public void onClick(
												final DialogInterface dialog,
												final int id) {
											dialog.cancel();
										}
									});
							builder2.create().show();
						} else { // delete
							CallMeter.prefsExcludePeople.remove(position);
							CallMeter.excludedPeaoplAdapter
									.notifyDataSetChanged();
						}
					}

				});
				builder.create().show();
			}
		}
	}

	/**
	 * AsyncTask to handel calcualtions in background.
	 * 
	 * @author flx
	 */
	class Updater extends AsyncTask<Void, Void, Integer[]> {
		/** Status Strings. */
		private String callsIn, callsOut1, callsOut2, callsBillDate, smsIn,
				smsOut1, smsOut2, smsBillDate, dataIn, dataOut;
		/** Status TextViews. */
		private TextView twCallsIn, twCallsOut1, twCallsOut2, twCallsBillDate,
				twSMSIn, twSMSOut1, twSMSOut2, twSMSBillDate, twDataBillDate,
				twDataIn, twDataOut;
		/** Status ProgressBars. */
		private ProgressBar pbCalls1, pbCalls2, pbSMS1, pbSMS2, pbData;

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

		/** Sum of displayed calls out. Used if merging sms into calls. */
		private int callsOutSum;

		/**
		 * Parse number of seconds to a readable time format.
		 * 
		 * @param seconds
		 *            seconds
		 * @return parsed string
		 */
		private String getTime(final int seconds) {
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
		 * Round up time with billmode in mind.
		 * 
		 * @param time
		 *            time
		 * @return rounded time
		 */
		private int roundTime(final int time) {
			final String prefBillMode = CallMeter.preferences.getString(
					PREFS_BILLMODE, BILLMODE_1_1);
			// 0 => 0
			if (time == 0) {
				return 0;
			}
			// !0 ..
			if (prefBillMode.equals(BILLMODE_1_1)) {
				return time;
			} else if (prefBillMode.equals(BILLMODE_10_10)) {
				if (time % 10 != 0) {
					return ((time / 10) + 1) * 10;
				}
			} else if (prefBillMode.equals(BILLMODE_45_1)) {
				if (time < 45) {
					return 45;
				}
			} else if (prefBillMode.equals(BILLMODE_60_0)) {
				return 60;
			} else if (prefBillMode.equals(BILLMODE_60_1)) {
				if (time < 60) {
					return 60;
				}
			} else if (prefBillMode.equals(BILLMODE_60_10)) {
				if (time < 60) {
					return 60;
				} else if (time % 10 != 0) {
					return ((time / 10) + 1) * 10;
				}
			} else if (prefBillMode.equals(BILLMODE_30_6)) {
				if (time < 30) {
					return 30;
				} else if (time % 6 != 0) {
					return ((time / 6) + 1) * 6;
				}
			} else if (prefBillMode.equals(BILLMODE_30_10)) {
				if (time < 30) {
					return 30;
				} else if (time % 10 != 0) {
					return ((time / 10) + 1) * 10;
				}
			} else if (prefBillMode.equals(BILLMODE_60_60)) {
				if (time % 60 != 0) {
					return ((time / 60) + 1) * 60;
				}
			}
			return time;
		}

		/**
		 * Return "old" date as timestamp.
		 * 
		 * @return date before all calls/sms are "old"
		 */
		private long getOldDate() {
			Calendar cal = Calendar.getInstance();
			cal.roll(Calendar.MONTH, -1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal.getTimeInMillis();
		}

		/**
		 * Load plans from preferences.
		 * 
		 * @param p
		 *            {@link SharedPreferences} to read plans from
		 * @return null if plans aren't splitetd, else true for all hours in
		 *         plan1
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
			this.twCallsBillDate.setText(this.callsBillDate);
			this.twCallsIn.setText(this.callsIn);
			this.twCallsOut1.setText(this.callsOut1);
			this.twCallsOut2.setText(this.callsOut2);

			this.twSMSBillDate.setText(this.smsBillDate);
			this.twSMSIn.setText(this.smsIn);
			this.twSMSOut1.setText(this.smsOut1);
			this.twSMSOut2.setText(this.smsOut2);

			this.twDataBillDate.setText(this.callsBillDate);
			this.twDataIn.setText(this.dataIn);
			this.twDataOut.setText(this.dataOut);
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
		private String calcString(final int thisPeriod, final int limit,
				final int all, final boolean calls) {
			if (limit > 0) {
				if (calls) {
					return ((thisPeriod * 100) / (limit * SECONDS_MINUTE))
							+ "% / " + this.getTime(thisPeriod) + " / "
							+ this.getTime(all);
				} else {
					return ((thisPeriod * 100) / limit) + "% / " + thisPeriod
							+ " / " + all;
				}
			} else {
				if (calls) {
					return this.getTime(thisPeriod) + " / " + this.getTime(all);
				} else {
					return thisPeriod + " / " + all;
				}
			}
		}

		/**
		 * Run before background task.
		 */
		@Override
		protected void onPreExecute() {
			CallMeter.this.setProgressBarIndeterminateVisibility(true);

			if (CallMeter.preferences.getBoolean(PREFS_SPLIT_PLANS, false)) {
				this.plansMergeCalls = CallMeter.preferences.getBoolean(
						PREFS_MERGE_PLANS_CALLS, false);
				this.plansMergeSms = CallMeter.preferences.getBoolean(
						PREFS_MERGE_PLANS_SMS, false);
			} else {
				this.plansMergeCalls = true;
				this.plansMergeSms = true;
			}

			String namePlan1 = CallMeter.preferences.getString(
					PREFS_NAME_PLAN1, "");
			if (namePlan1.length() <= 0) {
				namePlan1 = CallMeter.this.getString(R.string.plan1);
			}
			String namePlan2 = CallMeter.preferences.getString(
					PREFS_NAME_PLAN2, "");
			if (namePlan2.length() <= 0) {
				namePlan2 = CallMeter.this.getString(R.string.plan2);
			}
			namePlan1 = " (" + namePlan1 + ")";
			namePlan2 = " (" + namePlan2 + ")";

			// load old values from database
			this.allCallsIn = CallMeter.preferences.getInt(PREFS_ALL_CALLS_IN,
					0);
			this.allCallsOut = CallMeter.preferences.getInt(
					PREFS_ALL_CALLS_OUT, 0);
			this.allSMSIn = CallMeter.preferences.getInt(PREFS_ALL_SMS_IN, 0);
			this.allSMSOut = CallMeter.preferences.getInt(PREFS_ALL_SMS_OUT, 0);
			this.allOldDate = CallMeter.preferences.getLong(PREFS_DATE_OLD, 0);

			this.pbCalls1 = (ProgressBar) CallMeter.this
					.findViewById(R.id.calls1_progressbar);
			this.pbCalls2 = (ProgressBar) CallMeter.this
					.findViewById(R.id.calls2_progressbar);

			this.pbSMS1 = (ProgressBar) CallMeter.this
					.findViewById(R.id.sms1_progressbar);
			this.pbSMS2 = (ProgressBar) CallMeter.this
					.findViewById(R.id.sms2_progressbar);
			this.pbData = (ProgressBar) CallMeter.this
					.findViewById(R.id.data_progressbar);

			this.twCallsIn = (TextView) CallMeter.this
					.findViewById(R.id.calls_in);
			this.twCallsOut1 = (TextView) CallMeter.this
					.findViewById(R.id.calls1_out);
			this.twCallsOut2 = (TextView) CallMeter.this
					.findViewById(R.id.calls2_out);
			this.twCallsBillDate = (TextView) CallMeter.this
					.findViewById(R.id.calls_billdate);
			this.twSMSIn = (TextView) CallMeter.this.findViewById(R.id.sms_in);
			this.twSMSOut1 = (TextView) CallMeter.this
					.findViewById(R.id.sms1_out);
			this.twSMSOut2 = (TextView) CallMeter.this
					.findViewById(R.id.sms2_out);
			this.twSMSBillDate = (TextView) CallMeter.this
					.findViewById(R.id.sms_billdate);
			this.twDataBillDate = (TextView) CallMeter.this
					.findViewById(R.id.data_billdate);
			this.twDataIn = (TextView) CallMeter.this
					.findViewById(R.id.data_in);
			this.twDataOut = (TextView) CallMeter.this
					.findViewById(R.id.data_out);

			this.pbCalls1.setProgress(0);
			this.pbCalls1.setIndeterminate(false);
			this.pbCalls1.setVisibility(View.VISIBLE);
			if (this.plansMergeCalls) {
				((TextView) CallMeter.this.findViewById(R.id.calls1_out_))
						.setText(String.format(CallMeter.this
								.getString(R.string.out_calls), ""));

				CallMeter.this.findViewById(R.id.calls2_out_).setVisibility(
						View.GONE);
				this.twCallsOut2.setVisibility(View.GONE);
				this.pbCalls2.setVisibility(View.GONE);
			} else {
				CallMeter.this.findViewById(R.id.calls2_out_).setVisibility(
						View.VISIBLE);
				this.twCallsOut2.setVisibility(View.VISIBLE);

				String s = CallMeter.this.getString(R.string.out_calls);
				((TextView) CallMeter.this.findViewById(R.id.calls1_out_))
						.setText(String.format(s, namePlan1));
				((TextView) CallMeter.this.findViewById(R.id.calls2_out_))
						.setText(String.format(s, namePlan2));

				this.pbCalls2.setProgress(0);
				this.pbCalls2.setIndeterminate(false);
				this.pbCalls2.setVisibility(View.VISIBLE);
			}
			this.pbSMS1.setProgress(0);
			this.pbSMS1.setIndeterminate(false);
			this.pbSMS1.setVisibility(View.VISIBLE);
			if (this.plansMergeSms) {
				((TextView) CallMeter.this.findViewById(R.id.sms1_out_))
						.setText(String.format(CallMeter.this
								.getString(R.string.out_sms), ""));

				CallMeter.this.findViewById(R.id.sms2_out_).setVisibility(
						View.GONE);
				this.twSMSOut2.setVisibility(View.GONE);
				this.pbSMS2.setVisibility(View.GONE);
			} else {
				CallMeter.this.findViewById(R.id.sms2_out_).setVisibility(
						View.VISIBLE);
				this.twSMSOut2.setVisibility(View.VISIBLE);

				String s = CallMeter.this.getString(R.string.out_sms);
				((TextView) CallMeter.this.findViewById(R.id.sms1_out_))
						.setText(String.format(s, namePlan1));
				((TextView) CallMeter.this.findViewById(R.id.sms2_out_))
						.setText(String.format(s, namePlan2));

				this.pbSMS2.setProgress(0);
				this.pbSMS2.setIndeterminate(false);
				this.pbSMS2.setVisibility(View.VISIBLE);
			}

			int v = View.VISIBLE;
			if (CallMeter.preferences.getBoolean(PREFS_MERGE_SMS_TO_CALLS,
					false)) {
				v = View.GONE;
				CallMeter.this.findViewById(R.id.sms2_out_).setVisibility(v);
				this.twSMSOut2.setVisibility(v);
				this.pbSMS2.setVisibility(v);
			}
			CallMeter.this.findViewById(R.id.sms_).setVisibility(v);
			CallMeter.this.findViewById(R.id.sms_billdate_).setVisibility(v);
			CallMeter.this.findViewById(R.id.sms_billdate).setVisibility(v);
			CallMeter.this.findViewById(R.id.sms1_out_).setVisibility(v);
			CallMeter.this.findViewById(R.id.sms1_out).setVisibility(v);
			CallMeter.this.findViewById(R.id.sms_in_).setVisibility(v);
			CallMeter.this.findViewById(R.id.sms_in).setVisibility(v);
			this.pbSMS1.setVisibility(v);

			// data
			v = View.GONE;
			if (CallMeter.preferences.getBoolean(CallMeter.PREFS_DATA_ENABLE,
					false)) {
				v = View.VISIBLE;
			}
			CallMeter.this.findViewById(R.id.data_).setVisibility(v);
			CallMeter.this.findViewById(R.id.data_billdate_layout)
					.setVisibility(v);
			CallMeter.this.findViewById(R.id.data_in_layout).setVisibility(v);
			CallMeter.this.findViewById(R.id.data_out_layout).setVisibility(v);
			CallMeter.this.findViewById(R.id.data_progressbar).setVisibility(v);

			// common

			this.callsBillDate = "?";
			this.callsIn = "?";
			this.callsOut1 = "?";
			this.callsOut2 = "?";
			this.smsBillDate = "?";
			this.smsIn = "?";
			this.smsOut1 = "?";
			this.smsOut2 = "?";
			this.dataIn = "?";
			this.dataOut = "?";

			this.updateText();
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
		private void walkCalls(final boolean[][] plans,
				final Calendar calBillDate, final long oldDate,
				final Integer[] status) {

			this.callsBillDate = DateFormat.getDateFormat(CallMeter.this)
					.format(calBillDate.getTime());
			long billDate = calBillDate.getTimeInMillis();

			final String[] excludeNumbers = CallMeter.prefsExcludePeople
					.toArray(new String[1]);
			final int excludeNumbersSize = excludeNumbers.length;

			// report calls
			String[] projection = new String[] { Calls.TYPE, Calls.DURATION,
					Calls.DATE, Calls.NUMBER };

			Cursor cur = CallMeter.this.managedQuery(Calls.CONTENT_URI,
					projection, Calls.DATE + " >= " + this.allOldDate, null,
					Calls.DATE + " DESC");

			int durIn = this.allCallsIn;
			int durOut = this.allCallsOut;
			int durInMonth = 0;
			int durOut1Month = 0;
			int durOut2Month = 0;
			int free1 = 0; // -1 -> totally free
			int free2 = 0;
			if (CallMeter.preferences.getBoolean(PREFS_PLAN1_T_FREE_CALLS,
					false)) {
				free1 = -1;
			} else {
				String s = CallMeter.preferences.getString(PREFS_PLAN1_FREEMIN,
						"0");
				if (s.length() > 0) {
					free1 = Integer.parseInt(s);
				}
			}
			if (CallMeter.preferences.getBoolean(PREFS_PLAN2_T_FREE_CALLS,
					false)) {
				free2 = -1;
			} else {
				String s = CallMeter.preferences.getString(PREFS_PLAN2_FREEMIN,
						"0");
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
									if (n.startsWith(s.substring(0,
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
						this.callsIn = this.calcString(durInMonth, 0, durIn,
								true);
						this.callsOut1 = this.calcString(durOut1Month, free1,
								durOut, true);
						this.callsOut2 = this.calcString(durOut2Month, free2,
								durOut, true);
						this.publishProgress((Void) null);
					}
				} while (cur.moveToNext());
			}
			this.callsIn = this.calcString(durInMonth, 0, durIn, true);
			this.callsOut1 = this.calcString(durOut1Month, free1, durOut, true);
			this.callsOut2 = this.calcString(durOut2Month, free2, durOut, true);

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
		private void walkSMS(final boolean[][] plans,
				final Calendar calBillDate, final long oldDate,
				final Integer[] status) {
			// report basics
			this.smsBillDate = DateFormat.getDateFormat(CallMeter.this).format(
					calBillDate.getTime());
			final long billDate = calBillDate.getTimeInMillis();
			final String[] projection = new String[] { Calls.TYPE, Calls.DATE,
					BODY };
			final Cursor cur = CallMeter.this.managedQuery(Uri
					.parse("content://sms"), projection, Calls.DATE + " >= "
					+ this.allOldDate, null, Calls.DATE + " DESC");
			int free1 = 0;
			int free2 = 0;
			if (CallMeter.preferences.getBoolean(PREFS_PLAN1_T_FREE_SMS, false)) {
				free1 = -1;
			} else {
				String s = CallMeter.preferences.getString(PREFS_PLAN1_FREESMS,
						"0");
				if (s.length() > 0) {
					free1 = Integer.parseInt(s);
				}
			}
			if (CallMeter.preferences.getBoolean(PREFS_PLAN2_T_FREE_SMS, false)) {
				free2 = -1;
			} else {
				String s = CallMeter.preferences.getString(PREFS_PLAN2_FREESMS,
						"0");
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
				final int idBody = cur.getColumnIndex(BODY);
				int i = 0;
				int l = 1;
				do {
					type = cur.getInt(idType);
					d = cur.getLong(idDate);
					l = SmsMessage
							.calculateLength(cur.getString(idBody), false)[0];
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
						this.smsIn = this.calcString(smsInMonth, 0, iSMSIn,
								false);
						this.smsOut1 = this.calcString(smsOut2Month, free1,
								iSMSOut, false);
						this.smsOut2 = this.calcString(smsOut2Month, free2,
								iSMSOut, false);
						this.publishProgress((Void) null);
					}
				} while (cur.moveToNext());
			}

			this.smsIn = this.calcString(smsInMonth, 0, iSMSIn, false);
			this.smsOut1 = this.calcString(smsOut1Month, free1, iSMSOut, false);
			this.smsOut2 = this.calcString(smsOut2Month, free2, iSMSOut, false);

			status[4] = smsOut1Month;
			status[5] = free1;
			status[6] = smsOut2Month;
			status[7] = free2;

			if (CallMeter.preferences.getBoolean(PREFS_MERGE_SMS_TO_CALLS,
					false)) {
				// merge sms into calls.
				final boolean mergeToPlan1 = this.plansMergeCalls
						|| CallMeter.preferences.getBoolean(
								PREFS_MERGE_SMS_PLAN1, true);
				final int secondsForSMS = Integer
						.parseInt(CallMeter.preferences.getString(
								PREFS_MERGE_SMS_TO_CALLS_SECONDS, "0"));
				int i = 0; // plan 1 number of seconds
				if (!mergeToPlan1) {
					i = 2; // plan 2 number of seconds
				}
				status[i] += secondsForSMS * smsOut1Month;

				status[4] = 0;
				status[5] = 0;
				status[6] = 0;
				status[7] = 0;

				final String s = this.calcString(status[i], status[i + 1],
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
		 * @param data
		 *            amaount of data transfered
		 * @return more readable output
		 */
		private String makeBytesReadable(final long data) {
			return data / (BYTES_MEGABYTE) + "MB";
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
			final boolean[][] plans = this.loadPlans(CallMeter.preferences);
			final long oldDate = this.getOldDate();

			// progressbar positions: calls1_pos, calls1_max, calls2_*, sms*,
			// data*
			final Integer[] ret = { 0, 0, 0, 0, 1, 1, 1, 1, 2, 2 };
			Calendar calBillDate = getBillDate(Integer
					.parseInt(CallMeter.preferences.getString(PREFS_BILLDAY,
							"0")));
			if (this.plansMergeCalls) {
				this.walkCalls(null, calBillDate, oldDate, ret);
			} else {
				this.walkCalls(plans, calBillDate, oldDate, ret);
			}

			// report sms
			if (!CallMeter.preferences.getBoolean(PREFS_SMSPERIOD, false)) {
				calBillDate = getBillDate(Integer
						.parseInt(CallMeter.preferences.getString(
								PREFS_SMSBILLDAY, "0")));
			}
			if (this.plansMergeSms) {
				this.walkSMS(null, calBillDate, oldDate, ret);
			} else {
				this.walkSMS(plans, calBillDate, oldDate, ret);
			}

			// report data
			if (CallMeter.preferences.getBoolean(CallMeter.PREFS_DATA_ENABLE,
					false)) {
				// walk data
				CMBroadcastReceiver.updateTraffic(CallMeter.this,
						CallMeter.preferences);
				// get data from prefs
				final long preBootIn = CallMeter.preferences.getLong(
						CallMeter.PREFS_DATA_BOOT_IN, 0);
				final long preBootOut = CallMeter.preferences.getLong(
						CallMeter.PREFS_DATA_BOOT_OUT, 0);
				final long preBillingIn = CallMeter.preferences.getLong(
						CallMeter.PREFS_DATA_PREBILLING_IN, 0);
				final long preBillingOut = CallMeter.preferences.getLong(
						CallMeter.PREFS_DATA_PREBILLING_OUT, 0);
				final long runningIn = CallMeter.preferences.getLong(
						CallMeter.PREFS_DATA_RUNNING_IN, 0);
				final long runningOut = CallMeter.preferences.getLong(
						CallMeter.PREFS_DATA_RUNNING_OUT, 0);
				final long currentIn = preBootIn + runningIn;
				final long currentOut = preBootOut + runningOut;
				final long thisBillingIn = currentIn - preBillingIn;
				final long thisBillingOut = currentOut - preBillingOut;
				this.dataIn = this.makeBytesReadable(thisBillingIn) + "/"
						+ this.makeBytesReadable(currentIn);
				this.dataOut = this.makeBytesReadable(currentOut
						- preBillingOut)
						+ "/" + this.makeBytesReadable(currentOut);
				final long limit = Long.parseLong(CallMeter.preferences
						.getString(PREFS_DATA_LIMIT, "0"));
				ret[8] = (int) (thisBillingIn + thisBillingOut)
						/ (BYTES_MEGABYTE);
				ret[9] = (int) limit;
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

			pb1 = this.pbData;
			if (result[9] > 0) {
				pb1.setMax(result[9]);
				pb1.setProgress(result[8]);
				pb1.setVisibility(View.VISIBLE);
			} else {
				pb1.setVisibility(View.GONE);
			}

			// save old values to database
			SharedPreferences.Editor editor = CallMeter.preferences.edit();
			editor.putInt(PREFS_ALL_CALLS_IN, this.allCallsIn);
			editor.putInt(PREFS_ALL_CALLS_OUT, this.allCallsOut);
			editor.putInt(PREFS_ALL_SMS_IN, this.allSMSIn);
			editor.putInt(PREFS_ALL_SMS_OUT, this.allSMSOut);
			editor.putLong(PREFS_DATE_OLD, this.allOldDate);
			editor.commit();

			CallMeter.this.setProgressBarIndeterminateVisibility(false);
		}
	}

	/**
	 * Return Billdate as Calendar for a given day of month.
	 * 
	 * @param billDay
	 *            first day of bill
	 * @return date as Calendar
	 */
	static final Calendar getBillDate(final int billDay) {
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
	 * Called when the activity is first created.
	 * 
	 * @param savedInstanceState
	 *            a Bundle
	 */
	@Override
	public final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		this.setContentView(R.layout.main);
		// get prefs.
		CallMeter.preferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		String v0 = CallMeter.preferences.getString(PREFS_LAST_RUN, "");
		String v1 = this.getString(R.string.app_version);
		if (!v0.equals(v1)) {
			SharedPreferences.Editor editor = CallMeter.preferences.edit();
			editor.putString(PREFS_LAST_RUN, v1);
			editor.commit();
			this.showDialog(DIALOG_UPDATE);
		}
		// get imei
		TelephonyManager mTelephonyMgr = (TelephonyManager) this
				.getSystemService(TELEPHONY_SERVICE);
		final String s = mTelephonyMgr.getDeviceId();
		prefsNoAds = false;
		if (s != null) {
			this.imeiHash = md5(s);
			for (String h : NO_AD_HASHS) {
				if (this.imeiHash.equals(h)) {
					prefsNoAds = true;
					break;
				}
			}
		}
		prefsExcludePeople = new ArrayList<String>();
		prefsExcludePeople.add(this.getString(R.string.exclude_people_add));
		final int c = CallMeter.preferences.getInt(PREFS_EXCLUDE_PEOPLE_COUNT,
				0);
		for (int i = 0; i < c; i++) {
			CallMeter.prefsExcludePeople.add(CallMeter.preferences.getString(
					PREFS_EXCLUDE_PEOPLE_PREFIX + i, "???"));
		}
		excludedPeaoplAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, prefsExcludePeople);
	}

	/** Called on Activity resume. */
	@Override
	protected final void onResume() {
		super.onResume();
		if (!prefsNoAds) {
			((AdView) this.findViewById(R.id.ad)).setVisibility(View.VISIBLE);
		}
		// get calls
		new Updater().execute((Void[]) null);
	}

	/**
	 * Called to create dialog.
	 * 
	 * @param id
	 *            Dialog id
	 * @return dialog
	 */
	@Override
	protected final Dialog onCreateDialog(final int id) {
		Dialog d;
		AlertDialog.Builder builder;
		switch (id) {
		case DIALOG_DONATE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.remove_ads);
			builder.setMessage(R.string.postdonate);
			builder.setPositiveButton(R.string.send_,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							final Intent in = new Intent(Intent.ACTION_SEND);
							in.putExtra(Intent.EXTRA_EMAIL, new String[] {
									CallMeter.this
											.getString(R.string.donate_mail),
									"" }); // FIXME: "" is a k9 hack.
							in.putExtra(Intent.EXTRA_TEXT,
									CallMeter.this.imeiHash);
							in
									.putExtra(
											Intent.EXTRA_SUBJECT,
											CallMeter.this
													.getString(R.string.app_name)
													+ " "
													+ CallMeter.this
															.getString(R.string.donate_subject));
							in.setType("text/plain");
							CallMeter.this.startActivity(in);
							dialog.dismiss();
						}
					});
			builder.setNegativeButton(android.R.string.cancel,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int which) {
							dialog.cancel();
						}
					});
			return builder.create();
		case DIALOG_ABOUT:
			d = new Dialog(this);
			d.setContentView(R.layout.about);
			d.setTitle(this.getString(R.string.about_) + " v"
					+ this.getString(R.string.app_version));
			return d;
		case DIALOG_UPDATE:
			builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.changelog_);
			final String[] changes = this.getResources().getStringArray(
					R.array.updates);
			final StringBuilder buf = new StringBuilder(changes[0]);
			for (int i = 1; i < changes.length; i++) {
				buf.append("\n\n");
				buf.append(changes[i]);
			}
			builder.setIcon(android.R.drawable.ic_menu_info_details);
			builder.setMessage(buf.toString());
			builder.setCancelable(true);
			builder.setPositiveButton(android.R.string.ok,
					new DialogInterface.OnClickListener() {
						public void onClick(final DialogInterface dialog,
								final int id) {
							dialog.cancel();
						}
					});
			return builder.create();
		default:
			return null;
		}
	}

	/**
	 * Open menu.
	 * 
	 * @param menu
	 *            menu to inflate
	 * @return ok/fail?
	 */
	@Override
	public final boolean onCreateOptionsMenu(final Menu menu) {
		MenuInflater inflater = this.getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Handles item selections.
	 * 
	 * @param item
	 *            menu item
	 * @return done?
	 */
	public final boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case R.id.item_about: // start about dialog
			this.showDialog(DIALOG_ABOUT);
			return true;
		case R.id.item_settings: // start settings activity
			this.startActivity(new Intent(this, Preferences.class));
			return true;
		case R.id.item_donate:
			try {
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse(this.getString(R.string.donate_url))));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no browser", e);
			}
			CallMeter.this.showDialog(DIALOG_DONATE);
			return true;
		case R.id.item_more:
			try {
				this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
						.parse("market://search?q=pub:\"Felix Bechstein\"")));
			} catch (ActivityNotFoundException e) {
				Log.e(TAG, "no market", e);
			}
			return true;
		default:
			return false;
		}
	}

	/**
	 * Calc MD5 Hash from String.
	 * 
	 * @param s
	 *            input
	 * @return hash
	 */
	private static String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte[] messageDigest = digest.digest();
			// Create Hex String
			StringBuilder hexString = new StringBuilder(32);
			int b;
			for (int i = 0; i < messageDigest.length; i++) {
				b = 0xFF & messageDigest[i];
				if (b < 0x10) {
					hexString.append('0' + Integer.toHexString(b));
				} else {
					hexString.append(Integer.toHexString(b));
				}
			}
			return hexString.toString();
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, null, e);
		}
		return "";
	}
}
