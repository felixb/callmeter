/*
 * Copyright (C) 2009-2013 Felix Bechstein, The Android Open Source Project
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

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.TrackingSherlockPreferenceActivity;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * Edit a single Plan.
 *
 * @author flx
 */
public final class HourGroupEdit extends TrackingSherlockPreferenceActivity implements
        OnPreferenceClickListener {

    /** {@link ContactsWrapper}. */
    public static final ContactsWrapper CWRAPPER = ContactsWrapper.getInstance();

    /** Item menu: edit. */
    private static final int WHICH_EDIT = 0;
    /** Item menu: delete. */
    private static final int WHICH_DELETE = 1;

    /** List of weekdays. */
    private String[] resDays;
    /** List of hours. */
    private String[] resHours;

    /** Id of edited filed. */
    private long gid = -1;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);

        resDays = getResources().getStringArray(R.array.weekdays_all);
        resHours = getResources().getStringArray(R.array.hours_all);

        addPreferencesFromResource(R.xml.group_prefs);

        final Uri u = getIntent().getData();
        if (u != null) {
            gid = ContentUris.parseId(u);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Utils.setLocale(this);

        reload();
    }

    /**
     * Reload numbers.
     */
    @SuppressWarnings("deprecation")
    private void reload() {
        Cursor c = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.HoursGroup.CONTENT_URI, gid),
                DataProvider.HoursGroup.PROJECTION, null, null, null);
        if (c.moveToFirst()) {
            getSupportActionBar().setSubtitle(c.getString(DataProvider.HoursGroup.INDEX_NAME));
        }
        c.close();
        PreferenceScreen ps = (PreferenceScreen) findPreference("container");
        ps.removeAll();
        c = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.Hours.GROUP_URI, gid),
                DataProvider.Hours.PROJECTION, null, null,
                DataProvider.Hours.DAY + ", " + DataProvider.Hours.HOUR);
        if (c.moveToFirst()) {
            do {
                Preference p = new Preference(this);
                p.setPersistent(false);
                final int day = c.getInt(DataProvider.Hours.INDEX_DAY);
                final int hour = c.getInt(DataProvider.Hours.INDEX_HOUR);
                p.setTitle(resDays[day] + ": " + resHours[hour]);
                p.setKey("item_" + c.getInt(DataProvider.Hours.INDEX_ID));
                p.setOnPreferenceClickListener(this);
                ps.addPreference(p);
            } while (c.moveToNext());
        }
        c.close();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Cursor c = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.Hours.GROUP_URI, gid),
                DataProvider.Hours.PROJECTION, null, null, null);
        if (c.getCount() == 0) {
            Toast.makeText(this, R.string.empty_group, Toast.LENGTH_LONG).show();
        }
        c.close();
    }

    /**
     * Show help text.
     *
     * @param res resource
     */
    private void showHelp(final int res) {
        final Builder b = new Builder(this);
        b.setMessage(res);
        b.setPositiveButton(android.R.string.ok, null);
        b.show();
    }

    /**
     * Set a Hour.
     *
     * @param nid  id of entry
     * @param day  day
     * @param hour hour
     */
    private void setHour(final long nid, final int day, final int hour) {
        final ContentValues cv = new ContentValues();
        cv.put(DataProvider.Hours.GID, gid);
        cv.put(DataProvider.Hours.DAY, day);
        cv.put(DataProvider.Hours.HOUR, hour);
        if (nid < 0) {
            getContentResolver().insert(DataProvider.Hours.CONTENT_URI, cv);
        } else {
            getContentResolver()
                    .update(ContentUris.withAppendedId(DataProvider.Hours.CONTENT_URI, nid), cv,
                            null, null);
        }
        reload();
        RuleMatcher.unmatch(this);
    }

    /**
     * Get a Hour.
     *
     * @param nid id of entry
     * @return Hour
     */
    private int[] getHour(final long nid) {
        int[] ret = new int[]{0, 0};
        final Cursor cursor = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.Hours.CONTENT_URI, nid),
                new String[]{DataProvider.Hours.DAY, DataProvider.Hours.HOUR}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            ret[0] = cursor.getInt(0);
            ret[1] = cursor.getInt(1);
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return ret;
    }

    /**
     * Show dialog to edit the group name.
     */
    private void showNameDialog() {
        final Uri u = ContentUris.withAppendedId(DataProvider.HoursGroup.CONTENT_URI, gid);
        Cursor c = getContentResolver().query(u, DataProvider.HoursGroup.PROJECTION, null,
                null, null);
        String name = null;
        if (c.moveToFirst()) {
            name = c.getString(DataProvider.NumbersGroup.INDEX_NAME);
        }
        c.close();
        final Builder builder = new Builder(this);
        final EditText et = new EditText(this);
        et.setText(name);
        builder.setView(et);
        builder.setTitle(R.string.name_);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                ContentValues values = new ContentValues();
                values.put(DataProvider.NumbersGroup.NAME, et.getText().toString());
                HourGroupEdit.this.getContentResolver().update(u, values, null, null);
                HourGroupEdit.this.getSupportActionBar().setSubtitle(et.getText().toString());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    /**
     * Show an add/delete dialog.
     *
     * @param nid id of entry
     */
    private void showHourDialog(final long nid) {
        final Builder builder = new Builder(this);
        final View v = View.inflate(this, R.layout.prefs_hourgroup_dialog, null);
        final Spinner spDays = (Spinner) v.findViewById(R.id.days);
        final Spinner spHours = (Spinner) v.findViewById(R.id.hours);
        if (nid >= 0) {
            final int[] h = getHour(nid);
            spDays.setSelection(h[0]);
            spHours.setSelection(h[1]);
        }
        builder.setView(v);
        builder.setTitle(R.string.add_hour);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                HourGroupEdit.this.setHour(nid, spDays.getSelectedItemPosition(),
                        spHours.getSelectedItemPosition());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        String k = preference.getKey();
        if (k != null && k.startsWith("item_")) {
            final long id = Long.parseLong(k.substring("item_".length()));

            final Builder builder = new Builder(this);
            builder.setItems(R.array.dialog_edit_delete,
                    new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            switch (which) {
                                case WHICH_EDIT:
                                    HourGroupEdit.this.showHourDialog(id);
                                    break;
                                case WHICH_DELETE:
                                    HourGroupEdit.this.getContentResolver().delete(
                                            ContentUris
                                                    .withAppendedId(DataProvider.Hours.CONTENT_URI,
                                                            id), null, null);
                                    HourGroupEdit.this.reload();
                                    RuleMatcher.unmatch(HourGroupEdit.this);
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_group_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_add:
                showHourDialog(-1);
                return true;
            case R.id.item_delete:
                Builder b = new Builder(this);
                b.setTitle(R.string.delete_group_);
                b.setMessage(R.string.delete_group_hint);
                b.setNegativeButton(android.R.string.no, null);
                b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        HourGroupEdit.this.getContentResolver().delete(
                                ContentUris.withAppendedId(DataProvider.HoursGroup.CONTENT_URI,
                                        HourGroupEdit.this.gid), null, null);
                        Preferences.setDefaultPlan(HourGroupEdit.this, false);
                        RuleMatcher.unmatch(HourGroupEdit.this);
                        HourGroupEdit.this.finish();
                    }
                });
                b.show();
                return true;
            case R.id.item_rename:
                showNameDialog();
                return true;
            case R.id.item_help:
                showHelp(R.string.hourgroup_help);
                return true;
            default:
                return false;
        }
    }
}
