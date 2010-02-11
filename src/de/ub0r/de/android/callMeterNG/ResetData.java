/**
 * 
 */
package de.ub0r.de.android.callMeterNG;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

/**
 * @author flx
 */
public class ResetData extends Activity {

	/**
	 * Delete saved data from prefs.
	 * 
	 * @param c
	 *            context
	 */
	static void resetData(final Context c) {
		final Editor edt = PreferenceManager.getDefaultSharedPreferences(c)
				.edit();
		edt.remove(Updater.PREFS_CALLS_PERIOD_IN);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT1);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT1_COUNT);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT2);
		edt.remove(Updater.PREFS_CALLS_PERIOD_OUT2_COUNT);
		edt.remove(Updater.PREFS_CALLS_ALL_IN);
		edt.remove(Updater.PREFS_CALLS_ALL_OUT);
		edt.remove(Updater.PREFS_CALLS_PERIOD_LASTCHECK);
		edt.remove(Updater.PREFS_CALLS_WALK_LASTCHECK);
		edt.remove(Updater.PREFS_SMS_PERIOD_IN);
		edt.remove(Updater.PREFS_SMS_PERIOD_OUT1);
		edt.remove(Updater.PREFS_SMS_PERIOD_OUT2);
		edt.remove(Updater.PREFS_SMS_ALL_IN);
		edt.remove(Updater.PREFS_SMS_ALL_OUT);
		edt.remove(Updater.PREFS_SMS_PERIOD_LASTCHECK);
		edt.remove(Updater.PREFS_SMS_WALK_LASTCHECK);
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
				ResetData.resetData(ResetData.this);
				ResetData.this.finish();
			}
		});
		builder.setNegativeButton(android.R.string.no, new OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				ResetData.this.finish();
			}
		});
		builder.create().show();
	}
}
