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

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.admob.android.ads.AdView;

public class CallMeter extends Activity {
	/** Tag for output. */
	// private static final String TAG = "CallMeterNG";
	/** Dialog: main. */
	// private static final int DIALOG_MAIN = 0;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 1;

	/** Days of a week. */
	private static final int DAYS_WEEK = 7;
	/** Hours of a day. */
	private static final int HOURS_DAY = 24;

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

	/** Prefs: billmode: 1/1. */
	private static final String BILLMODE_1_1 = "1_1";
	/** Prefs: billmode: 10/10. */
	private static final String BILLMODE_10_10 = "10_10";
	/** Prefs: billmode: 60/1. */
	private static final String BILLMODE_60_1 = "60_1";
	/** Prefs: billmode: 60/10. */
	private static final String BILLMODE_60_10 = "60_10";
	/** Prefs: billmode: 60/60. */
	private static final String BILLMODE_60_60 = "60_60";

	private SharedPreferences preferences;

	/**
	 * Preferences.
	 * 
	 * @author flx
	 */
	public static class Preferences extends PreferenceActivity {
		public final void onCreate(final Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			this.addPreferencesFromResource(R.xml.prefs);
		}
	}

	private class Updater extends AsyncTask<Boolean, Integer, Boolean> {
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

		private int roundTime(final int time) {
			final String prefBillMode = CallMeter.this.preferences.getString(
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

		private void updateText() {
			this.twCallsBillDate.setText(this.callsBillDate);
			this.twCallsIn.setText(this.callsIn);
			this.twCallsOut.setText(this.callsOut);

			this.twSMSBillDate.setText(this.smsBillDate);
			this.twSMSIn.setText(this.smsIn);
			this.twSMSOut.setText(this.smsOut);
		}

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

		@Override
		protected void onPreExecute() {
			// load old values from database
			this.allCallsIn = CallMeter.this.preferences.getInt(
					PREFS_ALL_CALLS_IN, 0);
			this.allCallsOut = CallMeter.this.preferences.getInt(
					PREFS_ALL_CALLS_OUT, 0);
			this.allSMSIn = CallMeter.this.preferences.getInt(PREFS_ALL_SMS_IN,
					0);
			this.allSMSOut = CallMeter.this.preferences.getInt(
					PREFS_ALL_SMS_OUT, 0);
			this.allOldDate = CallMeter.this.preferences.getLong(
					PREFS_DATE_OLD, 0);

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

		@Override
		protected Boolean doInBackground(final Boolean... arg0) {
			// report basics
			Calendar calBillDate = this.getBillDate(Integer
					.parseInt(CallMeter.this.preferences.getString(
							PREFS_BILLDAY, "0")));
			long billDate = calBillDate.getTimeInMillis();
			final long oldDate = this.getOldDate();

			this.callsBillDate = DateFormat.getDateFormat(CallMeter.this)
					.format(calBillDate.getTime());

			// get not free days/timeslots
			final boolean freeDaysCalls = CallMeter.this.preferences
					.getBoolean(PREFS_FREEDAYS_CALL, false);
			final boolean freeDaysSMS = CallMeter.this.preferences.getBoolean(
					PREFS_FREEDAYS_SMS, false);
			boolean[] freeDays = null;
			if (freeDaysCalls || freeDaysSMS) {
				freeDays = new boolean[DAYS_WEEK];
				for (int i = 0; i < DAYS_WEEK; i++) {
					freeDays[i] = CallMeter.this.preferences.getBoolean(
							PREFS_FREEDAYS_ + (i + 1), false);
				}
			}
			final boolean freeHoursCalls = CallMeter.this.preferences
					.getBoolean(PREFS_FREEHOURS_CALL, false);
			final boolean freeHoursSMS = CallMeter.this.preferences.getBoolean(
					PREFS_FREEHOURS_SMS, false);
			boolean[] freeHours = null;
			if (freeHoursCalls || freeHoursSMS) {
				freeHours = new boolean[HOURS_DAY];
				for (int i = 0; i < HOURS_DAY; i++) {
					freeHours[i] = CallMeter.this.preferences.getBoolean(
							PREFS_FREEHOURS_ + i, false);
				}
			}

			Integer[] status = { 0, 0, 1, 1 };

			// report calls
			String[] projection = new String[] { Calls.TYPE, Calls.DURATION,
					Calls.DATE };

			Cursor cur = CallMeter.this.managedQuery(Calls.CONTENT_URI,
					projection, Calls.DATE + " >= " + this.allOldDate, null,
					Calls.DATE + " DESC");

			int durIn = this.allCallsIn;
			int durOut = this.allCallsOut;
			int durInMonth = 0;
			int durOutMonth = 0;
			int free = Integer.parseInt(CallMeter.this.preferences.getString(
					PREFS_FREEMIN, "0"));

			if (cur.moveToFirst()) {
				status[2] = cur.getCount();
				int type;
				long d;
				final int idType = cur.getColumnIndex(Calls.TYPE);
				final int idDuration = cur.getColumnIndex(Calls.DURATION);
				final int idDate = cur.getColumnIndex(Calls.DATE);
				int t = 0;
				int i = 0;
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
								durOutMonth += this.roundTime(t);
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
						status[0] = (i * 100) / status[2];
						this.callsIn = this.calcString(durInMonth, 0, durIn,
								true);
						this.callsOut = this.calcString(durOutMonth, free,
								durOut, true);
						this.publishProgress(status);
					}
				} while (cur.moveToNext());
			}
			this.callsIn = this.calcString(durInMonth, 0, durIn, true);
			this.callsOut = this.calcString(durOutMonth, free, durOut, true);

			status[0] = 100;
			this.publishProgress(status);

			// report sms
			if (!CallMeter.this.preferences.getBoolean(PREFS_SMSPERIOD, false)) {
				calBillDate = this.getBillDate(Integer
						.parseInt(CallMeter.this.preferences.getString(
								PREFS_SMSBILLDAY, "0")));
				billDate = calBillDate.getTimeInMillis();

			}
			this.smsBillDate = DateFormat.getDateFormat(CallMeter.this).format(
					calBillDate.getTime());
			projection = new String[] { Calls.TYPE, Calls.DATE };
			cur = CallMeter.this.managedQuery(Uri.parse("content://sms"),
					projection, Calls.DATE + " >= " + this.allOldDate, null,
					Calls.DATE + " DESC");
			free = Integer.parseInt(CallMeter.this.preferences.getString(
					PREFS_FREESMS, "0"));
			int smsIn = this.allSMSIn;
			int smsOut = this.allSMSOut;
			int smsInMonth = 0;
			int smsOutMonth = 0;
			if (cur.moveToFirst()) {
				status[3] = cur.getCount();
				int type;
				long d;
				final int idType = cur.getColumnIndex(Calls.TYPE);
				final int idDate = cur.getColumnIndex(Calls.DATE);
				int i = 0;
				do {
					type = cur.getInt(idType);
					d = cur.getLong(idDate);
					switch (type) {
					case Calls.INCOMING_TYPE:
						++smsIn;
						if (billDate <= d) {
							++smsInMonth;
						} else if (d < oldDate) {
							++this.allSMSIn;
						}
						break;
					case Calls.OUTGOING_TYPE:
						++smsOut;
						if (billDate <= d) {
							if (this.isBilled(freeDaysSMS, freeDays,
									freeHoursSMS, freeHours, d)) {
								++smsOutMonth;
							}
						} else if (d < oldDate) {
							++this.allSMSOut;
						}
						break;
					default:
						break;
					}
					++i;
					if (i % 50 == 1) {
						status[1] = (i * 100) / status[3];
						this.smsIn = this.calcString(smsInMonth, 0, smsIn,
								false);
						this.smsOut = this.calcString(smsOutMonth, free,
								smsOut, false);
						this.publishProgress(status);
					}
				} while (cur.moveToNext());
			}

			this.smsIn = this.calcString(smsInMonth, 0, smsIn, false);
			this.smsOut = this.calcString(smsOutMonth, free, smsOut, false);

			this.allOldDate = oldDate;

			return null;
		}

		/**
		 * Update progress.
		 * 
		 * @param progress
		 *            progress
		 */
		@Override
		protected final void onProgressUpdate(final Integer... progress) {
			this.pbCalls.setProgress(progress[0]);
			this.pbSMS.setProgress(progress[1]);
			this.updateText();
		}

		/**
		 * Push data back to GUI. Hide progressbars.
		 * 
		 * @param result
		 *            result
		 */
		@Override
		protected final void onPostExecute(final Boolean result) {
			this.pbCalls.setVisibility(View.INVISIBLE);
			this.pbSMS.setVisibility(View.GONE);
			this.updateText();

			// save old values to database
			SharedPreferences.Editor editor = CallMeter.this.preferences.edit();
			editor.putInt(PREFS_ALL_CALLS_IN, this.allCallsIn);
			editor.putInt(PREFS_ALL_CALLS_OUT, this.allCallsOut);
			editor.putInt(PREFS_ALL_SMS_IN, this.allSMSIn);
			editor.putInt(PREFS_ALL_SMS_OUT, this.allSMSOut);
			editor.putLong(PREFS_DATE_OLD, this.allOldDate);
			editor.commit();
		}
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
		// get prefs.
		this.preferences = PreferenceManager.getDefaultSharedPreferences(this);
	}

	/** Called on Activity resume. */
	@Override
	protected final void onResume() {
		super.onResume();
		((AdView) this.findViewById(R.id.ad)).setVisibility(View.VISIBLE);
		// get calls
		new Updater().execute((Boolean[]) null);
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
		Dialog myDialog;
		switch (id) {
		case DIALOG_ABOUT:
			myDialog = new Dialog(this);
			myDialog.setContentView(R.layout.about);
			myDialog.setTitle(this.getResources().getString(R.string.about_)
					+ " v"
					+ this.getResources().getString(R.string.app_version));
			Button button = (Button) myDialog.findViewById(R.id.btn_donate);
			button.setOnClickListener(new OnClickListener() {
				public void onClick(final View view) {
					Uri uri = Uri.parse(CallMeter.this
							.getString(R.string.donate_url));
					CallMeter.this.startActivity(new Intent(Intent.ACTION_VIEW,
							uri));
				}
			});
			break;
		default:
			myDialog = null;
		}
		return myDialog;
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
		default:
			return false;
		}
	}
}
