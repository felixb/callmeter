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
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog.Builder;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.callmeter.ui.prefs.UpDownPreference.OnUpDownClickListener;
import de.ub0r.android.lib.Utils;

/**
 * {@link SherlockPreferenceActivity} for setting plans.
 *
 * @author flx
 */
public final class Plans extends SherlockPreferenceActivity
        implements OnPreferenceClickListener,
        OnUpDownClickListener {
    /** Tag for output. */
    // private static final String TAG = "pp";

    /**
     * Item menu: edit.
     */
    private static final int WHICH_EDIT = 0;

    /**
     * Item menu: delete.
     */
    private static final int WHICH_DELETE = 1;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);
        addPreferencesFromResource(R.xml.group_prefs);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    /**
     * Reload plans from ContentProvider.
     */
    @SuppressWarnings("deprecation")
    private void reload() {
        PreferenceScreen ps = (PreferenceScreen) findPreference("container");
        ps.removeAll();
        Cursor c = getContentResolver().query(DataProvider.Plans.CONTENT_URI,
                DataProvider.Plans.PROJECTION, null, null, null);
        if (c.moveToFirst()) {
            String[] types = getResources().getStringArray(R.array.plans_type);
            do {
                UpDownPreference p = new UpDownPreference(this, this);
                p.setKey("group_" + c.getInt(DataProvider.Plans.INDEX_ID));
                p.setTitle(c.getString(DataProvider.Plans.INDEX_NAME));

                int t = c.getInt(DataProvider.Plans.INDEX_TYPE);
                String hint;
                if (t >= 0 && t < types.length) {
                    hint = types[t];
                } else {
                    hint = "???";
                }
                String s = c.getString(DataProvider.Plans.INDEX_MERGED_PLANS);
                if (s != null && s.length() > 0) {
                    hint += ", " + getString(R.string.merge_plans_);
                }
                p.setSummary(hint);

                p.setOnPreferenceClickListener(this);
                ps.addPreference(p);
            } while (c.moveToNext());
        }
        c.close();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getSupportMenuInflater().inflate(R.menu.menu_group, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                Preferences.PREFS_ADVANCED, false);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_add:
                Preferences.setDefaultPlan(this, false);
                final ContentValues cv = new ContentValues();
                cv.put(DataProvider.Plans.NAME, getString(R.string.plans_new));
                final Uri uri = getContentResolver().insert(DataProvider.Plans.CONTENT_URI, cv);
                final Intent intent = new Intent(this, PlanEdit.class);
                intent.setData(uri);
                startActivity(intent);
                return true;
            default:
                return false;
        }
    }

    /**
     * Move an item.
     *
     * @param u        item's {@link Uri}
     * @param diretion +1/-1
     */
    private void move(final Uri u, final int diretion) {
        Cursor c0 = getContentResolver().query(u, DataProvider.Plans.PROJECTION_BASIC, null,
                null, null);
        if (c0.moveToFirst()) {
            int idx = c0.getColumnIndex(DataProvider.Plans.ORDER);
            int o0;
            if (c0.isNull(idx)) {
                o0 = c0.getInt(DataProvider.Plans.INDEX_ID);
            } else {
                o0 = c0.getInt(idx);
            }
            String w;
            String o;
            if (diretion < 0) {
                w = DataProvider.Plans.ORDER + "<? or (" + DataProvider.Plans.ORDER
                        + " is null and " + DataProvider.Plans.ID + "<?)";
                o = DataProvider.Plans.REVERSE_ORDER;
            } else {
                w = DataProvider.Plans.ORDER + ">? or (" + DataProvider.Plans.ORDER
                        + " is null and " + DataProvider.Plans.ID + ">?)";
                o = DataProvider.Plans.DEFAULT_ORDER;
            }
            String s0 = String.valueOf(o0);
            Cursor c1 = getContentResolver().query(DataProvider.Plans.CONTENT_URI,
                    DataProvider.Plans.PROJECTION_BASIC, w, new String[]{s0, s0}, o);
            if (c1.moveToFirst()) {
                ContentValues values = new ContentValues();
                values.put(DataProvider.Plans.ORDER, o0);
                getContentResolver().update(
                        ContentUris.withAppendedId(DataProvider.Plans.CONTENT_URI,
                                c1.getInt(DataProvider.Plans.INDEX_ID)), values, null, null);
            }
            c1.close();

            ContentValues values = new ContentValues();
            values.put(DataProvider.Plans.ORDER, o0 + diretion);
            getContentResolver().update(u, values, null, null);

            reload();
        }
        c0.close();
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        String k = preference.getKey();
        if (k != null && k.startsWith("group_")) {
            final int id = Integer.parseInt(k.substring("group_".length()));
            final Uri uri = ContentUris.withAppendedId(DataProvider.Plans.CONTENT_URI, id);
            final Builder builder = new Builder(this);
            builder.setItems(R.array.dialog_edit_delete,
                    new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, final int which) {
                            switch (which) {
                                case WHICH_EDIT:
                                    final Intent intent = new Intent(Plans.this, PlanEdit.class);
                                    intent.setData(uri);
                                    Plans.this.startActivity(intent);
                                    break;
                                case WHICH_DELETE:
                                    Builder b = new Builder(Plans.this);
                                    b.setTitle(R.string.delete_);
                                    b.setMessage(R.string.delete_plan_hint);
                                    b.setNegativeButton(android.R.string.no, null);
                                    b.setPositiveButton(android.R.string.yes,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(final DialogInterface dialog,
                                                        final int which) {
                                                    Plans.this
                                                            .getContentResolver()
                                                            .delete(ContentUris.withAppendedId(
                                                                            DataProvider.Plans.CONTENT_URI,
                                                                            id),
                                                                    null, null);
                                                    Plans.this.reload();
                                                    Preferences.setDefaultPlan(Plans.this, false);
                                                    RuleMatcher.unmatch(Plans.this);
                                                }
                                            });
                                    b.show();
                                    break;
                                default:
                                    break;
                            }
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
            return true;
        }
        return false;
    }

    @Override
    public void onUpDownClick(final Preference preference, final int direction) {
        String k = preference.getKey();
        int id = Integer.parseInt(k.substring("group_".length()));
        Uri uri = ContentUris.withAppendedId(DataProvider.Plans.CONTENT_URI, id);
        move(uri, direction);
    }
}
