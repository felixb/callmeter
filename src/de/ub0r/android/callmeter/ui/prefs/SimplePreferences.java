/*
 * Copyright (C) 2009-2013 Felix Bechstein
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

import java.util.Calendar;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

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
public final class SimplePreferences extends SherlockPreferenceActivity implements
		OnPreferenceChangeListener {
	/** Tag for output. */
	private static final String TAG = "sprefs";

	/** Preference's name: bill day. */
	static final String PREFS_BILLDAY = "sp_billday";

	/** Preference's name: bill mode. */
	static final String PREFS_BILLMODE = "sp_billmode";
	/** Preference's name: custom bill mode. */
	static final String PREFS_CUSTOM_BILLMODE = "sp_custom_billmode";
	/** Preference's name: free minutes. */
	static final String PREFS_FREEMIN = "sp_freemin";
	/** Preference's name: free cost per call. */
	static final String PREFS_COST_PER_CALL = "sp_cost_per_call";
	/** Preference's name: free cost per min. */
	static final String PREFS_COST_PER_MIN = "sp_cost_per_min";
	/** Preference's name: free sms. */
	static final String PREFS_FREESMS = "sp_freesms";
	/** Preference's name: free cost per sms. */
	static final String PREFS_COST_PER_SMS = "sp_cost_per_sms";
	/** Preference's name: free mms. */
	static final String PREFS_FREEMMS = "sp_freemms";
	/** Preference's name: free cost per mms. */
	static final String PREFS_COST_PER_MMS = "sp_cost_per_mms";
	/** Preference's name: free MiBi. */
	static final String PREFS_FREEDATA = "sp_freedata";
	/** Preference's name: free cost per MiBi. */
	static final String PREFS_COST_PER_MB = "sp_cost_per_mb";

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);

		loadPrefs(this);
		this.addPreferencesFromResource(R.xml.simple_prefs);
		this.findPreference(PREFS_BILLDAY).setOnPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		savePrefs(this);
	}

	@Override
	public boolean onPreferenceChange(final Preference preference, final Object newValue) {
		if (preference == null) {
			return false;
		}
		RuleMatcher.unmatch(this);
		return true;
	}

	/**
	 * Load preferences from plans.
	 */
	static void loadPrefs(final Context context) {
		Log.d(TAG, "loadPrefs()");
		Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
		final ContentResolver cr = context.getContentResolver();
		String selectionId = DataProvider.Plans.ID + "=?";
		String selectionType = DataProvider.Plans.TYPE + "=?";

		// common
		Cursor c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION,
				selectionType, new String[] { String.valueOf(DataProvider.TYPE_BILLPERIOD) }, null);
		if (c.moveToFirst()) {
			long billday = c.getLong(DataProvider.Plans.INDEX_BILLDAY);
			Log.d(TAG, "billday: " + billday);
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(billday);
			e.putString(PREFS_BILLDAY, cal.get(Calendar.DAY_OF_MONTH) + ".");
		}
		c.close();

		// calls
		c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION, selectionId,
				new String[] { "16" }, null);
		if (c.moveToFirst()) {
			String billmode = c.getString(DataProvider.Plans.INDEX_BILLMODE);
			Log.d(TAG, "billmode: " + billmode);
			e.putString(PREFS_BILLMODE, billmode);
			e.putString(PREFS_CUSTOM_BILLMODE, billmode);
			int i = c.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			if (i == DataProvider.LIMIT_TYPE_UNITS) {
				e.putString(PREFS_FREEMIN, c.getString(DataProvider.Plans.INDEX_LIMIT));
			} else {
				e.putString(PREFS_FREEMIN, "");
			}
			e.putString(PREFS_COST_PER_CALL, c.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
			e.putString(PREFS_COST_PER_MIN, c.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT1));
		}
		c.close();

		// sms
		c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION, selectionId,
				new String[] { "20" }, null);
		if (c.moveToFirst()) {
			int i = c.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			if (i == DataProvider.LIMIT_TYPE_UNITS) {
				e.putString(PREFS_FREESMS, c.getString(DataProvider.Plans.INDEX_LIMIT));
			} else {
				e.putString(PREFS_FREESMS, "");
			}
			e.putString(PREFS_COST_PER_SMS, c.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
		}
		c.close();

		// mms
		c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION, selectionId,
				new String[] { "28" }, null);
		if (c.moveToFirst()) {
			int i = c.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			if (i == DataProvider.LIMIT_TYPE_UNITS) {
				e.putString(PREFS_FREEMMS, c.getString(DataProvider.Plans.INDEX_LIMIT));
			} else {
				e.putString(PREFS_FREEMMS, "");
			}
			e.putString(PREFS_COST_PER_MMS, c.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
		}
		c.close();

		// data
		c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION, selectionType,
				new String[] { String.valueOf(DataProvider.TYPE_DATA) }, null);
		if (c.moveToFirst()) {
			int i = c.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
			if (i == DataProvider.LIMIT_TYPE_UNITS) {
				e.putString(PREFS_FREEDATA, c.getString(DataProvider.Plans.INDEX_LIMIT));
			} else {
				e.putString(PREFS_FREEDATA, "");
			}
			e.putString(PREFS_COST_PER_MB, c.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT1));
		}
		c.close();
		e.commit();
	}

	/**
	 * Save preferences to plans.
	 */
	static void savePrefs(final Context context) {
		Log.d(TAG, "savePrefs()");
		final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		final ContentResolver cr = context.getContentResolver();
		String selectionType = DataProvider.Plans.TYPE + "=?";
		String selectionId = DataProvider.Plans.ID + "=?";
		ContentValues cv = new ContentValues();

		// common
		String s = p.getString(PREFS_BILLDAY, "1").replace(".", "");
		Log.d(TAG, "billday: " + s);
		int i = Utils.parseInt(s, 1);
		Log.d(TAG, "billday: " + i);
		Calendar c = Calendar.getInstance();
		c.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), i, 1, 0, 1);
		if (c.getTimeInMillis() > System.currentTimeMillis()) {
			c.add(Calendar.MONTH, -1);
		}
		Log.d(TAG, "bd: " + DateFormat.getDateFormat(context).format(c.getTime()));
		cv.clear();
		cv.put(DataProvider.Plans.BILLDAY, c.getTimeInMillis());
		cv.put(DataProvider.Plans.BILLPERIOD, DataProvider.BILLPERIOD_1MONTH);
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selectionType,
				new String[] { String.valueOf(DataProvider.TYPE_BILLPERIOD) });

		// calls
		cv.clear();
		s = p.getString(PREFS_BILLMODE, "1/1");
		if (!s.contains("/")) {
			s = p.getString(PREFS_CUSTOM_BILLMODE, "1/1");
			if (!s.contains("/")) {
				s = "1/1";
			}
		}
		cv.put(DataProvider.Plans.BILLMODE, s);
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selectionType,
				new String[] { String.valueOf(DataProvider.TYPE_CALL) });

		cv.clear();
		s = p.getString(PREFS_FREEMIN, "0");
		i = Utils.parseInt(s, 0);
		if (i > 0) {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_UNITS);
		} else {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_NONE);
		}
		cv.put(DataProvider.Plans.LIMIT, i);
		cv.put(DataProvider.Plans.COST_PER_ITEM,
				Utils.parseFloat(p.getString(PREFS_COST_PER_CALL, "0"), 0f));
		float f = Utils.parseFloat(p.getString(PREFS_COST_PER_MIN, "0"), 0f);
		cv.put(DataProvider.Plans.COST_PER_AMOUNT1, f);
		cv.put(DataProvider.Plans.COST_PER_AMOUNT2, f);
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selectionId, new String[] { "16" });

		// sms
		cv.clear();
		s = p.getString(PREFS_FREESMS, "0");
		i = Utils.parseInt(s, 0);
		if (i > 0) {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_UNITS);
		} else {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_NONE);
		}
		cv.put(DataProvider.Plans.LIMIT, i);
		cv.put(DataProvider.Plans.COST_PER_ITEM,
				Utils.parseFloat(p.getString(PREFS_COST_PER_SMS, "0"), 0f));
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selectionId, new String[] { "20" });

		// mms
		cv.clear();
		s = p.getString(PREFS_FREEMMS, "0");
		i = Utils.parseInt(s, 0);
		if (i > 0) {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_UNITS);
		} else {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_NONE);
		}
		cv.put(DataProvider.Plans.LIMIT, i);
		cv.put(DataProvider.Plans.COST_PER_ITEM,
				Utils.parseFloat(p.getString(PREFS_COST_PER_MMS, "0"), 0f));
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selectionId, new String[] { "28" });

		// data
		cv.clear();
		s = p.getString(PREFS_FREEDATA, "0");
		i = Utils.parseInt(s, 0);
		if (i > 0) {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_UNITS);
		} else {
			cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_NONE);
		}
		cv.put(DataProvider.Plans.LIMIT, i);
		f = Utils.parseFloat(p.getString(PREFS_COST_PER_MB, "0"), 0f);
		cv.put(DataProvider.Plans.COST_PER_AMOUNT1, f);
		cv.put(DataProvider.Plans.COST_PER_AMOUNT2, f);
		cr.update(DataProvider.Plans.CONTENT_URI, cv, selectionType,
				new String[] { String.valueOf(DataProvider.TYPE_DATA) });
	}
}
