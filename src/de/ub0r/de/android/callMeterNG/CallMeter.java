package de.ub0r.de.android.callMeterNG;

import java.util.Calendar;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.CallLog.Calls;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.admob.android.ads.AdView;

public class CallMeter extends Activity {

	/** Dialog: main. */
	// private static final int DIALOG_MAIN = 0;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 1;

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
		this.updateTime();
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
		final String prefBillMode = this.preferences.getString(PREFS_BILLMODE,
				BILLMODE_1_1);
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

	private void updateTime() {
		// report basics

		Calendar calBillDate = this.getBillDate(Integer
				.parseInt(this.preferences.getString(PREFS_BILLDAY, "0")));
		long billDate = calBillDate.getTimeInMillis();
		((TextView) this.findViewById(R.id.billdate)).setText(DateFormat
				.getDateFormat(this).format(calBillDate.getTime()));

		// report calls
		String[] projection = new String[] { Calls.TYPE, Calls.DURATION,
				Calls.DATE };

		Cursor cur = this.managedQuery(Calls.CONTENT_URI, projection, null,
				null, null);

		int durIn = 0;
		int durOut = 0;
		int durInMonth = 0;
		int durOutMonth = 0;
		if (cur.moveToFirst()) {
			int type;
			final int idType = cur.getColumnIndex(Calls.TYPE);
			final int idDuration = cur.getColumnIndex(Calls.DURATION);
			final int idDate = cur.getColumnIndex(Calls.DATE);
			int t = 0;
			do {
				type = cur.getInt(idType);
				switch (type) {
				case Calls.INCOMING_TYPE:
					t = this.roundTime(cur.getInt(idDuration));
					durIn += t;
					if (billDate <= cur.getLong(idDate)) {
						durInMonth += t;
					}
					break;
				case Calls.OUTGOING_TYPE:
					t = this.roundTime(cur.getInt(idDuration));
					durOut += t;
					if (billDate <= cur.getLong(idDate)) {
						durOutMonth += t;
					}
					break;
				default:
					break;
				}
			} while (cur.moveToNext());

		}

		((TextView) this.findViewById(R.id.in)).setText(this
				.getTime(durInMonth)
				+ " / " + this.getTime(durIn));

		StringBuilder s = new StringBuilder();
		int free = Integer.parseInt(this.preferences.getString(PREFS_FREEMIN,
				"0"));
		if (free != 0) {
			s.append((durOutMonth * 100) / (free * 60));
			s.append("% / ");
		}
		s.append(this.getTime(durOutMonth));
		s.append(" / ");
		s.append(this.getTime(durOut));
		((TextView) this.findViewById(R.id.out)).setText(s.toString());
		s = null;

		// report sms
		if (!this.preferences.getBoolean(PREFS_SMSPERIOD, false)) {
			calBillDate = this.getBillDate(Integer.parseInt(this.preferences
					.getString(PREFS_SMSBILLDAY, "0")));
			billDate = calBillDate.getTimeInMillis();

		}
		((TextView) this.findViewById(R.id.smsbilldate)).setText(DateFormat
				.getDateFormat(this).format(calBillDate.getTime()));
		projection = new String[] { Calls.TYPE, Calls.DATE };
		cur = this.managedQuery(Uri.parse("content://sms"), null, null, null,
				null);

		int smsIn = 0;
		int smsOut = 0;
		int smsInMonth = 0;
		int smsOutMonth = 0;
		if (cur.moveToFirst()) {
			do {
				int type;
				int idType = cur.getColumnIndex(Calls.TYPE);
				int idDate = cur.getColumnIndex(Calls.DATE);
				type = cur.getInt(idType);
				switch (type) {
				case Calls.INCOMING_TYPE:
					++smsIn;
					if (billDate <= cur.getLong(idDate)) {
						++smsInMonth;
					}
					break;
				case Calls.OUTGOING_TYPE:
					++smsOut;
					if (billDate <= cur.getLong(idDate)) {
						++smsOutMonth;
					}
					break;
				default:
					break;
				}
			} while (cur.moveToNext());
		}
		((TextView) this.findViewById(R.id.sms_in)).setText(smsInMonth + " / "
				+ smsIn);

		s = new StringBuilder();
		free = Integer.parseInt(this.preferences.getString(PREFS_FREESMS, "0"));
		if (free != 0) {
			s.append((smsOutMonth * 100) / free);
			s.append("% / ");
		}
		s.append(smsOutMonth);
		s.append(" / ");
		s.append(smsOut);
		((TextView) this.findViewById(R.id.sms_out)).setText(s.toString());
	}
}
