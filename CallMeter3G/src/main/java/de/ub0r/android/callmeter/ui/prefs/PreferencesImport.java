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
import com.actionbarsherlock.view.Window;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceGroup;
import android.widget.Toast;

import java.io.File;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.logg0r.Log;

/**
 * Show a list of ready to import rule sets.
 *
 * @author flx
 */
public final class PreferencesImport extends SherlockPreferenceActivity {

    /**
     * {@link AsyncTask} running through the SD card and adding {@link Preferences} for each file.
     *
     * @author flx
     */
    private class FileFinder extends AsyncTask<Void, File, Boolean> {

        /**
         * Tag for output.
         */
        private static final String TAG = "PreferencesImport.FileFinder";

        @Override
        protected void onPreExecute() {
            PreferencesImport.this.setProgressBarIndeterminate(true);
            PreferencesImport.this.setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected Boolean doInBackground(final Void... paramArrayOfParams) {
            return addExport(Environment.getExternalStorageDirectory(), MAXDEPTH);
        }

        @SuppressWarnings("deprecation")
        @Override
        protected void onProgressUpdate(final File... values) {
            for (File f : values) {
                // add file to list
                PreferencesImport context = PreferencesImport.this;
                Preference p = new Preference(context);
                p.setTitle(f.getName());
                p.setSummary(f.getAbsolutePath());
                Intent i = new Intent(context, Preferences.class);
                i.setData(Uri.parse("file://" + f.getAbsolutePath()));
                p.setIntent(i);
                ((PreferenceGroup) context.findPreference("import_rules_files")).addPreference(p);
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            PreferencesImport.this.setProgressBarIndeterminateVisibility(false);
            if (!result) {
                Toast.makeText(PreferencesImport.this, R.string.import_rules_sd_nofiles,
                        Toast.LENGTH_LONG).show();
            }
        }

        ;

        /**
         * Add all ready to import rule sets to preference list.
         *
         * @param d     root directory
         * @param depth maximal depth for searching files
         * @return true, if a file were found
         */
        private boolean addExport(final File d, final int depth) {
            if (d == null) {
                Log.e(TAG, "null file: " + d);
                return false;
            }
            if (!d.exists()) {
                Log.e(TAG, d + " does not exist");
                return false;
            }
            if (!d.isDirectory()) {
                Log.e(TAG, d + " is not a directory");
                return false;
            }
            if (d.list() == null) {
                Log.e(TAG, d + ".list() is null");
                return false;
            }

            boolean ret = false;
            // read directory
            for (String s : d.list()) {
                if (s.startsWith(".") || s.equals("Android") || s.equals("clockworkmod")
                        || s.equals("DCIM") || s.equals("Music") || s.equals("TitaniumBackup")
                        || s.equals("openfeint") || s.equals("soundhound") || s.equals("WhatsApp")
                        || s.equals("Pictures") || s.equals("SMSBackupRestore")) {
                    Log.d(TAG, "skip: ", s);
                    continue;
                }
                File f = new File(d.getAbsoluteFile(), s);
                Log.d(TAG, "try file: ", f.getAbsolutePath());
                if (f.isDirectory()) {
                    if (depth > 0) {
                        ret |= addExport(f, depth - 1);
                    }
                } else if (f.isFile()
                        && (f.getAbsolutePath().endsWith(".export") || f.getAbsolutePath()
                        .endsWith(".xml"))) {
                    // add file to list
                    onProgressUpdate(f);
                    ret = true;
                }
            }
            return ret;
        }
    }

    /**
     * Maximal depth for searching files.
     */
    private static final int MAXDEPTH = 3;

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);
        addPreferencesFromResource(R.xml.import_from_sd);
        new FileFinder().execute((Void) null);
    }
}
