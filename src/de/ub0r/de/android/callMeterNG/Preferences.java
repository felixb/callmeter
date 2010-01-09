/*
 * Copyright (C) 2010 Felix Bechstein
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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;

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
	/** Preference: bill excluded people in plan1. */
	private Preference prefExcludePeopleToPlan1 = null;
	/** Preference: bill excluded people in plan2. */
	private Preference prefExcludePeopleToPlan2 = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.addPreferencesFromResource(R.xml.prefs);
		CallMeter.preferences.registerOnSharedPreferenceChangeListener(this);
		this.prefMergeSMStoCalls = this
				.findPreference(CallMeter.PREFS_MERGE_SMS_TO_CALLS);
		this.prefMergeToPlan1 = this
				.findPreference(CallMeter.PREFS_MERGE_SMS_PLAN1);
		this.prefExcludePeopleToPlan1 = this
				.findPreference(CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN1);
		this.prefExcludePeopleToPlan2 = this
				.findPreference(CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN2);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected final void onResume() {
		super.onResume();
		// run check on resume!
		this.onSharedPreferenceChanged(CallMeter.preferences,
				CallMeter.PREFS_SPLIT_PLANS);
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
		if (key.equals(CallMeter.PREFS_SPLIT_PLANS)
				|| key.equals(CallMeter.PREFS_MERGE_PLANS_CALLS)
				|| key.equals(CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN1)
				|| key.equals(CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN2)) {
			final boolean b0 = sharedPreferences.getBoolean(
					CallMeter.PREFS_SPLIT_PLANS, false);
			final boolean b1 = sharedPreferences.getBoolean(
					CallMeter.PREFS_MERGE_PLANS_CALLS, false);
			if (!b0 || b1) {
				this.prefExcludePeopleToPlan1.setEnabled(false);
				this.prefExcludePeopleToPlan2.setEnabled(false);
			} else {
				// check crosswise
				final boolean a1 = sharedPreferences.getBoolean(
						CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN1, false);
				final boolean a2 = sharedPreferences.getBoolean(
						CallMeter.PREFS_EXCLUDE_PEOPLE_PLAN2, false);
				this.prefExcludePeopleToPlan1
						.setEnabled(!(a2 && this.prefExcludePeopleToPlan2
								.isEnabled()));
				this.prefExcludePeopleToPlan2
						.setEnabled(!(a1 && this.prefExcludePeopleToPlan1
								.isEnabled()));
			}

		}
	}
}
