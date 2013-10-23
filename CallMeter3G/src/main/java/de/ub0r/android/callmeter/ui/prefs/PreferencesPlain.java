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

import org.json.JSONException;
import org.json.JSONObject;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.ExportProvider;
import de.ub0r.android.callmeter.ui.HelpActivity;
import de.ub0r.android.callmeter.widget.LogsAppWidgetConfigure;
import de.ub0r.android.callmeter.widget.LogsAppWidgetProvider;
import de.ub0r.android.callmeter.widget.StatsAppWidgetConfigure;
import de.ub0r.android.callmeter.widget.StatsAppWidgetProvider;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Show simple preferences.
 *
 * @author flx
 */
public final class PreferencesPlain extends SherlockPreferenceActivity implements
        OnPreferenceClickListener, OnPreferenceChangeListener {

    private static final String TAG = "PrefPlain";

    /** Action. */
    private static final String APPERANCE = "APPERANCE";
    /** Action. */
    private static final String TEXTSIZE = "TEXTSIZE";
    /** Action. */
    private static final String BEHAVIOR = "BEHAVIOR";
    /** Action. */
    private static final String ALERT = "ALERT";
    /** Action. */
    private static final String ASK_FOR_PLAN = "ASK_FOR_PLAN";
    /** Action. */
    private static final String WIDGETS = "WIDGETS";
    /** Action. */
    private static final String EXPORT = "EXPORT";
    /** Action. */
    private static final String IMPORT = "IMPORT";
    /** Action. */
    private static final String ADVANCED = "ADVANCED";

    private static final long CACHE_TIMEOUT = 1000L * 60L * 15L; // 15min

    private void loadRules() {
        new AsyncTask<Void, Void, JSONObject>() {
            @Override
            protected JSONObject doInBackground(final Void... params) {
                try {
                    String l;
                    StringBuilder sb = new StringBuilder();

                    File f = new File(PreferencesPlain.this.getCacheDir(), "ub0rrules.json");
                    if (f.exists() && f.lastModified() + CACHE_TIMEOUT > System
                            .currentTimeMillis()) {
                        Log.i(TAG, "found cached data: " + f.getAbsolutePath());
                        BufferedReader r = new BufferedReader(new FileReader(f));
                        while ((l = r.readLine()) != null) {
                            sb.append(l);
                        }
                        r.close();
                    }
                    if (sb.length() == 0) {
                        if (f.exists()) {
                            f.delete();
                        }
                        HttpURLConnection c = (HttpURLConnection) new URL(
                                "http://ub0r.de/android/callmeter/rulesets/rulesets.json")
                                .openConnection();
                        Log.i(TAG, "load new data: " + c.getURL());
                        BufferedReader r = new BufferedReader(new InputStreamReader(
                                c.getInputStream()));
                        FileWriter w = new FileWriter(f);
                        while ((l = r.readLine()) != null) {
                            sb.append(l);
                            w.write(l);
                        }
                        r.close();
                        w.close();
                    }
                    try {
                        return new JSONObject(sb.toString());
                    } catch (JSONException e) {
                        Log.e(TAG, "JSON Error", e);
                        Log.e(TAG, "JSON: " + sb.toString());
                        return null;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOError", e);
                }
                return null;
            }

            @SuppressWarnings({"deprecation", "rawtypes"})
            @Override
            protected void onPostExecute(final JSONObject result) {
                if (result == null) {
                    Toast.makeText(PreferencesPlain.this, R.string.err_export_read,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                PreferenceGroup base = (PreferenceGroup) PreferencesPlain.this
                        .findPreference("import_rules_users");
                PreferenceManager pm = base.getPreferenceManager();
                // delete old
                base.removeAll();
                base = (PreferenceGroup) PreferencesPlain.this.findPreference("import_rules_base");
                // build list
                ArrayList<String> keys = new ArrayList<String>(result.length());

                Iterator it = result.keys();
                while (it.hasNext()) {
                    keys.add(it.next().toString());
                }
                Collections.sort(keys);
                keys.remove("common");
                keys.add(0, "common");

                OnPreferenceClickListener opcl = new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(final Preference preference) {
                        Intent intent = new Intent(PreferencesPlain.this, PreferencesRules.class);
                        intent.putExtra(PreferencesRules.EXTRA_JSON, result.toString());
                        intent.putExtra(PreferencesRules.EXTRA_COUNTRY, preference.getKey());
                        PreferencesPlain.this.startActivity(intent);
                        return true;
                    }
                };

                for (String k : keys) {
                    PreferenceScreen p = pm.createPreferenceScreen(PreferencesPlain.this);
                    p.setPersistent(false);
                    p.setKey(k);
                    p.setTitle(k);
                    p.setOnPreferenceClickListener(opcl);
                    base.addPreference(p);
                }
            }

            ;
        }.execute((Void) null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);

        String a = getIntent().getAction();
        if (a == null) {
            finish();
            return;
        } else if (APPERANCE.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_apperance);
        } else if (TEXTSIZE.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_apperance_textsize);
        } else if (BEHAVIOR.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_behavior);
        } else if (ALERT.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_behavior_alert);
        } else if (ASK_FOR_PLAN.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_behavior_askforplan);
        } else if (WIDGETS.equals(a)) {
            addPreferencesFromResource(R.xml.group_prefs);
        } else if (IMPORT.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_import);
            findPreference("import_rules_default").setOnPreferenceClickListener(this);
            loadRules();
        } else if (EXPORT.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_export);
            findPreference("export_rules").setOnPreferenceClickListener(this);
            findPreference("export_rules_sd").setOnPreferenceClickListener(this);
            findPreference("export_rules_dev").setOnPreferenceClickListener(this);
            findPreference("export_logs").setOnPreferenceClickListener(this);
            findPreference("export_logs_csv").setOnPreferenceClickListener(this);
            findPreference("export_numgroups").setOnPreferenceClickListener(this);
            findPreference("export_hourgroups").setOnPreferenceClickListener(this);
        } else if (ADVANCED.equals(a)) {
            addPreferencesFromResource(R.xml.prefs_advanced);
            findPreference(Preferences.PREFS_ADVANCED).setOnPreferenceChangeListener(this);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        String a = getIntent().getAction();
        if (a == null) {
            finish();
            return;
        } else if (WIDGETS.equals(a)) {
            loadWidgets();
        }
    }

    /** Load widget list. */
    @SuppressWarnings("deprecation")
    private void loadWidgets() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Preference p = findPreference("container");
        if (p != null && p instanceof PreferenceScreen) {
            PreferenceScreen ps = (PreferenceScreen) p;
            ps.removeAll();
            int[] ids = AppWidgetManager.getInstance(this).getAppWidgetIds(
                    new ComponentName(this, StatsAppWidgetProvider.class));
            boolean added = false;
            if (ids != null && ids.length > 0) {
                for (int id : ids) {
                    if (prefs.getLong(StatsAppWidgetProvider.WIDGET_PLANID + id, -1) <= 0) {
                        continue;
                    }
                    added = true;
                    p = new Preference(this);
                    p.setTitle(this.getString(R.string.widget_) + " #" + id);
                    final int fid = id;
                    p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(final Preference preference) {
                            Intent i = new Intent(PreferencesPlain.this,
                                    StatsAppWidgetConfigure.class);
                            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, fid);
                            PreferencesPlain.this.startActivity(i);
                            return true;
                        }
                    });
                    ps.addPreference(p);
                }
            }
            ids = AppWidgetManager.getInstance(this).getAppWidgetIds(
                    new ComponentName(this, LogsAppWidgetProvider.class));
            if (ids != null && ids.length > 0) {
                for (int id : ids) {
                    if (prefs.getLong(LogsAppWidgetProvider.WIDGET_PLANID + id, -1) <= 0) {
                        continue;
                    }
                    added = true;
                    p = new Preference(this);
                    p.setTitle(this.getString(R.string.widget_) + " #" + id);
                    final int fid = id;
                    p.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(final Preference preference) {
                            Intent i = new Intent(PreferencesPlain.this,
                                    LogsAppWidgetConfigure.class);
                            i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, fid);
                            PreferencesPlain.this.startActivity(i);
                            return true;
                        }
                    });
                    ps.addPreference(p);
                }
            }
            if (!added) {
                p = new Preference(this);
                p.setTitle(R.string.widgets_no_widgets_);
                p.setSummary(R.string.widgets_no_widgets_hint);
                ps.addPreference(p);
            }
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        final String k = preference.getKey();
        if (k.equals("export_rules")) {
            Preferences
                    .exportData(this, null, null, null, ExportProvider.EXPORT_RULESET_FILE, null);
            return true;
        } else if (k.equals("export_rules_sd")) {
            Preferences.exportData(this, null, null, null, ExportProvider.EXPORT_RULESET_FILE,
                    "sdcard");
            return true;
        } else if (k.equals("export_rules_dev")) {
            Preferences.exportData(this, null, null, null, ExportProvider.EXPORT_RULESET_FILE,
                    "android@ub0r.de");
            return true;
        } else if (k.equals("export_logs")) {
            Preferences.exportData(this, null, null, null, ExportProvider.EXPORT_LOGS_FILE, null);
            return true;
        } else if (k.equals("export_logs_csv")) {
            Preferences.exportLogsCsv(this);
            return true;
        } else if (k.equals("export_numgroups")) {
            Preferences.exportData(this, null, null, null, ExportProvider.EXPORT_NUMGROUPS_FILE,
                    null);
            return true;
        } else if (k.equals("export_hourgroups")) {
            Preferences.exportData(this, null, null, null, ExportProvider.EXPORT_HOURGROUPS_FILE,
                    null);
            return true;
        } else if (k.equals("import_rules_default")) {
            final Intent i = new Intent(this, Preferences.class);
            i.setData(Uri.parse("content://default"));
            startActivity(i);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        final String k = preference.getKey();
        if (k.equals(Preferences.PREFS_ADVANCED)) {
            if (newValue.equals(true)) {
                startActivity(new Intent(this, HelpActivity.class));
            }
            return true;
        }
        return false;
    }

}
