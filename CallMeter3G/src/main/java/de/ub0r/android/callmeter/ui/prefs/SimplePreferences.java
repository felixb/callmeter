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

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import java.util.Calendar;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.TrackingSherlockPreferenceActivity;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Show simple preferences.
 *
 * @author flx
 */
public final class SimplePreferences extends TrackingSherlockPreferenceActivity implements
        OnPreferenceChangeListener {

    /** Tag for output. */
    private static final String TAG = "sprefs";

    /** Preference's name: bill day. */
    static final String PREFS_BILLDAY = "sp_billday";

    /** Preference's name: bill mode. */
    static final String PREFS_BILLMODE = "sp_billmode";
    /** Preference's name: bill mode. */
    static final String PREFS_BILLMODE_2 = "sp_billmode_2";
    /** Preference's name: bill mode. */
    static final String PREFS_BILLMODE_VOIP = "sp_billmode_voip";
    /** Preference's name: custom bill mode. */
    static final String PREFS_CUSTOM_BILLMODE = "sp_custom_billmode";
    /** Preference's name: custom bill mode. */
    static final String PREFS_CUSTOM_BILLMODE_2 = "sp_custom_billmode_2";
    /** Preference's name: custom bill mode. */
    static final String PREFS_CUSTOM_BILLMODE_VOIP = "sp_custom_billmode_voip";
    /** Preference's name: free minutes. */
    static final String PREFS_FREEMIN = "sp_freemin";
    /** Preference's name: free minutes. */
    static final String PREFS_FREEMIN_2 = "sp_freemin_2";
    /** Preference's name: free minutes. */
    static final String PREFS_FREEMIN_VOIP = "sp_freemin_voip";
    /** Preference's name: free cost per call. */
    static final String PREFS_COST_PER_CALL = "sp_cost_per_call";
    /** Preference's name: free cost per call. */
    static final String PREFS_COST_PER_CALL_2 = "sp_cost_per_call_2";
    /** Preference's name: free cost per call. */
    static final String PREFS_COST_PER_CALL_VOIP = "sp_cost_per_call_voip";
    /** Preference's name: free cost per min. */
    static final String PREFS_COST_PER_MIN = "sp_cost_per_min";
    /** Preference's name: free cost per min. */
    static final String PREFS_COST_PER_MIN_2 = "sp_cost_per_min_2";
    /** Preference's name: free cost per min. */
    static final String PREFS_COST_PER_MIN_VOIP = "sp_cost_per_min_voip";
    /** Preference's name: free sms. */
    static final String PREFS_FREESMS = "sp_freesms";
    /** Preference's name: free sms. */
    static final String PREFS_FREESMS_2 = "sp_freesms_2";
    /** Preference's name: free sms. */
    static final String PREFS_FREESMS_WEBSMS = "sp_freesms_websms";
    /** Preference's name: free cost per sms. */
    static final String PREFS_COST_PER_SMS = "sp_cost_per_sms";
    /** Preference's name: free cost per sms. */
    static final String PREFS_COST_PER_SMS_2 = "sp_cost_per_sms_2";
    /** Preference's name: free cost per sms. */
    static final String PREFS_COST_PER_SMS_WEBSMS = "sp_cost_per_sms_websms";
    /** Preference's name: free mms. */
    static final String PREFS_FREEMMS = "sp_freemms";
    /** Preference's name: free cost per mms. */
    static final String PREFS_COST_PER_MMS = "sp_cost_per_mms";
    /** Preference's name: free MiBi. */
    static final String PREFS_FREEDATA = "sp_freedata";
    /** Preference's name: free cost per MiBi. */
    static final String PREFS_COST_PER_MB = "sp_cost_per_mb";

    private static final String SELECTION_ID = DataProvider.Plans.ID + "=?";
    private static final String SELECTION_TYPE = DataProvider.Plans.TYPE + "=?";

    private void removePreferenceIfPlanMissing(final ContentResolver cr, final PreferenceGroup pg,
            final String key, final long planId) {
        Preference p = findPreference(key);
        if (p == null) {
            return;
        }
        Cursor c = cr.query(DataProvider.Plans.CONTENT_URI, new String[]{DataProvider.Plans.ID},
                SELECTION_ID, new String[]{String.valueOf(planId)}, null);
        if (c.getCount() == 0) {
            pg.removePreference(p);
        }
        c.close();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);

        loadPrefs(this);
        addPreferencesFromResource(R.xml.simple_prefs);
        findPreference(PREFS_BILLDAY).setOnPreferenceChangeListener(this);

        // remove prefs for dual sim, websms, voip if not available
        PreferenceGroup pg = (PreferenceGroup) findPreference("container");
        ContentResolver cr = getContentResolver();
        removePreferenceIfPlanMissing(cr, pg, "calls_2", 31);
        removePreferenceIfPlanMissing(cr, pg, "calls_voip", 30);
        removePreferenceIfPlanMissing(cr, pg, "sms_2", 34);
        removePreferenceIfPlanMissing(cr, pg, "sms_websms", 29);
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

    private static void loadPrefsCall(final Editor e, final ContentResolver cr, final long planId,
            final String postfix) {
        Cursor c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION,
                SELECTION_ID, new String[]{String.valueOf(planId)}, null);
        if (c.moveToFirst()) {
            String billmode = c.getString(DataProvider.Plans.INDEX_BILLMODE);
            Log.d(TAG, "billmode: " + billmode);
            e.putString(PREFS_BILLMODE + postfix, billmode);
            e.putString(PREFS_CUSTOM_BILLMODE + postfix, billmode);
            int i = c.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
            if (i == DataProvider.LIMIT_TYPE_UNITS) {
                e.putString(PREFS_FREEMIN + postfix, c.getString(DataProvider.Plans.INDEX_LIMIT));
            } else {
                e.putString(PREFS_FREEMIN + postfix, "");
            }
            e.putString(PREFS_COST_PER_CALL + postfix,
                    c.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
            e.putString(PREFS_COST_PER_MIN + postfix,
                    c.getString(DataProvider.Plans.INDEX_COST_PER_AMOUNT1));
        }
        c.close();
    }

    private static void loadPrefsSMS(final Editor e, final ContentResolver cr, final long planId,
            final String postfix) {
        Cursor c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION,
                SELECTION_ID, new String[]{String.valueOf(planId)}, null);
        if (c.moveToFirst()) {
            int i = c.getInt(DataProvider.Plans.INDEX_LIMIT_TYPE);
            if (i == DataProvider.LIMIT_TYPE_UNITS) {
                e.putString(PREFS_FREESMS + postfix, c.getString(DataProvider.Plans.INDEX_LIMIT));
            } else {
                e.putString(PREFS_FREESMS + postfix, "");
            }
            e.putString(PREFS_COST_PER_SMS + postfix,
                    c.getString(DataProvider.Plans.INDEX_COST_PER_ITEM));
        }
        c.close();
    }

    /**
     * Load preferences from plans.
     */
    static void loadPrefs(final Context context) {
        Log.d(TAG, "loadPrefs()");
        Editor e = PreferenceManager.getDefaultSharedPreferences(context).edit();
        ContentResolver cr = context.getContentResolver();

        // common
        Cursor c = cr
                .query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION,
                        SELECTION_TYPE,
                        new String[]{String.valueOf(DataProvider.TYPE_BILLPERIOD)}, null);
        if (c.moveToFirst()) {
            long billday = c.getLong(DataProvider.Plans.INDEX_BILLDAY);
            Log.d(TAG, "billday: " + billday);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(billday);
            e.putString(PREFS_BILLDAY, cal.get(Calendar.DAY_OF_MONTH) + ".");
        }
        c.close();

        // calls
        loadPrefsCall(e, cr, 16, "");
        loadPrefsCall(e, cr, 31, "_2");
        loadPrefsCall(e, cr, 30, "_voip");

        // sms
        loadPrefsSMS(e, cr, 20, "");
        loadPrefsSMS(e, cr, 34, "_2");
        loadPrefsSMS(e, cr, 29, "_websms");

        // mms
        c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION, SELECTION_ID,
                new String[]{"28"}, null);
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
        c = cr.query(DataProvider.Plans.CONTENT_URI, DataProvider.Plans.PROJECTION, SELECTION_TYPE,
                new String[]{String.valueOf(DataProvider.TYPE_DATA)}, null);
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

    private static void savePrefsCall(final SharedPreferences p, final ContentResolver cr,
            final long planId, final String postfix) {
        ContentValues cv = new ContentValues();
        String s = p.getString(PREFS_BILLMODE + postfix, "1/1");
        if (!s.contains("/")) {
            s = p.getString(PREFS_CUSTOM_BILLMODE + postfix, "1/1");
            if (!s.contains("/")) {
                s = "1/1";
            }
        }
        cv.put(DataProvider.Plans.BILLMODE, s);

        s = p.getString(PREFS_FREEMIN + postfix, "0");
        int i = Utils.parseInt(s, 0);
        if (i > 0) {
            cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_UNITS);
        } else {
            cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_NONE);
        }
        cv.put(DataProvider.Plans.LIMIT, i);
        cv.put(DataProvider.Plans.COST_PER_ITEM,
                Utils.parseFloat(p.getString(PREFS_COST_PER_CALL + postfix, "0"), 0f));
        float f = Utils.parseFloat(p.getString(PREFS_COST_PER_MIN + postfix, "0"), 0f);
        cv.put(DataProvider.Plans.COST_PER_AMOUNT1, f);
        cv.put(DataProvider.Plans.COST_PER_AMOUNT2, f);

        cr.update(DataProvider.Plans.CONTENT_URI, cv, SELECTION_ID,
                new String[]{String.valueOf(planId)});
    }

    private static void savePrefsSMS(final SharedPreferences p, final ContentResolver cr,
            final long planId, final String postfix) {
        ContentValues cv = new ContentValues();
        String s = p.getString(PREFS_FREESMS + postfix, "0");
        int i = Utils.parseInt(s, 0);
        if (i > 0) {
            cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_UNITS);
        } else {
            cv.put(DataProvider.Plans.LIMIT_TYPE, DataProvider.LIMIT_TYPE_NONE);
        }
        cv.put(DataProvider.Plans.LIMIT, i);
        cv.put(DataProvider.Plans.COST_PER_ITEM,
                Utils.parseFloat(p.getString(PREFS_COST_PER_SMS + postfix, "0"), 0f));
        cr.update(DataProvider.Plans.CONTENT_URI, cv, SELECTION_ID,
                new String[]{String.valueOf(planId)});
    }

    /**
     * Save preferences to plans.
     */
    static void savePrefs(final Context context) {
        Log.d(TAG, "savePrefs()");
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        ContentResolver cr = context.getContentResolver();
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
        cr.update(DataProvider.Plans.CONTENT_URI, cv, SELECTION_TYPE,
                new String[]{String.valueOf(DataProvider.TYPE_BILLPERIOD)});

        // calls
        savePrefsCall(p, cr, 16, "");
        savePrefsCall(p, cr, 31, "_2");
        savePrefsCall(p, cr, 30, "_voip");

        // sms
        savePrefsSMS(p, cr, 20, "");
        savePrefsSMS(p, cr, 34, "_2");
        savePrefsSMS(p, cr, 29, "_websms");

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
        cr.update(DataProvider.Plans.CONTENT_URI, cv, SELECTION_ID, new String[]{"28"});

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
        float f = Utils.parseFloat(p.getString(PREFS_COST_PER_MB, "0"), 0f);
        cv.put(DataProvider.Plans.COST_PER_AMOUNT1, f);
        cv.put(DataProvider.Plans.COST_PER_AMOUNT2, f);
        cr.update(DataProvider.Plans.CONTENT_URI, cv, SELECTION_TYPE,
                new String[]{String.valueOf(DataProvider.TYPE_DATA)});
    }
}
