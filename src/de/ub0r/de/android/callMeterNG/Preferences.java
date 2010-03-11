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

	/** {@link Preference}: merge sms into calls. */
	private Preference prefMergeSMStoCalls = null;
	/** {@link Preference}: merge sms into plan 1. */
	private Preference prefMergeToPlan1 = null;
	/** {@link Preference}: plan1 - cost per call. */
	private Preference prefPlan1CostPerCall = null;
	/** {@link Preference}: plan2 - cost per call. */
	private Preference prefPlan2CostPerCall = null;

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
				.findPreference(Updater.PREFS_MERGE_SMS_TO_CALLS);
		this.prefMergeToPlan1 = this
				.findPreference(Updater.PREFS_MERGE_SMS_PLAN1);
		this.prefPlan1CostPerCall = this
				.findPreference(Updater.PREFS_PLAN1_COST_PER_CALL);
		this.prefPlan2CostPerCall = this
				.findPreference(Updater.PREFS_PLAN2_COST_PER_CALL);
		// run check on create!
		this.onSharedPreferenceChanged(prefs, Updater.PREFS_SPLIT_PLANS);
		this.onSharedPreferenceChanged(prefs, Updater.PREFS_PLAN1_FREEMIN);
		this.onSharedPreferenceChanged(prefs, Updater.PREFS_PLAN2_FREEMIN);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void onSharedPreferenceChanged(
			final SharedPreferences sharedPreferences, final String key) {
		if (key.equals(Updater.PREFS_SPLIT_PLANS)
				|| key.equals(Updater.PREFS_MERGE_PLANS_SMS)
				|| key.equals(Updater.PREFS_MERGE_PLANS_CALLS)
				|| key.equals(Updater.PREFS_MERGE_SMS_TO_CALLS)) {
			final boolean b0 = sharedPreferences.getBoolean(
					Updater.PREFS_SPLIT_PLANS, false);
			final boolean b1 = sharedPreferences.getBoolean(
					Updater.PREFS_MERGE_PLANS_SMS, false);
			final boolean b2 = sharedPreferences.getBoolean(
					Updater.PREFS_MERGE_PLANS_CALLS, false);
			final boolean b3 = sharedPreferences.getBoolean(
					Updater.PREFS_MERGE_SMS_TO_CALLS, false);
			this.prefMergeSMStoCalls.setEnabled(!b0 || b1);
			this.prefMergeToPlan1.setEnabled(b0 && b1 && !b2 && b3);
		} else if (key.equals(Updater.PREFS_PLAN1_FREEMIN)) {
			final String s = sharedPreferences.getString(
					Updater.PREFS_PLAN1_FREEMIN, "");
			this.prefPlan1CostPerCall.setEnabled(s.length() == 0
					|| Integer.parseInt(s) == 0);
		} else if (key.equals(Updater.PREFS_PLAN2_FREEMIN)) {
			final String s = sharedPreferences.getString(
					Updater.PREFS_PLAN2_FREEMIN, "");
			this.prefPlan2CostPerCall.setEnabled(s.length() == 0
					|| Integer.parseInt(s) == 0);
		}
	}
}
