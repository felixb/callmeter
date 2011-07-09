/*
 * Copyright (C) 2009-2011 Felix Bechstein
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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.text.TextUtils;
import android.widget.Toast;
import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Show simple preferences.
 * 
 * @author flx
 */
public final class SimplePreferences extends PreferenceActivity implements
		OnPreferenceChangeListener {
	/** Tag for output. */
	private static final String TAG = "sprefs";

	/** Preference's name: bill mode. */
	private static final String PREFS_BILLMODE = "billmode";
	/** Preference's name: custom bill mode. */
	private static final String PREFS_CUSTOM_BILLMODE = "custom_billmode";

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(R.string.simple_preferences_);
		this.loadPrefs();
		this.addPreferencesFromResource(R.xml.simple_prefs);

		this.findPreference(PREFS_BILLMODE).setOnPreferenceChangeListener(this);
		this.findPreference(PREFS_CUSTOM_BILLMODE)
				.setOnPreferenceChangeListener(this);
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		this.onPreferenceChange(this.findPreference(PREFS_BILLMODE), p
				.getString(PREFS_BILLMODE, "1/1"));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPause() {
		super.onPause();
		this.savePrefs();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPreferenceChange(final Preference preference,
			final Object newValue) {
		if (preference == null) {
			return false;
		}
		RuleMatcher.unmatch(this);
		final String k = preference.getKey();
		if (k.equals(PREFS_BILLMODE)) {
			this.findPreference(PREFS_CUSTOM_BILLMODE).setEnabled(
					!newValue.toString().contains("/"));
			return true;
		} else if (k.equals(PREFS_CUSTOM_BILLMODE)) {
			final String[] t = newValue.toString().split("/");
			if (t.length != 2 || !TextUtils.isDigitsOnly(t[0].trim())
					|| !TextUtils.isDigitsOnly(t[1].trim())) {
				Toast.makeText(this, R.string.missing_slash, Toast.LENGTH_LONG)
						.show();
				return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Load preferences from plans.
	 */
	private void loadPrefs() {
		Log.d(TAG, "loadPrefs()");
		Editor e = PreferenceManager.getDefaultSharedPreferences(this).edit();
		final ContentResolver cr = this.getContentResolver();
		String selection = DataProvider.Plans.ID + "=?";
		Cursor c = cr.query(DataProvider.Plans.CONTENT_URI,
				DataProvider.Plans.PROJECTION, selection,
				new String[] { "16" }, null);
		if (c.moveToFirst()) {
			String billmode = c.getString(DataProvider.Plans.INDEX_BILLMODE);
			Log.d(TAG, "billmode: " + billmode);
			e.putString(PREFS_BILLMODE, billmode);
			e.putString(PREFS_CUSTOM_BILLMODE, billmode);
		}
		c.close();
		e.commit();
	}

	/**
	 * Save preferences to plans.
	 */
	private void savePrefs() {
		Log.d(TAG, "savePrefs()");
		final SharedPreferences p = PreferenceManager
				.getDefaultSharedPreferences(this);
		final ContentResolver cr = this.getContentResolver();
		String selection = DataProvider.Plans.ID + "=?";
		ContentValues cv = new ContentValues();

		// calls
		String s = p.getString(PREFS_BILLMODE, "1/1");
		if (!s.contains("/")) {
			s = p.getString(PREFS_CUSTOM_BILLMODE, "1/1");
			if (!s.contains("/")) {
				s = "1/1";
			}
		}
		cv.put(DataProvider.Plans.BILLMODE, s);
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selection,
				new String[] { "15" });
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selection,
				new String[] { "16" });
	}
}
