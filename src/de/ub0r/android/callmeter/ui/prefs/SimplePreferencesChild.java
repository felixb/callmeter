/*
 * Copyright (C) 2009-2012 Felix Bechstein
 * 
 * This file is part of Call Meter 3G.
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
package de.ub0r.android.callmeter.ui.prefs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.Utils;

/**
 * Show simple preferences.
 * 
 * @author flx
 */
public final class SimplePreferencesChild extends SherlockPreferenceActivity implements
		OnPreferenceChangeListener {

	/** Action. */
	private static final String CALLS = "CALLS";
	/** Action. */
	private static final String SMS = "SMS";
	/** Action. */
	private static final String MMS = "MMS";
	/** Action. */
	private static final String DATA = "DATA";

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		SimplePreferences.loadPrefs(this);

		String a = this.getIntent().getAction();
		if (a == null) {
			this.finish();
			return;
		} else if (CALLS.equals(a)) {
			this.addPreferencesFromResource(R.xml.simple_prefs_calls);
			this.findPreference(SimplePreferences.PREFS_BILLMODE).setOnPreferenceChangeListener(
					this);
			this.findPreference(SimplePreferences.PREFS_CUSTOM_BILLMODE)
					.setOnPreferenceChangeListener(this);
			this.findPreference(SimplePreferences.PREFS_FREEMIN)
					.setOnPreferenceChangeListener(this);
			this.findPreference(SimplePreferences.PREFS_COST_PER_CALL)
					.setOnPreferenceChangeListener(this);
			this.findPreference(SimplePreferences.PREFS_COST_PER_MIN)
					.setOnPreferenceChangeListener(this);
			final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
			this.onPreferenceChange(this.findPreference(SimplePreferences.PREFS_BILLMODE),
					p.getString(SimplePreferences.PREFS_BILLMODE, "1/1"));
		} else if (SMS.equals(a)) {
			this.addPreferencesFromResource(R.xml.simple_prefs_sms);
			this.findPreference(SimplePreferences.PREFS_FREESMS)
					.setOnPreferenceChangeListener(this);
			this.findPreference(SimplePreferences.PREFS_COST_PER_SMS)
					.setOnPreferenceChangeListener(this);
		} else if (MMS.equals(a)) {
			this.addPreferencesFromResource(R.xml.simple_prefs_mms);
			this.findPreference(SimplePreferences.PREFS_FREEMMS)
					.setOnPreferenceChangeListener(this);
			this.findPreference(SimplePreferences.PREFS_COST_PER_MMS)
					.setOnPreferenceChangeListener(this);
		} else if (DATA.equals(a)) {
			this.addPreferencesFromResource(R.xml.simple_prefs_data);
			this.findPreference(SimplePreferences.PREFS_FREEDATA).setOnPreferenceChangeListener(
					this);
			this.findPreference(SimplePreferences.PREFS_COST_PER_MB).setOnPreferenceChangeListener(
					this);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		SimplePreferences.savePrefs(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean onPreferenceChange(final Preference preference, final Object newValue) {
		if (preference == null) {
			return false;
		}
		RuleMatcher.unmatch(this);
		final String k = preference.getKey();
		if (k.equals(SimplePreferences.PREFS_BILLMODE)) {
			this.findPreference(SimplePreferences.PREFS_CUSTOM_BILLMODE).setEnabled(
					!newValue.toString().contains("/"));
		} else if (k.equals(SimplePreferences.PREFS_CUSTOM_BILLMODE)) {
			final String[] t = newValue.toString().split("/");
			if (t.length != 2 || !TextUtils.isDigitsOnly(t[0].trim())
					|| !TextUtils.isDigitsOnly(t[1].trim())) {
				Toast.makeText(this, R.string.missing_slash, Toast.LENGTH_LONG).show();
				return false;
			}
		}
		return true;
	}
}
