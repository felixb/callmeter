package de.ub0r.de.android.callMeterNG;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

/**
 * Preferences.
 * 
 * @author flx
 */
public class Preferences extends PreferenceActivity implements
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
		final SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		this.prefMergeSMStoCalls = this
				.findPreference(CallMeter.PREFS_MERGE_SMS_TO_CALLS);
		this.prefMergeToPlan1 = this
				.findPreference(CallMeter.PREFS_MERGE_SMS_PLAN1);
		// run check on create!
		this.onSharedPreferenceChanged(prefs, CallMeter.PREFS_SPLIT_PLANS);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onSharedPreferenceChanged(
			final SharedPreferences sharedPreferences, final String key) {
		if (key.equals(CallMeter.PREFS_SPLIT_PLANS)
				|| key.equals(CallMeter.PREFS_MERGE_PLANS_SMS)
				|| key.equals(CallMeter.PREFS_MERGE_PLANS_CALLS)
				|| key.equals(CallMeter.PREFS_MERGE_SMS_TO_CALLS)) {
			final boolean b0 = sharedPreferences.getBoolean(
					CallMeter.PREFS_SPLIT_PLANS, false);
			final boolean b1 = sharedPreferences.getBoolean(
					CallMeter.PREFS_MERGE_PLANS_SMS, false);
			final boolean b2 = sharedPreferences.getBoolean(
					CallMeter.PREFS_MERGE_PLANS_CALLS, false);
			final boolean b3 = sharedPreferences.getBoolean(
					CallMeter.PREFS_MERGE_SMS_TO_CALLS, false);
			this.prefMergeSMStoCalls.setEnabled(!b0 || b1);
			this.prefMergeToPlan1.setEnabled(b0 && b1 && !b2 && b3);
		}
	}
}
