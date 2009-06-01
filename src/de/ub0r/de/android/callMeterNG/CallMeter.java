package de.ub0r.de.android.callMeterNG;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class CallMeter extends Activity {

	/** Dialog: main. */
	// private static final int DIALOG_MAIN = 0;
	/** Dialog: about. */
	private static final int DIALOG_ABOUT = 1;
	/** Dialog: settings. */
	private static final int DIALOG_SETTINGS = 2;

	/** Preference's name. */
	public static final String PREFS_NAME = "CallMeterPrefs";
	/** Prefs: name for first day. */
	private static final String PREFS_FIRSTDAY = "fd";
	/** Prefs: first bill day. */
	private static int prefsFirstDay = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.main);
		// Restore preferences
		SharedPreferences settings = this.getSharedPreferences(PREFS_NAME, 0);
		prefsFirstDay = settings.getInt(PREFS_FIRSTDAY, prefsFirstDay);
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
			myDialog.setTitle(this.getResources().getString(R.string.about));
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
		case DIALOG_SETTINGS:
			final CharSequence[] items = { "1", "2", "3", "4", "5", "6", "7",
					"8", "9", "10", "11", "12", "13", "14", "15", "16", "17",
					"18", "19", "20", "21", "22", "23", "24", "25", "26", "27",
					"28", "29", "30", "31" };

			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(this.getResources().getString(
					R.string.pref_startday));
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(final DialogInterface dialog, final int item) {
					Toast.makeText(CallMeter.this.getApplicationContext(),
							items[item], Toast.LENGTH_SHORT).show();
				}
			});
			myDialog = builder.create();
			myDialog
					.setOnDismissListener(new DialogInterface.OnDismissListener() {
						@Override
						public void onDismiss(final DialogInterface di) {
							// TODO: fill me
							// save prefs from TextEdits
							// TODO: get value

							// save prefs to global
							// TODO: prefsFirstDay = value
							// save prefs
							SharedPreferences settings = CallMeter.this
									.getSharedPreferences(PREFS_NAME, 0);
							SharedPreferences.Editor editor = settings.edit();
							editor.putInt(PREFS_FIRSTDAY, prefsFirstDay);
							// commit changes
							editor.commit();
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
			this.showDialog(DIALOG_SETTINGS);

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

	private void updateTime() {
		// Form an array specifying which columns to return.
		String[] projection = new String[] { Calls.TYPE, Calls.DURATION,
				Calls.DATE };

		Cursor cur = this.managedQuery(Calls.CONTENT_URI, projection, null,
				null, null);

		if (cur.moveToFirst()) {
			int durIn = 0;
			int durOut = 0;
			int durInMonth = 0;
			int durOutMonth = 0;
			int type;
			int idType = cur.getColumnIndex(Calls.TYPE);
			int idDuration = cur.getColumnIndex(Calls.DURATION);
			int idDate = cur.getColumnIndex(Calls.DATE);
			int t = 0;
			Calendar cal = Calendar.getInstance();
			if (cal.get(Calendar.DAY_OF_MONTH) < prefsFirstDay) {
				cal.roll(Calendar.MONTH, -1);
			}
			cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
					prefsFirstDay);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			final long billDate = cal.getTimeInMillis();
			do {
				type = cur.getInt(idType);
				switch (type) {
				case Calls.INCOMING_TYPE:
					t = cur.getInt(idDuration);
					durIn += t;
					if (billDate >= cur.getLong(idDate)) {
						durInMonth += t;
					}
					break;
				case Calls.OUTGOING_TYPE:
					t = cur.getInt(idDuration);
					durOut += t;
					if (billDate >= cur.getLong(idDate)) {
						durOutMonth += t;
					}
					break;
				default:
					break;
				}
			} while (cur.moveToNext());

			((TextView) this.findViewById(R.id.in)).setText(this.getResources()
					.getString(R.string.in_)
					+ " " + this.getTime(durIn));
			((TextView) this.findViewById(R.id.out)).setText(this
					.getResources().getString(R.string.out_)
					+ " " + this.getTime(durOut));

			((TextView) this.findViewById(R.id.in_month)).setText(this
					.getResources().getString(R.string.in_month_)
					+ " " + this.getTime(durInMonth));
			((TextView) this.findViewById(R.id.out_month)).setText(this
					.getResources().getString(R.string.out_month_)
					+ " " + this.getTime(durOutMonth));
		}
	}
}

