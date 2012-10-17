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

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.ui.HelpActivity;
import de.ub0r.android.callmeter.widget.LogsAppWidgetConfigure;
import de.ub0r.android.callmeter.widget.LogsAppWidgetProvider;
import de.ub0r.android.callmeter.widget.StatsAppWidgetConfigure;
import de.ub0r.android.callmeter.widget.StatsAppWidgetProvider;
import de.ub0r.android.lib.Utils;

/**
 * Show simple preferences.
 * 
 * @author flx
 */
public final class PreferencesPlain extends SherlockPreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener {

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

	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);

		String a = this.getIntent().getAction();
		if (a == null) {
			this.finish();
			return;
		} else if (APPERANCE.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_apperance);
		} else if (TEXTSIZE.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_apperance_textsize);
		} else if (BEHAVIOR.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_behavior);
		} else if (ALERT.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_behavior_alert);
		} else if (ASK_FOR_PLAN.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_behavior_askforplan);
		} else if (WIDGETS.equals(a)) {
			this.addPreferencesFromResource(R.xml.group_prefs);
		} else if (IMPORT.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_import);
			this.findPreference("import_rules").setOnPreferenceClickListener(this);
			this.findPreference("import_rules_default").setOnPreferenceClickListener(this);
		} else if (EXPORT.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_export);
			this.findPreference("export_rules").setOnPreferenceClickListener(this);
			this.findPreference("export_rules_dev").setOnPreferenceClickListener(this);
			this.findPreference("export_logs").setOnPreferenceClickListener(this);
			this.findPreference("export_logs_csv").setOnPreferenceClickListener(this);
			this.findPreference("export_numgroups").setOnPreferenceClickListener(this);
			this.findPreference("export_hourgroups").setOnPreferenceClickListener(this);
		} else if (ADVANCED.equals(a)) {
			this.addPreferencesFromResource(R.xml.prefs_advanced);
			this.findPreference(Preferences.PREFS_ADVANCED).setOnPreferenceChangeListener(this);
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		String a = this.getIntent().getAction();
		if (a == null) {
			this.finish();
			return;
		} else if (WIDGETS.equals(a)) {
			this.loadWidgets();
		}
	}

	/** Load widget list. */
	@SuppressWarnings("deprecation")
	private void loadWidgets() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Preference p = this.findPreference("container");
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
			Preferences.exportData(this, null, null, null, DataProvider.EXPORT_RULESET_FILE, null);
			return true;
		} else if (k.equals("export_rules_dev")) {
			Preferences.exportData(this, null, null, null, DataProvider.EXPORT_RULESET_FILE,
					"android@ub0r.de");
			return true;
		} else if (k.equals("export_logs")) {
			Preferences.exportData(this, null, null, null, DataProvider.EXPORT_LOGS_FILE, null);
			return true;
		} else if (k.equals("export_logs_csv")) {
			Preferences.exportLogsCsv(this);
			return true;
		} else if (k.equals("export_numgroups")) {
			Preferences
					.exportData(this, null, null, null, DataProvider.EXPORT_NUMGROUPS_FILE, null);
			return true;
		} else if (k.equals("export_hourgroups")) {
			Preferences.exportData(this, null, null, null, DataProvider.EXPORT_HOURGROUPS_FILE,
					null);
			return true;
		} else if (k.equals("import_rules")) {
			this.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(this
					.getString(R.string.url_rulesets))));
			return true;
		} else if (k.equals("import_rules_default")) {
			final Intent i = new Intent(this, Preferences.class);
			i.setData(Uri.parse("content://default"));
			this.startActivity(i);
			return true;
		}
		return false;
	}

	@Override
	public boolean onPreferenceChange(final Preference preference, final Object newValue) {
		final String k = preference.getKey();
		if (k.equals(Preferences.PREFS_ADVANCED)) {
			if (newValue.equals(true)) {
				this.startActivity(new Intent(this, HelpActivity.class));
			}
			return true;
		}
		return false;
	}

}
