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

import java.io.File;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockPreferenceActivity;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;

/**
 * Show a list of ready to import rule sets.
 * 
 * @author flx
 */
public final class PreferencesImport extends SherlockPreferenceActivity {
	/** Tag for output. */
	private static final String TAG = "prefs.im";

	/** Maximal depth for searching files. */
	private static final int MAXDEPTH = 3;

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Utils.setLocale(this);
		this.setTitle(R.string.import_rules_);
		this.addPreferencesFromResource(R.xml.import_from_sd);

		if (!this.addExport(Environment.getExternalStorageDirectory(), MAXDEPTH)) {
			Toast.makeText(this, R.string.import_rules_sd_nofiles, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Add all ready to import rule sets to preference list.
	 * 
	 * @param d
	 *            root directory
	 * @param depth
	 *            maximal depth for searching files
	 * @return true, if a files were found
	 */
	private boolean addExport(final File d, final int depth) {
		if (!d.exists()) {
			Log.e(TAG, d + " does not exist");
			return false;
		}
		if (!d.isDirectory()) {
			Log.e(TAG, d + " is not a directory");
			return false;
		}

		boolean ret = false;
		// read directory
		for (String s : d.list()) {
			if (s.startsWith(".") || s.equals("Android") || s.equals("DCIM") || s.equals("Music")
					|| s.equals("TitaniumBackup") || s.equals("openfeint")
					|| s.equals("soundhound") || s.equals("WhatsApp") || s.equals("Pictures")) {
				Log.d(TAG, "skip: " + s);
				continue;
			}
			File f = new File(d.getAbsoluteFile(), s);
			Log.d(TAG, "try file: " + f.getAbsolutePath());
			if (f.isDirectory()) {
				if (depth > 0) {
					ret |= this.addExport(f, depth - 1);
				}
			} else if (f.isFile() && f.getAbsolutePath().endsWith(".export")) {
				// add file to list
				Preference p = new Preference(this);
				p.setTitle(f.getName());
				p.setSummary(f.getAbsolutePath());
				Intent i = new Intent(this, Preferences.class);
				i.setData(Uri.parse("file://" + f.getAbsolutePath()));
				p.setIntent(i);
				((PreferenceGroup) this.findPreference("import_rules_files")).addPreference(p);
				ret = true;
			}
		}
		return ret;
	}
}
