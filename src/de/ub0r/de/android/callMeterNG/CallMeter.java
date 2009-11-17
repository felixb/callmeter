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

	/** Prefs: name for last version run. */
	private static final String PREFS_LAST_RUN = "lastrun";
	/** Prefs: name for first day. */
	private static final String PREFS_BILLDAY = "billday";
	/** Prefs: name for billingmode. */
	private static final String PREFS_BILLMODE = "billmode";
	/** Prefs: name for smsperiod. */
	private static final String PREFS_SMSPERIOD = "smsperiod";
	/** Prefs: name for smsbillday. */
	private static final String PREFS_SMSBILLDAY = "smsbillday";
	/** Prefs: name for free min. */
	private static final String PREFS_FREEMIN = "freemin";
	/** Prefs: name for free sms. */
	private static final String PREFS_FREESMS = "freesms";

	/** Prefs: name for freedays calls. */
	private static final String PREFS_FREEDAYS_CALL = "freedays_call";
	/** Prefs: name for freedays sms. */
	private static final String PREFS_FREEDAYS_SMS = "freedays_sms";
	/** Prefs: name for freedays .... */
	private static final String PREFS_FREEDAYS_ = "freedays_";

	/** Prefs: name for freehours calls. */
	private static final String PREFS_FREEHOURS_CALL = "freehours_call";
	/** Prefs: name for freehours sms. */
	private static final String PREFS_FREEHOURS_SMS = "freehours_sms";
	/** Prefs: name for freehours .... */
	private static final String PREFS_FREEHOURS_ = "freehours_";

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

	/** Prefs: billmode: 1/1. */
	private static final String BILLMODE_1_1 = "1_1";
	/** Prefs: billmode: 10/10. */
	private static final String BILLMODE_10_10 = "10_10";
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
			"9e30468a2b516aac2d1ddf2a875ca8b8" // Alfons V.
	};

	/**
	 * Preferences.
	 * 
	 * @author flx
	 */
	public static class Preferences extends PreferenceActivity {
		/**
		 * Called on Create.
		 * 
		 * @param savedInstanceState
		 *            a Bundle
		 */
		public final void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.addPreferencesFromResource(R.xml.prefs);
		}
	}

	/**
	 * Preferences.
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
		private String callsIn, callsOut, callsBillDate, smsIn, smsOut,
				smsBillDate;
		/** Status TextViews. */
		private TextView twCallsIn, twCallsOut, twCallsBillDate, twSMSIn,
				twSMSOut, twSMSBillDate;
		/** Status ProgressBars. */
		private ProgressBar pbCalls, pbSMS;

		/** Sum of old calls/sms loaded/saved from preferences. */
		private int allCallsIn, allCallsOut, allSMSIn, allSMSOut;
		/** Date of when calls/sms are "old". */
		private long allOldDate = 0;

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
			} else if (prefBillMode.equals(BILLMODE_60_60)) {
				if (time % 60 != 0) {
					return ((time / 60) + 1) * 60;
				}
			}
			return time;
		}

		/**
		 * Return Billdate as Calendar for a given day of month.
		 * 
		 * @param billDay
		 *            first day of bill
		 * @return date as Calendar
		 */
		private Calendar getBillDate(final int billDay) {
			Calendar cal = Calendar.getInstance();
			if (cal.get(Calendar.DAY_OF_MONTH) < billDay) {
				cal.roll(Calendar.MONTH, -1);
			}
			cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), billDay);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			return cal;
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
		 * Is the call/sms billed?
		 * 
		 * @param checkFreeDays
		 *            check freeDays
		 * @param freeDays
		 *            array of free days / week
		 * @param checkFreeHours
		 *            check freeHours
		 * @param freeHours
		 *            array of free hours / day
		 * @param d
		 *            date
		 * @return is billed?
		 */
		private boolean isBilled(final boolean checkFreeDays,
				final boolean[] freeDays, final boolean checkFreeHours,
				final boolean[] freeHours, final long d) {
			if (d < 0) {
				return true;
			}
			if (freeDays == null && freeHours == null) {
				return true;
			}
			Calendar date = Calendar.getInstance();
			date.setTimeInMillis(d);
			if (checkFreeDays && freeDays != null) {
				if (freeDays[(date.get(Calendar.DAY_OF_WEEK) + 5) % 7]) {
					return false;
				}
			}
			if (checkFreeHours && freeHours != null) {
				if (freeHours[date.get(Calendar.HOUR_OF_DAY)]) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Update text on main Activity.
		 */
		private void updateText() {
			this.twCallsBillDate.setText(this.callsBillDate);
			this.twCallsIn.setText(this.callsIn);
			this.twCallsOut.setText(this.callsOut);

			this.twSMSBillDate.setText(this.smsBillDate);
			this.twSMSIn.setText(this.smsIn);
			this.twSMSOut.setText(this.smsOut);
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
					return ((thisPeriod * 100) / (limit * 60)) + "% / "
							+ this.getTime(thisPeriod) + " / "
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
			// load old values from database
			this.allCallsIn = CallMeter.preferences.getInt(PREFS_ALL_CALLS_IN,
					0);
			this.allCallsOut = CallMeter.preferences.getInt(
					PREFS_ALL_CALLS_OUT, 0);
			this.allSMSIn = CallMeter.preferences.getInt(PREFS_ALL_SMS_IN, 0);
			this.allSMSOut = CallMeter.preferences.getInt(PREFS_ALL_SMS_OUT, 0);
			this.allOldDate = CallMeter.preferences.getLong(PREFS_DATE_OLD, 0);

			this.pbCalls = (ProgressBar) CallMeter.this
					.findViewById(R.id.calls_progressbar);
			this.pbSMS = (ProgressBar) CallMeter.this
					.findViewById(R.id.sms_progressbar);

			this.pbCalls.setProgress(0);
			this.pbCalls.setIndeterminate(false);
			this.pbSMS.setProgress(0);
			this.pbCalls.setVisibility(View.VISIBLE);
			this.pbSMS.setVisibility(View.VISIBLE);

			this.twCallsIn = (TextView) CallMeter.this.findViewById(R.id.in);
			this.twCallsOut = (TextView) CallMeter.this.findViewById(R.id.out);
			this.twCallsBillDate = (TextView) CallMeter.this
					.findViewById(R.id.billdate);
			this.twSMSIn = (TextView) CallMeter.this.findViewById(R.id.sms_in);
			this.twSMSOut = (TextView) CallMeter.this
					.findViewById(R.id.sms_out);
			this.twSMSBillDate = (TextView) CallMeter.this
					.findViewById(R.id.smsbilldate);

			this.callsBillDate = "?";
			this.callsIn = "?";
			this.callsOut = "?";
			this.smsBillDate = "?";
			this.smsIn = "?";
			this.smsOut = "?";

			this.updateText();
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
			Integer[] ret = { 0, 0, 1, 1 };
			// report basics
			Calendar calBillDate = this.getBillDate(Integer
					.parseInt(CallMeter.preferences.getString(PREFS_BILLDAY,
							"0")));
			long billDate = calBillDate.getTimeInMillis();
			final long oldDate = this.getOldDate();

			this.callsBillDate = DateFormat.getDateFormat(CallMeter.this)
					.format(calBillDate.getTime());

			// get not free days/timeslots
			final boolean freeDaysCalls = CallMeter.preferences.getBoolean(
					PREFS_FREEDAYS_CALL, false);
			final boolean freeDaysSMS = CallMeter.preferences.getBoolean(
					PREFS_FREEDAYS_SMS, false);
			boolean[] freeDays = null;
			if (freeDaysCalls || freeDaysSMS) {
				freeDays = new boolean[DAYS_WEEK];
				for (int i = 0; i < DAYS_WEEK; i++) {
					freeDays[i] = CallMeter.preferences.getBoolean(
							PREFS_FREEDAYS_ + (i + 1), false);
				}
			}
			final boolean freeHoursCalls = CallMeter.preferences.getBoolean(
					PREFS_FREEHOURS_CALL, false);
			final boolean freeHoursSMS = CallMeter.preferences.getBoolean(
					PREFS_FREEHOURS_SMS, false);
			boolean[] freeHours = null;
			if (freeHoursCalls || freeHoursSMS) {
				freeHours = new boolean[HOURS_DAY];
				for (int i = 0; i < HOURS_DAY; i++) {
					freeHours[i] = CallMeter.preferences.getBoolean(
							PREFS_FREEHOURS_ + i, false);
				}
			}

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
			int durOutMonth = 0;
			String s = CallMeter.preferences.getString(PREFS_FREEMIN, "0");
			int free = 0;
			if (s.length() > 0) {
				free = Integer.parseInt(s);
			}
			s = null;

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
							if (this.isBilled(freeDaysCalls, freeDays,
									freeHoursCalls, freeHours, d)) {
								check = true;
								n = cur.getString(idNumber);
								for (int j = 1; j < excludeNumbersSize; j++) {
									if (n.equals(excludeNumbers[j])) {
										check = false;
										break;
									}
								}
								if (check) {
									durOutMonth += this.roundTime(t);
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
					if (i % 50 == 1) {
						this.callsIn = this.calcString(durInMonth, 0, durIn,
								true);
						this.callsOut = this.calcString(durOutMonth, free,
								durOut, true);
						this.publishProgress((Void) null);
					}
				} while (cur.moveToNext());
			}
			this.callsIn = this.calcString(durInMonth, 0, durIn, true);
			this.callsOut = this.calcString(durOutMonth, free, durOut, true);

			ret[0] = durOutMonth;
			ret[1] = free * 60;
			this.publishProgress((Void) null);

			// report sms
			if (!CallMeter.preferences.getBoolean(PREFS_SMSPERIOD, false)) {
				calBillDate = this.getBillDate(Integer
						.parseInt(CallMeter.preferences.getString(
								PREFS_SMSBILLDAY, "0")));
				billDate = calBillDate.getTimeInMillis();

			}
			this.smsBillDate = DateFormat.getDateFormat(CallMeter.this).format(
					calBillDate.getTime());
			projection = new String[] { Calls.TYPE, Calls.DATE, BODY };
			cur = CallMeter.this.managedQuery(Uri.parse("content://sms"),
					projection, Calls.DATE + " >= " + this.allOldDate, null,
					Calls.DATE + " DESC");
			s = CallMeter.preferences.getString(PREFS_FREESMS, "0");
			free = 0;
			if (s.length() > 0) {
				free = Integer.parseInt(s);
			}
			s = null;
			int iSMSIn = this.allSMSIn;
			int iSMSOut = this.allSMSOut;
			int smsInMonth = 0;
			int smsOutMonth = 0;
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
							if (this.isBilled(freeDaysSMS, freeDays,
									freeHoursSMS, freeHours, d)) {
								smsOutMonth += l;
							}
						} else if (d < oldDate) {
							this.allSMSOut += l;
						}
						break;
					default:
						break;
					}
					++i;
					if (i % 50 == 1) {
						this.smsIn = this.calcString(smsInMonth, 0, iSMSIn,
								false);
						this.smsOut = this.calcString(smsOutMonth, free,
								iSMSOut, false);
						this.publishProgress((Void) null);
					}
				} while (cur.moveToNext());
			}

			this.smsIn = this.calcString(smsInMonth, 0, iSMSIn, false);
			this.smsOut = this.calcString(smsOutMonth, free, iSMSOut, false);

			this.allOldDate = oldDate;

			ret[2] = smsOutMonth;
			ret[3] = free;

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
			if (result[1] > 0) {
				this.pbCalls.setMax(result[1]);
				this.pbCalls.setProgress(result[0]);
				this.pbCalls.setVisibility(View.VISIBLE);
			}
			if (result[3] > 0) {
				this.pbSMS.setProgress(result[2]);
				this.pbSMS.setMax(result[3]);
				this.pbSMS.setVisibility(View.VISIBLE);
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
		this.imeiHash = md5(mTelephonyMgr.getDeviceId());
		prefsNoAds = false;
		for (String h : NO_AD_HASHS) {
			if (this.imeiHash.equals(h)) {
				prefsNoAds = true;
				break;
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
