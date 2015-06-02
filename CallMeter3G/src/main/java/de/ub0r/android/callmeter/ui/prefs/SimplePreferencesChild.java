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

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;

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

    /**
     * Action.
     */
    private static final String CALLS = "CALLS";

    /**
     * Action.
     */
    private static final String CALLS_2 = "CALLS_2";

    /**
     * Action.
     */
    private static final String CALLS_VOIP = "CALLS_VOIP";

    /**
     * Action.
     */
    private static final String SMS = "SMS";

    /**
     * Action.
     */
    private static final String SMS_2 = "SMS_2";

    /**
     * Action.
     */
    private static final String SMS_WEBSMS = "SMS_WEBSMS";

    /**
     * Action.
     */
    private static final String MMS = "MMS";

    /**
     * Action.
     */
    private static final String DATA = "DATA";

    @SuppressWarnings("deprecation")
    private void setOnChangeListenerCall(final String postfix) {
        findPreference(SimplePreferences.PREFS_BILLMODE + postfix)
                .setOnPreferenceChangeListener(this);
        findPreference(SimplePreferences.PREFS_FREEMIN + postfix)
                .setOnPreferenceChangeListener(this);
        findPreference(SimplePreferences.PREFS_COST_PER_CALL + postfix)
                .setOnPreferenceChangeListener(this);
        findPreference(SimplePreferences.PREFS_COST_PER_MIN + postfix)
                .setOnPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    private void setOnChangeListenerSMS(final String postfix) {
        findPreference(SimplePreferences.PREFS_FREESMS + postfix)
                .setOnPreferenceChangeListener(this);
        findPreference(SimplePreferences.PREFS_COST_PER_SMS + postfix)
                .setOnPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);
        SimplePreferences.loadPrefs(this);

        String a = getIntent().getAction();
        if (a == null) {
            finish();
            return;
        } else if (CALLS.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_calls);
            setOnChangeListenerCall("");
        } else if (CALLS_2.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_calls_2);
            setOnChangeListenerCall("_2");
        } else if (CALLS_VOIP.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_calls_voip);
            setOnChangeListenerCall("_voip");
        } else if (SMS.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_sms);
            setOnChangeListenerSMS("");
        } else if (SMS_2.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_sms_2);
            setOnChangeListenerSMS("_2");
        } else if (SMS_WEBSMS.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_sms_websms);
            setOnChangeListenerSMS("_websms");
        } else if (MMS.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_mms);
            findPreference(SimplePreferences.PREFS_FREEMMS)
                    .setOnPreferenceChangeListener(this);
            findPreference(SimplePreferences.PREFS_COST_PER_MMS)
                    .setOnPreferenceChangeListener(this);
        } else if (DATA.equals(a)) {
            addPreferencesFromResource(R.xml.simple_prefs_data);
            findPreference(SimplePreferences.PREFS_FREEDATA).setOnPreferenceChangeListener(
                    this);
            findPreference(SimplePreferences.PREFS_COST_PER_MB).setOnPreferenceChangeListener(
                    this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SimplePreferences.savePrefs(this);
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (preference == null) {
            return false;
        }
        RuleMatcher.unmatch(this);
        return true;
    }
}
