/**
 * 
 */
package de.ub0r.de.android.callMeterNG;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * Activity to reset statistics.
 * 
 * @author flx
 */
public class ResetData extends Activity {
	/**
	 * Delete saved data from preferences.
	 * 
	 * @param c
	 *            {@link Context}
	 * @param resetDataStats
	 *            reset data statistics too
	 */
	static void resetData(final Context c, final boolean resetDataStats) {
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(c);
		final Editor edt = prefs.edit();
		edt.remove(Updater.PREFS_CALLS_PERIOD_IN1);
		edt.remove(Updater.PREFS_CALLS_PERIOD_IN1_COUNT);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT1);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT1_COUNT);
		edt.remove(Updater.PREFS_CALLS_PERIOD_IN2);
		edt.remove(Updater.PREFS_CALLS_PERIOD_IN2_COUNT);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT2);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT2_COUNT);
		edt.remove(Updater.PREFS_CALLS_ALL_IN);
		edt.remove(Updater.PREFS_CALLS_ALL_OUT);
		edt.remove(Updater.PREFS_CALLS_PERIOD_LASTCHECK);
		edt.remove(Updater.PREFS_CALLS_WALK_LASTCHECK);
		edt.remove(Updater.PREFS_SMS_PERIOD_IN1);
		edt.remove(Updater.PREFS_SMS_PERIOD_IN2);
		edt.remove(Updater.PREFS_SMS_PERIOD_OUT1);
		edt.remove(Updater.PREFS_SMS_PERIOD_OUT2);
		edt.remove(Updater.PREFS_SMS_ALL_IN);
		edt.remove(Updater.PREFS_SMS_ALL_OUT);
		edt.remove(Updater.PREFS_SMS_PERIOD_LASTCHECK);
		edt.remove(Updater.PREFS_SMS_WALK_LASTCHECK);
		if (resetDataStats) {
			long runningIn = prefs
					.getLong(UpdaterData.PREFS_DATA_RUNNING_IN, 0);
			long runningOut = prefs.getLong(UpdaterData.PREFS_DATA_RUNNING_OUT,
					0);
			edt.putLong(UpdaterData.PREFS_DATA_BOOT_IN, 0);
			edt.putLong(UpdaterData.PREFS_DATA_BOOT_OUT, 0);
			edt.putLong(UpdaterData.PREFS_DATA_PREBILLING_IN, runningIn);
			edt.putLong(UpdaterData.PREFS_DATA_PREBILLING_OUT, runningOut);
		}
		edt.commit();
	}

	@Override
	protected final void onResume() {
		super.onResume();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.reset_data_);
		builder.setMessage(R.string.reset_data_hint);
		builder.setCancelable(false);
		builder.setPositiveButton(android.R.string.yes, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				ResetData.resetData(ResetData.this, false);
				ResetData.this.finish();
			}
		});
		builder.setNeutralButton(R.string.reset_data_data_,
				new OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog,
							final int which) {
						ResetData.resetData(ResetData.this, true);
						ResetData.this.finish();
					}
				});
		builder.setNegativeButton(android.R.string.no, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				ResetData.this.finish();
			}
		});
		builder.show();
	}
}
