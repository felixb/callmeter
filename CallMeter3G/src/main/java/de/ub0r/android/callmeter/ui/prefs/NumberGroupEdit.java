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
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import de.ub0r.android.callmeter.R;
import de.ub0r.android.callmeter.data.DataProvider;
import de.ub0r.android.callmeter.data.RuleMatcher;
import de.ub0r.android.lib.Log;
import de.ub0r.android.lib.Utils;
import de.ub0r.android.lib.apis.ContactsWrapper;

/**
 * Edit a single Plan.
 *
 * @author flx
 */
public final class NumberGroupEdit extends SherlockPreferenceActivity implements
        OnPreferenceClickListener {
    /** Tag for debug out. */
    // private static final String TAG = "nge";

    /** {@link ContactsWrapper}. */
    public static final ContactsWrapper CWRAPPER = ContactsWrapper.getInstance();

    /** Item menu: edit. */
    private static final int WHICH_EDIT = 0;
    /** Item menu: delete. */
    private static final int WHICH_DELETE = 1;

    /** Id of edited filed. */
    private long gid = -1;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.setLocale(this);

        addPreferencesFromResource(R.xml.group_prefs);

        final Intent i = getIntent();
        final Uri u = i.getData();
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
                ContentUris.withAppendedId(DataProvider.NumbersGroup.CONTENT_URI, gid),
                DataProvider.NumbersGroup.PROJECTION, null, null, null);
        if (c.moveToFirst()) {
            getSupportActionBar().setSubtitle(
                    c.getString(DataProvider.NumbersGroup.INDEX_NAME));
        }
        c.close();
        PreferenceScreen ps = (PreferenceScreen) findPreference("container");
        ps.removeAll();
        c = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.Numbers.GROUP_URI, gid),
                DataProvider.Numbers.PROJECTION, null, null, DataProvider.Numbers.NUMBER);
        if (c.moveToFirst()) {
            do {
                Preference p = new Preference(this);
                p.setPersistent(false);
                String number = c.getString(DataProvider.Numbers.INDEX_NUMBER);
                p.setTitle(number);
                p.setKey("item_" + c.getInt(DataProvider.Numbers.INDEX_ID));
                p.setOnPreferenceClickListener(this);
                ps.addPreference(p);

                if (number != null && !number.contains("%")) {
                    String name = CWRAPPER.getNameForNumber(getContentResolver(), number);
                    if (!TextUtils.isEmpty(name)) {
                        p.setSummary(name);
                    }
                }
            } while (c.moveToNext());
        }
        c.close();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Cursor c = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.Numbers.GROUP_URI, gid),
                DataProvider.Numbers.PROJECTION, null, null, null);
        if (c.getCount() == 0) {
            Toast.makeText(this, R.string.empty_group, Toast.LENGTH_LONG).show();
        }
        c.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        if (data == null || data.getData() == null) {
            return;
        }
        // get number for uri
        String number = CWRAPPER.getNumber(getContentResolver(), data.getData());
        if (number == null) {
            number = "???";
        }
        setNumber(requestCode - 1, number);
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
                showNumberDialog(-1);
                return true;
            case R.id.item_delete:
                Builder b = new Builder(this);
                b.setTitle(R.string.delete_group_);
                b.setMessage(R.string.delete_group_hint);
                b.setNegativeButton(android.R.string.no, null);
                b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(final DialogInterface dialog, final int which) {
                        NumberGroupEdit.this.getContentResolver().delete(
                                ContentUris.withAppendedId(DataProvider.NumbersGroup.CONTENT_URI,
                                        NumberGroupEdit.this.gid), null, null);
                        Preferences.setDefaultPlan(NumberGroupEdit.this, false);
                        RuleMatcher.unmatch(NumberGroupEdit.this);
                        NumberGroupEdit.this.finish();
                    }
                });
                b.show();
                return true;
            case R.id.item_rename:
                showNameDialog();
                return true;
            case R.id.item_help:
                showHelp(R.string.numbergroup_help);
                return true;
            default:
                return false;
        }
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
     * Set a number.
     *
     * @param nid    id of entry
     * @param number number
     */
    private void setNumber(final long nid, final String number) {
        final ContentValues cv = new ContentValues();
        String n = null;
        if (!TextUtils.isEmpty(number)) {
            n = number.trim();
            if (TextUtils.isEmpty(n.replaceAll("%", ""))) {
                n = null;
            }
        }
        if (n == null) {
            Log.e("TAG", "setNumber(" + nid + "," + number + ")");
            return;
        }
        cv.put(DataProvider.Numbers.GID, gid);
        String s = n.replaceAll("%", "").replaceAll("\\+", "");
        if (s.length() > 0 && Character.isDigit(s.charAt(0))) {
            n = DataProvider.Logs.cleanNumber(number, true);
        }
        cv.put(DataProvider.Numbers.NUMBER, n);
        if (nid < 0) {
            getContentResolver().insert(DataProvider.Numbers.CONTENT_URI, cv);
        } else {
            getContentResolver().update(
                    ContentUris.withAppendedId(DataProvider.Numbers.CONTENT_URI, nid), cv, null,
                    null);
        }
        reload();
        RuleMatcher.unmatch(this);
    }

    /**
     * Get a number.
     *
     * @param nid id of entry
     * @return number
     */
    private String getNumber(final long nid) {
        String ret = null;
        final Cursor cursor = getContentResolver().query(
                ContentUris.withAppendedId(DataProvider.Numbers.CONTENT_URI, nid),
                new String[]{DataProvider.Numbers.NUMBER}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            ret = cursor.getString(0);
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
        final Uri u = ContentUris.withAppendedId(DataProvider.NumbersGroup.CONTENT_URI, gid);
        Cursor c = getContentResolver().query(u, DataProvider.NumbersGroup.PROJECTION, null,
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
                NumberGroupEdit.this.getContentResolver().update(u, values, null, null);
                NumberGroupEdit.this.getSupportActionBar().setSubtitle(et.getText().toString());
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
    private void showNumberDialog(final long nid) {
        final Builder builder = new Builder(this);
        final EditText et = new EditText(this);
        if (nid >= 0) {
            et.setText(getNumber(nid));
        }
        builder.setView(et);
        builder.setTitle(R.string.add_number);
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                NumberGroupEdit.this.setNumber(nid, et.getText().toString());
            }
        });
        builder.setNeutralButton(R.string.contacts_, new DialogInterface.OnClickListener() {
            public void onClick(final DialogInterface dialog, final int id) {
                final Intent intent = CWRAPPER.getPickPhoneIntent();
                NumberGroupEdit.this.startActivityForResult(intent, (int) nid + 1);
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
                                    NumberGroupEdit.this.showNumberDialog(id);
                                    break;
                                case WHICH_DELETE:
                                    NumberGroupEdit.this.getContentResolver().delete(
                                            ContentUris.withAppendedId(
                                                    DataProvider.Numbers.CONTENT_URI, id), null,
                                            null);
                                    NumberGroupEdit.this.reload();
                                    RuleMatcher.unmatch(NumberGroupEdit.this);
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
}
