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
		edt.putInt(CallMeter.PREFS_ALL_CALLS_IN, 0);
		edt.putInt(CallMeter.PREFS_ALL_CALLS_OUT, 0);
		edt.putInt(CallMeter.PREFS_ALL_SMS_IN, 0);
		edt.putInt(CallMeter.PREFS_ALL_SMS_OUT, 0);
		edt.putLong(CallMeter.PREFS_DATE_OLD, 0);
		edt.commit();
	}

	@Override
	protected final void onResume() {
		super.onResume();
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.reset_data_);
		builder.setMessage(R.string.reset_data_hint);
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
