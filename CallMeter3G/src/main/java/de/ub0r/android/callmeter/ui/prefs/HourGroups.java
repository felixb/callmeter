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


import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuItem;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.lib.Utils;

/**
 * Activity for setting plans.
 *
 * @author flx
 */
public final class HourGroups extends PreferenceActivity implements
        OnPreferenceClickListener {

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);

        addPreferencesFromResource(R.xml.group_prefs);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();

        PreferenceScreen ps = (PreferenceScreen) findPreference("container");
        ps.removeAll();
        Cursor c = getContentResolver().query(DataProvider.HoursGroup.CONTENT_URI,
                DataProvider.HoursGroup.PROJECTION, null, null, null);
        if (c.moveToFirst()) {
            do {
                Preference p = new Preference(this);
                p.setPersistent(false);
                p.setTitle(c.getString(DataProvider.HoursGroup.INDEX_NAME));
                p.setKey("group_" + c.getInt(DataProvider.HoursGroup.INDEX_ID));
                p.setOnPreferenceClickListener(this);
                ps.addPreference(p);
            } while (c.moveToNext());
        }
        c.close();
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        String k = preference.getKey();
        if (k != null && k.startsWith("group_")) {
            int id = Integer.parseInt(k.substring("group_".length()));
            final Intent intent = new Intent(HourGroups.this, HourGroupEdit.class);
            intent.setData(ContentUris.withAppendedId(DataProvider.HoursGroup.CONTENT_URI, id));
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_add:
                final ContentValues cv = new ContentValues();
                cv.put(DataProvider.HoursGroup.NAME, getString(R.string.new_hourgroup));
                final Uri uri = getContentResolver().insert(DataProvider.HoursGroup.CONTENT_URI,
                        cv);
                final Intent intent = new Intent(this, HourGroupEdit.class);
                intent.setData(uri);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }
}
